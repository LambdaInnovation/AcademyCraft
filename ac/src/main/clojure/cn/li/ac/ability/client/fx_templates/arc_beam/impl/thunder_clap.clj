(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.thunder-clap
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

;; ---------------------------------------------------------------------------
;; Charging visuals — EntitySurroundArc (BOLD) + EntityRippleMark
;; ---------------------------------------------------------------------------

(def ^:private player-body-width 0.78)    ;; player width 0.6 * 1.3 (sizeMultiplyer)
(def ^:private player-body-height 2.34)   ;; player height 1.8 * 1.3
(def ^:private surround-arc-count 5)
(def ^:private surround-arc-scale 0.3)
(def ^:private surround-arc-life 30)
(def ^:private surround-template-count 10)

(defn- cube-point
  "Uniform random point on the surface of a (w, h, l) cube centered on the
  origin — matching upstream CubePointFactory (faces 0..5: -Y +Y -Z +Z -X +X)."
  [w h l]
  (let [half-w (/ (double w) 2.0)
        half-l (/ (double l) 2.0)
        a (rand)
        b (rand)
        face (rand-int 6)]
    (case face
      (0 1) (rv3/v3 (- (* a w) half-w) (if (zero? face) 0.0 (double h)) (- (* b l) half-l))
      (2 3) (rv3/v3 (- (* b w) half-w) (* a h) (+ (if (= face 2) 0.0 (double l)) (- half-l)))
      (rv3/v3 (+ (if (= face 4) 0.0 (double w)) (- half-w)) (* a h) (- (* b l) half-l)))))

(defn- generate-surround-batch
  "A fresh batch: 10 BOLD templates + 5 SubArcs — matching
  EntitySurroundArc.onFirstUpdate doGenerate (BOLD count 5, random
  cube-surface spawn points, random 0..360 rotations, random template,
  life 30 ticks, initially drawn)."
  []
  (let [templates (arc-fx/surround-arc-templates)]
    {:templates templates
     :arcs (vec (for [_ (range surround-arc-count)]
                  {:pos (cube-point player-body-width player-body-height
                                    player-body-width)
                   :rot-x (rand 360.0)
                   :rot-y (rand 360.0)
                   :rot-z (rand 360.0)
                   :tex-id (rand-int surround-template-count)
                   :tick 0
                   :life surround-arc-life
                   :draw true
                   :dead false}))}))

(defn- tick-sub-arc
  "Advance one SubArc a tick — matching SubArc.tick: template re-roll with
  p=0.5*frameRate (0.6), tick advance with p=0.9, death at life 30, draw
  flicker off with p=0.4*switchRate (0.7) / on with p=0.3*switchRate."
  [arc]
  (let [tex-id* (if (< (rand) 0.3) (rand-int surround-template-count) (:tex-id arc))
        tick* (if (< (rand) 0.9) (inc (long (or (:tick arc) 0))) (:tick arc))
        draw* (if (:draw arc)
                (if (< (rand) 0.24) false true)
                (if (< (rand) 0.18) true false))]
    (assoc arc :tex-id tex-id* :tick tick* :draw draw*
           :dead (>= tick* (long (or (:life arc) surround-arc-life))))))

(defn- tick-surround
  "Advance a surround batch; regenerate a fresh one when every arc died
  (EntitySurroundArc.onUpdate: if(arcHandler.isEmpty()) doGenerate())."
  [{:keys [templates arcs] :as surround}]
  (let [arcs* (mapv tick-sub-arc arcs)]
    (if (every? :dead arcs*)
      (generate-surround-batch)
      (assoc surround :arcs arcs*))))

(defn- surround-ops
  "Render every live SubArc of a batch around world `center` (V3) — the arcs
  are world-aligned at their cube points, matching upstream (the points are
  absolute player-pos offsets, the per-arc rotations random)."
  [center surround]
  (let [{:keys [templates arcs]} surround]
    (vec (mapcat
           (fn [arc]
             (when (and templates (:draw arc) (not (:dead arc)))
               (arc-fx/surround-arc-ops
                 (nth templates (mod (long (or (:tex-id arc) 0)) (count templates)))
                 (rv3/v+ center (:pos arc))
                 (:rot-x arc) (:rot-y arc) (:rot-z arc)
                 surround-arc-scale)))
           arcs))))

;; ---- Ripple aim mark (EntityRippleMark) ----

(def ^:private ripple-texture (modid/asset-path "textures" "effects/ripple.png"))
(def ^:private ripple-cycle 3.6)
(def ^:private ripple-offsets [0.0 -1.2 -2.4])
(def ^:private ripple-fade 1.6)

(defn- ripple-alpha
  "Fade envelope over one ripple cycle — matching RippleMarkRender.getAlpha
  (BIN = BOUT = 1.6): fade in, hold, fade out."
  [mod]
  (cond
    (< mod ripple-fade) (/ mod ripple-fade)
    (> mod (- ripple-cycle ripple-fade))
    (- 1.0 (/ (- mod (- ripple-cycle ripple-fade)) ripple-fade))
    :else 1.0))

(defn- ripple-mark-ops
  "The aim-point mark — 3 staggered ripples (matching RippleMarkRender: three
  textured quads offset 0/-1.2/-2.4s over a 3.6s cycle; each shrinks 1.9→1.4,
  rises mod*0.3, fades in/out). The texture is a thin white ring, so the
  visible ring diameter is ≈0.9×size."
  [target ticks]
  (let [tx (double (:x target))
        ty (+ (double (:y target)) 0.03)
        tz (double (:z target))
        dt (* (long (or ticks 0)) 0.05)]
    (vec
     (for [offset ripple-offsets
           :let [mod (rem (- dt offset) ripple-cycle)
                 alpha (max 0.0 (min 1.0 (ripple-alpha mod)))]
           :when (pos? alpha)
           :let [size (- 1.9 (* 0.5 (/ mod ripple-cycle)))
                 h (+ ty (* mod 0.3))
                 half (/ size 2.0)
                 p0 (rv3/v3 (- tx half) h (- tz half))
                 p1 (rv3/v3 (+ tx half) h (- tz half))
                 p2 (rv3/v3 (+ tx half) h (+ tz half))
                 p3 (rv3/v3 (- tx half) h (+ tz half))]]
       (ru/quad-op ripple-texture p0 p1 p2 p3
                   {:r 204 :g 204 :b 204 :a (int (* 255 alpha))})))))

