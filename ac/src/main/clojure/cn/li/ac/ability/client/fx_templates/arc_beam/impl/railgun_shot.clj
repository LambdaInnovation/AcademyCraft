(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.railgun-shot
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.effects.ray-composite :as ray-composite]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.fx-templates.arc-beam])
  (:import [cn.li.mcmod.math V3]))

(def ^:private beam-life-ticks 50)

(defn- fade-out-factor [life]
  ;; EntityRailgunFX: blend-out is the final 1000 ms (20 ticks).
  (min 1.0 (/ (double life) (/ 20.0 beam-life-ticks))))

(defn- width-factor [beam life]
  ;; RendererRayCylinder multiplies both radii by getWidth(). Upstream keeps a
  ;; small [0, 0.3] random-walk wiggle after the 800 ms shrink reaches zero.
  (let [shrink (min 1.0 (/ (double life) (/ 16.0 beam-life-ticks)))
        seed (double (or (:wiggle-seed beam) 0.0))
        wiggle (* 0.15 (+ 1.0 (Math/sin (+ seed (* life 20.0)))))]
    (+ shrink wiggle)))

(def ^:private railgun-beam-style
  ;; RendererRayComposite: cylinderOut radius 0.13 @ (236,170,93,60),
  ;; cylinderIn radius 0.09 @ (241,240,222,200). A camera-facing billboard of
  ;; half-width w has the same silhouette as a cylinder of radius w, so these
  ;; are the radii verbatim — the port had them at 0.45/0.28, three times the
  ;; original bore.
  {:width       (fn [beam life] (* 0.13 (width-factor beam life)))
   :core-width  (fn [beam life] (* 0.09 (width-factor beam life)))
   :outer-rgb   {:r 236 :g 170 :b 93}
   :outer-alpha (fn [_ life] (* 60.0 (fade-out-factor life)))
   :inner-rgb   {:r 241 :g 240 :b 222}
   :inner-alpha (fn [_ life] (* 200.0 (fade-out-factor life)))
   ;; Retain the port's enhanced cyan center highlight in addition to the
   ;; original inner/outer cylinders.
   :line-rgb    {:r 165 :g 230 :b 255}
   :line-alpha  (fn [_ life] (+ 40.0 (* 120.0 (fade-out-factor life))))})









(def ^:private charge-ttl 32)

(defn- all-beam-effects []
  (mapcat val (:beam-effects (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :railgun-shot))))

(defn- all-charge-effects []
  (vals (:charging (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :railgun-shot))))

(defn- ensure-store [store]
  (if (contains? (or store {}) :beam-effects)
    (or store {:beam-effects {} :charging {}})
    {:beam-effects {} :charging {}}))

;; SubArc: 30 ticks of life, a fixed random orientation, and a template that
;; is re-rolled about every other tick. `draw` starts false — an arc flickers
;; on before it is ever seen.
(def ^:private sub-arc-life 30)
(def ^:private arc-template-count 15)

(defn- new-sub-arc [distance]
  {:distance distance
   :theta (* 2.0 Math/PI (rand))
   :radius (+ 0.1 (* 0.15 (rand)))
   :tex-id (rand-int arc-template-count)
   :draw? false
   :age 0
   :rot-x (* 2.0 Math/PI (rand))
   :rot-y (* 2.0 Math/PI (rand))
   :rot-z (* 2.0 Math/PI (rand))})

(defn- tick-sub-arc
  "SubArc.tick(): re-roll the template half the time, age on 9 ticks out of 10,
  die at 30, and toggle visibility (40% off when shown, 30% on when hidden)."
  [arc]
  (let [arc (cond-> arc
              (< (rand) 0.5) (assoc :tex-id (rand-int arc-template-count))
              (< (rand) 0.9) (update :age (fnil inc 0)))]
    (when (< (long (:age arc 0)) sub-arc-life)
      (if (:draw? arc)
        (cond-> arc (< (rand) 0.4) (assoc :draw? false))
        (cond-> arc (< (rand) 0.3) (assoc :draw? true))))))

(defn- arc-placements [^V3 start ^V3 end]
  (let [length (vec3/vlen (vec3/v- end start))]
    (loop [cursor 1.0
           placements []]
      (if (> cursor length)
        placements
        (recur (+ cursor 1.0 (rand))
               (conj placements (new-sub-arc cursor)))))))

