(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.tp-mark
  "Shared tp_mark marker machinery (upstream EntityTPMarking + MarkRender).

  MarkTeleport, PenetrateTeleport and Flashing all spawn an EntityTPMarking
  client-side: a SimpleModelBiped humanoid textured with the tp_mark 7-frame
  effect sequence (frame = ticksExisted / 2.5 % 7), which copies the player's
  rotation every tick and, because MarkRender rotates by -yaw where a normal
  entity renderer uses 180 - yaw, ends up facing back at the caster.
  MarkRender tints it via glColor4d: white when available, red (1, 0.2, 0.2)
  when not. Every tick the mark emits a green TPParticleFactory particle 40%
  of the time (rand.nextDouble() < 0.4)."
  (:require [cn.li.ac.ability.client.effects.billboard-particles :as bp]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]))

(def ^:private mark-frame-count 7)

(defn- mark-frame-texture
  [frame]
  (modid/asset-path "textures/effects/tp_mark"
                    (str (mod (long frame) mark-frame-count) ".png")))

(defn- rand-range
  [a b]
  (+ a (rand (- b a))))

(def ^:private tp-particle-texture
  (modid/asset-path "textures/effects" "tp_particle.png"))

(def ^:private model-parts
  "SimpleModelBiped (ModelBiped 0.0) parts: each is a 3D box (half-width,
  half-height, half-depth, center offsets, and per-face skin UV regions in
  the 256x128 tp_mark canvas — the 64x32 atlas layout scaled x4). The boxes
  are oriented with their FRONT (+z) along the player's look direction
  (glRotated(-rotationYaw)), so rotating the view shows different faces —
  the humanoid's facing is observable, like the 3D model.

  :cy is measured from the ENTITY position, and the model HANGS from it.
  MarkRender does glTranslated(x, y, z) + glScaled(-1, -1, 1) and nothing
  else — it never applies RenderLivingBase's translate(0, -1.5, 0), which is
  what normally lifts a biped so its feet land on the entity. So ModelBiped's
  own layout (head -8..0, body 0..12, legs 12..24, y down) lands at head
  0..+0.5, body and arms -0.75..0, legs -1.5..-0.75: the anchor is the
  figure's NECK and its feet are 1.5 below."
  [;; head 8x8x8 -> 0.5^3, y 0..0.5 above the anchor
   {:hw 0.25 :hh 0.25 :hd 0.25 :cx 0.0 :cy 0.25
    :front [0.125 0.25 0.25 0.5] :back [0.375 0.5 0.25 0.5]
    :right [0.0 0.125 0.25 0.5] :left [0.25 0.375 0.25 0.5]
    :top [0.125 0.25 0.0 0.25] :bottom [0.25 0.375 0.0 0.25]}
   ;; headwear: SimpleModelBiped.draw renders bipedHeadwear too — the same box
   ;; inflated 0.5 model units a side (half-extent 4.5/16), texOffset 32,0.
   {:hw 0.28125 :hh 0.28125 :hd 0.28125 :cx 0.0 :cy 0.25
    :front [0.625 0.75 0.25 0.5] :back [0.875 1.0 0.25 0.5]
    :right [0.5 0.625 0.25 0.5] :left [0.75 0.875 0.25 0.5]
    :top [0.625 0.75 0.0 0.25] :bottom [0.75 0.875 0.0 0.25]}
   ;; body 8x12x4 -> 0.5x0.75x0.25, y -0.75..0
   {:hw 0.25 :hh 0.375 :hd 0.125 :cx 0.0 :cy -0.375
    :front [0.3125 0.4375 0.5 0.875] :back [0.5 0.625 0.5 0.875]
    :right [0.25 0.3125 0.5 0.875] :left [0.4375 0.5 0.5 0.875]
    :top [0.3125 0.4375 0.5 0.625] :bottom [0.3125 0.4375 0.75 0.875]}
   ;; right arm 4x12x4 -> 0.25x0.75x0.25 at x +0.375, y -0.75..0
   {:hw 0.125 :hh 0.375 :hd 0.125 :cx 0.375 :cy -0.375
    :front [0.6875 0.75 0.5 0.875] :back [0.75 0.8125 0.5 0.875]
    :right [0.625 0.6875 0.5 0.875] :left [0.8125 0.875 0.5 0.875]
    :top [0.6875 0.75 0.5 0.625] :bottom [0.6875 0.75 0.75 0.875]}
   ;; left arm at x -0.375 (mirrored regions)
   {:hw 0.125 :hh 0.375 :hd 0.125 :cx -0.375 :cy -0.375
    :front [0.5625 0.625 0.5 0.875] :back [0.5 0.5625 0.5 0.875]
    :right [0.5 0.5625 0.5 0.875] :left [0.5625 0.625 0.5 0.875]
    :top [0.5625 0.625 0.5 0.625] :bottom [0.5625 0.625 0.75 0.875]}
   ;; right leg 4x12x4 -> 0.25x0.75x0.25 at x +0.125, y -1.5..-0.75
   {:hw 0.125 :hh 0.375 :hd 0.125 :cx 0.125 :cy -1.125
    :front [0.0625 0.125 0.5 0.875] :back [0.0 0.0625 0.5 0.875]
    :right [0.0 0.0625 0.5 0.875] :left [0.0625 0.125 0.5 0.875]
    :top [0.0625 0.125 0.5 0.625] :bottom [0.0625 0.125 0.75 0.875]}
   ;; left leg at x -0.125 (mirrored regions)
   {:hw 0.125 :hh 0.375 :hd 0.125 :cx -0.125 :cy -1.125
    :front [0.1875 0.25 0.5 0.875] :back [0.125 0.1875 0.5 0.875]
    :right [0.125 0.1875 0.5 0.875] :left [0.1875 0.25 0.5 0.875]
    :top [0.1875 0.25 0.5 0.625] :bottom [0.1875 0.25 0.75 0.875]}])