(defn- strike-impact
  "One strike record. The bolt geometry is generated here, once, and carried
  for the record's whole life — the L-system is randomised, so rebuilding it
  per frame would make the channel crawl instead of holding its shape while
  it flickers out."
  [target charge-ratio ttl]
  {:target target
   :ttl ttl
   :max-ttl ttl
   :charge-ratio (double (or charge-ratio 0.0))
   :bolt (arc-fx/strike-bolt-segments target)})

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:effect-state {} :tails {} :impacts {}})
        {:keys [mode ticks charge-ratio target caster-pos performed? source-player-id world-id]} (or payload {})
        owner-key* (or owner-key [:ctx ctx-id])
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}
        effect-state (:effect-state store*)
        current-st (get effect-state owner-key*)]
    (case mode
      :start
      (assoc-in store* [:effect-state owner-key*]
                (merge base-meta
                       {:active? true
                        :ticks 0
                        :charge-ratio 0.0
                        :target nil
                        :caster-pos caster-pos
                        :performed? false
                        :surround (generate-surround-batch)}))

      :update
      (assoc-in store* [:effect-state owner-key*]
                (merge base-meta
                       current-st
                       {:active? true
                        :ticks (long (or ticks 0))
                        :charge-ratio (double (or charge-ratio 0.0))
                        :target target
                        :caster-pos (or caster-pos (:caster-pos current-st))}))

      :perform
      (let [next-store (assoc-in store* [:effect-state owner-key*]
                                 (merge base-meta
                                        current-st
                                        {:active? true
                                         :ticks (long (or ticks (:ticks current-st) 0))
                                         :charge-ratio (double (or charge-ratio (:charge-ratio current-st) 0.0))
                                         :target (or target (:target current-st))
                                         :caster-pos (or caster-pos (:caster-pos current-st))
                                         :performed? true}))]
        (if (map? target)
          (update-in next-store [:impacts owner-key*] (fnil conj [])
                     (merge base-meta (strike-impact target charge-ratio 8)))
          next-store))

      :end
      (let [without-active (update store* :effect-state dissoc owner-key*)
            ;; Original kills the caster-only ripple immediately but delays
            ;; EntitySurroundArc removal by 10 ticks.
            next-store (if current-st
                         (assoc-in without-active [:tails owner-key*]
                                   (merge base-meta current-st
                                          {:active? false :ttl 10}))
                         without-active)]
        (if (and (map? target) performed?)
          (update-in next-store [:impacts owner-key*] (fnil conj [])
                     (merge base-meta (strike-impact target charge-ratio 6)))
          next-store))

      store*)))