(defn- enqueue-state!
  "Charge events keep a self-contained one-shot arc-burst state. A live
  :charging entry also keeps level-effect rendering active before a beam exists."
  [store ctx-id channel owner-key payload]
  (let [store* (ensure-store store)
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode start end hit-distance source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :charge-start
      (assoc-in store* [:charging owner-key*]
                {:ttl charge-ttl
                 :max-ttl charge-ttl
                 ;; Wall clock, so the hand quad picks frames off the same
                 ;; timebase the entity billboard does — see charge-hand-ops.
                 :started-ms (System/currentTimeMillis)
                 :source-player-id source-player-id})

      ;; RailgunHandEffect is a self-contained 1.6-second animation. Charging
      ;; updates and cancellation do not restart or delete it upstream.
      (:charge-update :charge-end)
      store*

      (if (and start end)
        (update-in store* [:beam-effects owner-key*] (fnil conj [])
                   (merge base-meta
                          {:start (vec3/map->v3 start)
                           :end (vec3/map->v3 end)
                           :mode (or mode :block-hit)
                           :hit-distance (double (or hit-distance 18.0))
                           :ttl beam-life-ticks
                           :max-ttl beam-life-ticks
                           :arc-placements
                           (arc-placements
                             (vec3/map->v3 start)
                             (vec3/map->v3 end))
                           :wiggle-seed (* 2.0 Math/PI (rand))}))  ;; random phase [0, 2π)
        store*))))

;; EntityRailgunFX.onUpdate: arcHandler.clear() at ticksExisted == 30 — the
;; lightning is gone for the beam's last 20 ticks while the ray itself fades.
(def ^:private arc-clear-age 30)