(defn- face-quads
  "Emit the six faces of one box: face-axis (front/back/right/left/top/bottom)
  oriented in the box's local frame, each a quad spanning the two tangent
  axes with the face's skin UV region.

  Each face sits at ITS OWN half-extent along its normal — hd for front/back,
  hw for the sides, hh for top/bottom. Using hd for all six collapsed every
  non-cubic box: the body's sides sank 0.125 inside its silhouette and every
  part's caps floated near the middle of the box."
  [texture center {:keys [hw hh hd]} f r u
   {:keys [front back right left top bottom]} color]
  (let [qf (fn [face-axis face-half a1 h1 a2 h2 uv]
             (let [c (rv3/v+ center (rv3/v* face-axis (double face-half)))
                   s1 (rv3/v* a1 (double h1))
                   s2 (rv3/v* a2 (double h2))]
               (ru/quad-op texture
                           (rv3/v+ (rv3/v- c s1) s2)
                           (rv3/v- (rv3/v- c s1) s2)
                           (rv3/v- (rv3/v+ c s1) s2)
                           (rv3/v+ (rv3/v+ c s1) s2)
                           (nth uv 0) (nth uv 1) (nth uv 2) (nth uv 3)
                           color)))]
    [(qf f hd r hw u hh front)
     (qf (rv3/v* f -1.0) hd r hw u hh back)
     (qf r hw f hd u hh right)
     (qf (rv3/v* r -1.0) hw f hd u hh left)
     (qf u hh r hw f hd top)
     (qf (rv3/v* u -1.0) hh r hw f hd bottom)]))