(defn- tick-active-states
  "Keep :active? states, increment :ticks, and advance their surround-arc
  batches (the arcs keep flickering while charging, as upstream's
  EntitySurroundArc.onUpdate runs every tick)."
  [by-owner]
  (persistent!
   (reduce-kv (fn [acc owner-key st]
                (if (:active? st)
                  (assoc! acc owner-key
                          (-> st
                              (update :ticks (fnil inc 0))
                              (update :surround tick-surround)))
                  acc))
              (transient {})
              by-owner)))

(defn- tick-tail-states
  "Decrement :ttl on tail states (drop at 0), and keep the surround arcs
  flickering during the 10-tick removal delay — upstream's surroundArc keeps
  updating until it dies."
  [by-owner]
  (persistent!
   (reduce-kv (fn [acc owner-key st]
                (let [ttl (dec (long (or (:ttl st) 0)))]
                  (if (pos? ttl)
                    (assoc! acc owner-key
                            (-> st
                                (assoc :ttl ttl)
                                (update :ticks (fnil inc 0))
                                (update :surround tick-surround)))
                    acc)))
              (transient {})
              by-owner)))

(defn- tick-state!
  [store]
  (let [store* (or store {:effect-state {} :tails {} :impacts {}})]
    (-> store*
        (update :effect-state tick-active-states)
        (update :tails tick-tail-states)
        (update :impacts store-tick/tick-ttl-items-by-owner))))

(defn- bolt-alpha
  "Flash envelope for the descending channel: bright on the strike frame,
  fading out, with the odd dark frame so it reads as a real bolt's restrikes
  rather than a light source dimming smoothly."
  [ttl max-ttl]
  (let [ttl (long (or ttl 0))
        life (if (pos? (long (or max-ttl 0)))
               (/ (double ttl) (double max-ttl))
               0.0)]
    (cond
      (zero? ttl) 0.0
      ;; Vanilla LightningBolt re-seeds itself a couple of times before it
      ;; dies; the gaps between those restrikes are what makes lightning
      ;; flicker instead of glow.
      (zero? (mod ttl 3)) (* 0.25 life)
      :else life)))

(defn- impact-ops [{:keys [target ttl max-ttl charge-ratio bolt]}]
  (let [life (if (pos? (long (or max-ttl 0)))
               (/ (double (or ttl 0)) (double max-ttl))
               0.0)
        growth (- 1.0 life)
        radius (+ 0.8 (* 0.65 growth) (* 0.2 (double (or charge-ratio 0.0))))
        tx (double (:x target))
        y (+ (double (:y target)) 0.08)
        tz (double (:z target))
        segments 20
        alpha (int (+ 20 (* 160 life)))
        color {:r 220 :g 245 :b 255 :a alpha}
        ground-ring
        (vec
          (for [idx (range segments)
                :let [a0 (/ (* 2.0 Math/PI idx) segments)
                      a1 (/ (* 2.0 Math/PI (inc idx)) segments)
                      p0 (rv3/v3 (+ tx (* radius (Math/cos a0))) y (+ tz (* radius (Math/sin a0))))
                      p1 (rv3/v3 (+ tx (* radius (Math/cos a1))) y (+ tz (* radius (Math/sin a1))))]]
            (ru/line-op p0 p1 color)))]
    (into ground-ring
          (when (seq bolt)
            (arc-fx/bolt-segments->ops bolt (bolt-alpha ttl max-ttl))))))