(defn- tick-beam-arcs
  [beam]
  (let [age (- (double (:max-ttl beam)) (double (:ttl beam)))]
    (if (>= age arc-clear-age)
      (assoc beam :arc-placements [])
      (update beam :arc-placements #(vec (keep tick-sub-arc %))))))

(defn- tick-state!
  [store]
  (let [store* (ensure-store store)]
    (-> store*
        (update :beam-effects
          (fn [by-owner]
            (store-tick/tick-ttl-items-by-owner
              (reduce-kv (fn [acc owner beams]
                           (assoc acc owner (mapv tick-beam-arcs beams)))
                         {}
                         (or by-owner {})))))
        (update :charging store-tick/tick-ttl-states-by-owner))))

(defn- visible-beam [beam]
  ;; EntityRayBase grows to full length over the first 150 ms.
  (let [age (- (double (:max-ttl beam)) (double (:ttl beam)))
        blend-in (min 1.0 (/ age 3.0))
        start (:start beam)]
    (assoc beam :end
      (vec3/v+ start (vec3/v* (vec3/v- (:end beam) start) blend-in)))))

;; RendererRayComposite for "railgun": the glow is the three
;; blend_in/tile/blend_out boards at width 1.1, startFix -0.3, endFix +0.3.
(def ^:private glow-textures (ray-composite/glow-textures "railgun"))
(def ^:private glow-width 1.1)

(defn- railgun-beam-ops
  "The whole RendererRayComposite for a railgun shot: the three glow boards,
  an outer 0.13 tube and an inner 0.09 core, each with the paraboloid nose at
  both ends — plus the port's bright cyan core line down the axis.

  These are tubes, not billboards. The port went flat because a tube seen from
  the caster's own eye — which sits exactly on the axis of an eye-spawned ray —
  reads as a hollow pipe; that is what hand-muzzle-pos already solves, by
  starting the ray off to the side of the camera."
  [^V3 cam-v beam]
  (let [life (/ (double (:ttl beam)) (double (:max-ttl beam)))
        w (width-factor beam life)
        fade (fade-out-factor life)
        seed (double (or (:wiggle-seed beam) 0.0))
        ;; Upstream never sets the railgun glow colour, so RendererRayGlow's
        ;; default white (alpha 255) applies. With 255 the 1.1-wide board --
        ;; whose tile texture is a hard-edged full-width streak -- reads as a
        ;; solid white bar four to eight times the beam's bright core. The
        ;; port's 170 is the visual match for that number in this engine.
        glow-wiggle (ray-composite/glow-wiggle-factor seed life)
        outer-color (ru/with-alpha (:outer-rgb railgun-beam-style)
                                   (int (* 60.0 fade)))
        inner-color (ru/with-alpha (:inner-rgb railgun-beam-style)
                                   (int (* 200.0 fade)))
        line-color (ru/with-alpha (:line-rgb railgun-beam-style)
                                  (int (+ 40.0 (* 120.0 fade))))]
    (concat
      (ray-composite/composite-ops cam-v (:start beam) (:end beam)
        {:glow {:textures glow-textures
                :width (* glow-width w glow-wiggle)
                :color {:r 255 :g 255 :b 255
                        :a (int (ray-composite/glow-alpha 170.0 fade seed life))}}
         :inner {:radius (* 0.09 w) :color inner-color}
         :outer {:radius (* 0.13 w) :color outer-color}})
      ;; Port enhancement kept: a bright cyan core line down the axis.
      [(ru/line-op (:start beam) (:end beam) line-color)])))

(defn- impact-ring-ops [^V3 cam-pos ^V3 end ttl max-ttl]
  ;; Enhanced port effect: expanding cyan ring at the shot endpoint, oriented
  ;; to face the camera — the beam-perpendicular ring is edge-on from the
  ;; caster's first-person view. Deliberately additive to the original beam
  ;; visuals, not a gameplay hit indicator.
  (let [life (/ (double ttl) (double (max 1 max-ttl)))
        radius (+ 0.12 (* 0.22 (- 1.0 life)))
        color (ru/with-alpha {:r 188 :g 252 :b 238} (+ 20 (* 160 life)))
        segments 12
        to-cam (vec3/vnorm (vec3/v- cam-pos end))
        candidate (if (< (Math/abs (.-y ^V3 to-cam)) 0.9) vec3/unit-y vec3/unit-x)
        right (vec3/vnorm (vec3/vcross candidate to-cam))
        up (vec3/vnorm (vec3/vcross to-cam right))]
    (vec
      (for [idx (range segments)
            :let [t0 (/ (* 2.0 Math/PI idx) segments)
                  t1 (/ (* 2.0 Math/PI (inc idx)) segments)
                  p0 (vec3/v+ end
                               (vec3/v+ (vec3/v* right (* radius (Math/cos t0)))
                                        (vec3/v* up (* radius (Math/sin t0)))))
                  p1 (vec3/v+ end
                               (vec3/v+ (vec3/v* right (* radius (Math/cos t1)))
                                        (vec3/v* up (* radius (Math/sin t1)))))]]
        (ru/line-op p0 p1 color)))))

(defn- view-forward
  "Look direction from the hand-runtime's yaw/pitch (Minecraft convention)."
  [{:keys [player-yaw-rad player-pitch-rad]}]
  (let [yaw (double (or player-yaw-rad 0.0))
        pitch (double (or player-pitch-rad 0.0))
        cos-p (Math/cos pitch)]
    (vec3/v3 (* (- (Math/sin yaw)) cos-p)
             (- (Math/sin pitch))
             (* (Math/cos yaw) cos-p))))

(defn- charge-hand-ops
  "RailgunHandEffect's first-person branch: the 2x2 billboard scaled by 0.4 and
  offset (.26, -.15, -.24).

  Those offsets are applied inside renderHand, i.e. in VIEW space — right, up
  and forward of where the player is looking. The port had been adding them to
  world x/y/z, so the burst drifted off the hand as soon as the player turned,
  and the quad itself was axis-aligned in world space rather than facing the
  camera, so it vanished edge-on at the wrong angles.

  Note the Z sign: OpenGL's camera looks down -Z, so upstream's -.24 is a
  quarter block IN FRONT of the eye. Reading it as -0.24 along the look vector
  put the burst behind the near plane and nothing was visible at all."
  [^V3 hand-center ^V3 look-dir charge-state]
  (let [;; PER_FRAME is 40ms but a tick is 50, so counting frames off the tick
        ;; counter paced the animation unevenly and drifted from the entity
        ;; billboard (which reads age + partialTick). Both now run off elapsed
        ;; milliseconds, so switching camera mid-burst does not jump a frame.
        elapsed-ms (if-let [started (:started-ms charge-state)]
                     (- (System/currentTimeMillis) (long started))
                     (* 50.0 (- (double (:max-ttl charge-state))
                                (double (:ttl charge-state)))))
        ;; 40 frames at 40ms each = 1.6s total animation
        frame (max 0 (min 39 (int (/ (double elapsed-ms) 40.0))))
        texture-path (modid/asset-path
                       "textures"
                       (str "effects/arc_burst/" frame ".png"))
        half-size 0.4
        forward (vec3/vnorm look-dir)
        [right up] (vec3/orthonormal-basis forward)
        center (vec3/v+ hand-center
                        (vec3/v+ (vec3/v* right 0.26)
                                 (vec3/v+ (vec3/v* up -0.15)
                                          (vec3/v* forward 0.24))))
        rx (vec3/v* right half-size)
        uy (vec3/v* up half-size)
        p0 (vec3/v- (vec3/v- center rx) uy)
        p1 (vec3/v- (vec3/v+ center rx) uy)
        p2 (vec3/v+ (vec3/v+ center rx) uy)
        p3 (vec3/v+ (vec3/v- center rx) uy)]
    [{:kind :quad
      :texture texture-path
      :p0 p0 :p1 p1 :p2 p2 :p3 p3
      :u0 0.0 :u1 1.0 :v0 0.0 :v1 1.0
      :color {:r 255 :g 255 :b 255 :a 255}}]))

(defn- build-plan
  "Beam :start/:end are precomputed to V3 at enqueue time (see enqueue-state!
  above) — a beam's endpoints never change after it's fired, so converting
  once there instead of once per frame here removes an otherwise-per-frame
  allocation for every live beam."
  [camera-pos hand-center-pos game-ticks]
  (let [beams (all-beam-effects)
        ^V3 cam-v (vec3/map->v3 camera-pos)
        player-uuid (:player-uuid hand-center-pos)
        charge-state (when player-uuid
                       (some (fn [state]
                               (when (or (nil? (:source-player-id state))
                                         (= player-uuid (:source-player-id state)))
                                 state))
                             (all-charge-effects)))
        beam-plan (mapcat (fn [beam]
                            (let [visible (visible-beam beam)]
                              (concat
                              ;; Arc/lightning branches
                              (arc-fx/railgun-arc-ops cam-v beam {})
                              ;; Glow boards, the two cylinders, the core line
                              (railgun-beam-ops cam-v visible)
                              (impact-ring-ops
                                cam-v (:end visible) (:ttl beam) (:max-ttl beam)))))
                          beams)
        ;; RailgunHandEffect is ONE effect with two branches: the hand quad in
        ;; first person, the 2x2 billboard on the player model otherwise. The
        ;; world-anchored railgun_charge entity covers the second branch for
        ;; every viewer including the caster, so drawing the hand quad outside
        ;; first person played both animations at once.
        charge-plan (if (and hand-center-pos charge-state
                             (:first-person? hand-center-pos))
                      (charge-hand-ops
                        (vec3/v3 (double (:x hand-center-pos))
                                 (double (:y hand-center-pos))
                                 (double (:z hand-center-pos)))
                        (view-forward hand-center-pos)
                        charge-state)
                      [])]
    (when (or (seq beam-plan) (seq charge-plan))
      {:ops (vec (concat beam-plan charge-plan))})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:railgun-shot :level] [_ _] {:beam-effects {} :charging {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:railgun-shot :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:railgun-shot :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :railgun-shot
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :railgun-shot [_ store owner-key]
  ;; Only the charge marker is context-bound — it exists to keep the effect
  ;; non-idle while charging, and must stop when the context does.
  ;;
  ;; The beam must NOT be dropped here. Upstream's EntityRailgunFX is a world
  ;; entity spawned by performClient with its own ~2.5 s life; nothing kills it
  ;; when the ability context ends. Railgun's context ends immediately after
  ;; firing (the charge window closes on the same tick the shot goes out, and
  ;; MSG-CTX-TERMINATED then reaches client_ui_hooks' clear-effect-owner!),
  ;; so clearing :beam-effects here deleted every beam a tick or two after it
  ;; was created — the shot landed and dealt damage, the charge animation
  ;; played, and the beam itself was never drawn. Live beams expire on their
  ;; own ttl (beam-life-ticks) via tick-state!.
  (update store :charging dissoc owner-key))