(defn humanoid-ops
  "Upstream MarkRender: SimpleModelBiped (~1 wide across the arms x 2.0 tall)
  textured with the tp_mark frame sequence, frame =
  (int)((ticksExisted / 2.5) % 7). `anchor` is the EntityTPMarking position
  and the figure HANGS from it — head 0.5 above, feet 1.5 below (see
  model-parts). `yaw-rad` is the caster's head yaw, which the mark copies
  every tick; the figure ends up facing back toward them (see the basis
  below). `color` is the tint (white when available; upstream MarkRender
  paints glColor4d(1, 0.2, 0.2, 1) when not)."
  [yaw-rad anchor ticks color]
  (let [anchor-x (double (:x anchor))
        anchor-y (double (:y anchor))
        anchor-z (double (:z anchor))
        frame (mod (long (Math/floor (/ (double ticks) 2.5))) mark-frame-count)
        texture (mark-frame-texture frame)
        ;; MarkRender rotates by -rotationYaw where a normal entity renderer
        ;; uses 180 - rotationYaw, and the mark copies the player's yaw every
        ;; tick. That missing 180 is the whole point: the figure faces the
        ;; OPPOSITE of the caster's look, i.e. it stands at the destination
        ;; looking back at them. Deriving the facing from the camera->marker
        ;; direction instead (the marker rides the look ray, so that vector
        ;; turns with the view) left it showing the same side no matter which
        ;; way the caster turned.
        yaw (double (or yaw-rad 0.0))
        look-x (- (Math/sin yaw))
        look-z (Math/cos yaw)
        f (rv3/v3 (- look-x) 0.0 (- look-z))
        r (rv3/v3 (.z f) 0.0 (- (.x f)))
        u rv3/unit-y]
    (mapcat (fn [part]
              ;; Upstream MarkRender disables GL_DEPTH_TEST + GL_CULL_FACE:
              ;; the humanoid stays visible through walls even when the
              ;; destination is still inside one (penetrate's unavailable
              ;; case) — the renderer emits these with a no-depth, no-cull
              ;; translucent render type.
              (mapv #(assoc % :no-depth-test? true)
                    (face-quads texture
                                (rv3/v3 (+ anchor-x (:cx part))
                                        (+ anchor-y (:cy part))
                                        anchor-z)
                                part f r u part color)))
            model-parts)))

(defn ambient-particle
  "Upstream EntityTPMarking.onUpdate TPParticleFactory particle: position
  offsets (±1, rand(0.2,1.6)-1.6, ±1) from the ENTITY position, velocity
  (±0.03, 0..0.05, ±0.03); size 0.1-0.2, alpha 153-204, fadeAfter(20, 20)
  with the template fade-in 5.

  That -1.6 is why the offsets have to be measured from the same anchor the
  humanoid hangs from: relative to its feet the band is +0.1 to +1.5, i.e.
  wrapped around the figure. Measured from the feet instead it sits entirely
  below them, in the ground."
  [anchor]
  {:x (+ (double (:x anchor)) (rand-range -1.0 1.0))
   :y (+ (double (:y anchor)) (- (rand-range 0.2 1.6) 1.6))
   :z (+ (double (:z anchor)) (rand-range -1.0 1.0))
   :vx (rand-range -0.03 0.03)
   :vy (rand-range 0.0 0.05)
   :vz (rand-range -0.03 0.03)
   :size (rand-range 0.1 0.2)
   :texture tp-particle-texture
   :start-alpha (long (rand-range 153 204))
   :age 0 :life 20 :fade-in 5 :fade-out 20})

(defn tick-marker!
  "Advance the marker state: ticks + one TPParticleFactory spawn per tick at
  rand < 0.4 (upstream EntityTPMarking.onUpdate). `spawn-ambient?` is a
  predicate on the state deciding whether particles may spawn — upstream
  gates on `available` (penetrate) and both skills share the 0.4 roll."
  ([st] (tick-marker! st (fn [st] (map? (:target st)))))
  ([st spawn-ambient?]
   (let [target (:target st)]
     (assoc st
            :ticks (inc (long (or (:ticks st) 0)))
            :ambient-particles
            (cond-> (bp/tick-particles! (:ambient-particles st))
              (and (spawn-ambient? st) (< (rand) 0.4))
              (conj (ambient-particle target)))))))