(defn- live-target
  "The caster's own aim point, recomputed locally.

  Upstream's ThunderClapContextC.c_updateEffect runs its own
  Raytrace.traceLiving every client tick and feeds the result straight to
  mark.setPosition — the ripple mark never waits on the server. The synced
  :target arrives at server-tick rate plus network latency, so using it for
  the caster's own mark left the ring visibly trailing the crosshair while
  turning. Fall back to the synced value when the loader has no local aim op."
  [synced-target]
  (or (client-bridge/local-player-block-aim
        (skill-config/tunable-double :thunder-clap :targeting.range))
      synced-target))

(defn- local-walk-speed [ticks]
  (let [max-speed 0.1
        min-speed 0.001
        value (- max-speed (* (/ (- max-speed min-speed) 60.0) (double ticks)))]
    (float (max min-speed value))))

(defn- own-state?
  "True when st belongs to the local viewer (nil source-player-id/hand-center-pos
  is treated as a match, same as before — used only as a defensive fallback)."
  [st hand-center-pos]
  (or (nil? (:source-player-id st))
      (nil? (:player-uuid hand-center-pos))
      (= (str (:source-player-id st)) (str (:player-uuid hand-center-pos)))))

(defn- build-plan [_camera-pos hand-center-pos _tick]
  (let [{:keys [effect-state tails impacts]} (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :thunder-clap)
        active-states (filter :active? (vals effect-state))
        own-tc (some #(when (own-state? % hand-center-pos) %) active-states)
        ;; The surround arcs are broadcast and public (matches original's
        ;; EntitySurroundArc, spawned for everyone to see); the ripple mark
        ;; stays caster-only (matches original's isLocal EntityRippleMark) —
        ;; per-state below, not per-message, since both ride the same fx.
        charge-ops
        (vec (mapcat
               (fn [st]
                 (let [own? (own-state? st hand-center-pos)
                       ;; Zero-latency live position for the caster's own
                       ;; client; synced :caster-pos (from the fx payload)
                       ;; for everyone else observing this cast.
                       center (if (and own? hand-center-pos)
                                (dissoc hand-center-pos :player-uuid)
                                (:caster-pos st))
                       ticks (long (or (:ticks st) 0))]
                   (concat
                     (when (and (map? center) (:surround st))
                       (surround-ops (rv3/map->v3 center) (:surround st)))
                     (when (and own? (map? (:target st)))
                       (ripple-mark-ops (live-target (:target st)) ticks)))))
               active-states))
        tail-ops
        (vec
          (mapcat (fn [st]
                    (when (and (map? (:caster-pos st)) (:surround st))
                      (surround-ops (rv3/map->v3 (:caster-pos st)) (:surround st))))
                  (vals tails)))
        impact-render-ops (vec (mapcat impact-ops (mapcat val impacts)))
        ws (when own-tc (local-walk-speed (:ticks own-tc)))]
    (when (or (seq charge-ops) (seq tail-ops) (seq impact-render-ops) ws)
      {:ops (vec (concat charge-ops tail-ops impact-render-ops))
       :local-walk-speed ws})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:thunder-clap :level] [_ _] {:effect-state {} :tails {} :impacts {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:thunder-clap :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:thunder-clap :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :thunder-clap
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :thunder-clap [_ store owner-key]
  (-> store
      (update :effect-state dissoc owner-key)
      (update :tails dissoc owner-key)
      (update :impacts dissoc owner-key)))
