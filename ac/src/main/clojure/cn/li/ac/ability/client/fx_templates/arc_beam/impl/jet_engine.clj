(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.jet-engine
  (:require [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.effects.beam-ops :as fx-beam]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.runtime :as client-runtime]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [clojure.string :as str]
            [cn.li.ac.ability.client.fx-templates.arc-beam])
  (:import [cn.li.mcmod.math V3]))

(def ^:private local-scripted-effect-key :mcmod/spawn-local-scripted-effect)
(def ^:private local-remove-scripted-effect-key :mcmod/remove-local-scripted-effect)
(def ^:private diamond-shield-effect-id "entity_diamond_shield")


(def ^:private mark-ttl 8)
(def ^:private trigger-ttl 20)
(def ^:private min-segment-length 1.0e-5)

(defn- clamp01 [x]
  (max 0.0 (min 1.0 (double x))))

(defn- lerp-pos ^V3 [^V3 a ^V3 b t]
  (let [k (clamp01 t)]
    (vec3/v+ a (vec3/v* (vec3/v- b a) k))))

(defn- safe-trail-right-axis ^V3 [^V3 dir]
  (let [up-axis (if (> (Math/abs (.-y dir)) 0.95)
                  vec3/unit-x
                  vec3/unit-y)
        right (vec3/vcross dir up-axis)]
    (if (> (vec3/vlen right) min-segment-length)
      (vec3/vnorm right)
      vec3/unit-x)))

(defn- trail-layer-ops [^V3 start ^V3 pos ttl trigger-ticks]
  (let [travel (vec3/v- pos start)
        distance (vec3/vlen travel)]
    (if (< distance min-segment-length)
      []
      (let [dir (vec3/vnorm travel)
            right (safe-trail-right-axis dir)
            ttl-k (clamp01 (/ (double ttl) (double trigger-ttl)))
            base-half (+ 0.08 (* 0.05 ttl-k))]
        (vec
          (mapcat (fn [idx]
                    (let [layer (double idx)
                          head-t (- 1.0 (* 0.07 layer))
                          tail-t (- head-t (+ 0.2 (* 0.09 layer) (* 0.006 (double trigger-ticks))))
                          head (lerp-pos start pos head-t)
                          tail (lerp-pos start pos tail-t)
                          half-width (* base-half (+ 1.0 (* 0.18 layer)))
                          side (vec3/v* right half-width)
                          p0 (vec3/v+ tail side)
                          p1 (vec3/v+ head side)
                          p2 (vec3/v- head side)
                          p3 (vec3/v- tail side)
                          alpha (int (max 0 (min 255 (* 210.0 ttl-k (- 1.0 (* 0.17 layer))))))
                          color (fx-beam/rgba {:r 172 :g 240 :b 255} alpha)]
                      (when (> alpha 0)
                        [(fx-beam/glow-line-quad-op p0 p1 p2 p3 color)
                         (ru/line-op tail head {:r 200 :g 248 :b 255 :a (min 255 (+ 16 alpha))})])))
                  (range 4)))))))

(defn- impact-billboard-ops [^V3 cam-pos ^V3 target ttl trigger-ticks]
  (if (some? cam-pos)
    (let [center (vec3/v3 (.-x target) (+ (.-y target) 0.45) (.-z target))
          right (ru/camera-facing-right-axis center cam-pos)
          up (ru/billboard-up-axis center cam-pos right)
          ttl-k (clamp01 (/ (double ttl) (double trigger-ttl)))
          pulse (+ 0.52 (* 0.14 (Math/sin (* 0.35 (double trigger-ticks)))))
          outer-half (* pulse (+ 1.0 (* 0.2 ttl-k)))
          outer-v (* 0.62 (+ 1.0 (* 0.15 ttl-k)))
          inner-half (* outer-half 0.62)
          inner-v (* outer-v 0.62)
          outer-side (vec3/v* right outer-half)
          outer-up (vec3/v* up outer-v)
          inner-side (vec3/v* right inner-half)
          inner-up (vec3/v* up inner-v)
          o0 (vec3/v+ (vec3/v- center outer-side) outer-up)
          o1 (vec3/v+ (vec3/v+ center outer-side) outer-up)
          o2 (vec3/v- (vec3/v+ center outer-side) outer-up)
          o3 (vec3/v- (vec3/v- center outer-side) outer-up)
          i0 (vec3/v+ (vec3/v- center inner-side) inner-up)
          i1 (vec3/v+ (vec3/v+ center inner-side) inner-up)
          i2 (vec3/v- (vec3/v+ center inner-side) inner-up)
          i3 (vec3/v- (vec3/v- center inner-side) inner-up)
          outer-a (int (max 0 (min 255 (* 165.0 ttl-k))))
          inner-a (int (max 0 (min 255 (* 210.0 ttl-k))))]
      [(ru/quad-op (modid/namespaced-path "textures/effects/glow_circle.png") o0 o1 o2 o3 {:r 145 :g 220 :b 255 :a outer-a})
       (ru/quad-op (modid/namespaced-path "textures/effects/glow_circle.png") i0 i1 i2 i3 {:r 225 :g 252 :b 255 :a inner-a})])
    []))

(defn- impact-spike-ops [^V3 target ttl trigger-ticks]
  (let [cx (.-x target) cy (+ (.-y target) 0.08) cz (.-z target)
        center (vec3/v3 cx cy cz)
        ttl-k (clamp01 (/ (double ttl) (double trigger-ttl)))
        radius (+ 0.38 (* 0.18 (Math/sin (* 0.26 (double trigger-ticks)))))
        y-lift (+ 0.06 (* 0.03 ttl-k))
        alpha (int (max 0 (min 255 (* 200.0 ttl-k))))
        color {:r 214 :g 248 :b 255 :a alpha}
        inner {:r 180 :g 230 :b 255 :a (int (max 0 (min 255 (* 135.0 ttl-k))))}
        base (vec3/v3 cx (+ cy 0.16) cz)]
    (vec
      (mapcat (fn [idx]
                (let [a (/ (* 2.0 Math/PI idx) 8.0)
                      tip (vec3/v3 (+ cx (* radius (Math/cos a)))
                                   (+ cy y-lift)
                                   (+ cz (* radius (Math/sin a))))]
                  [(ru/line-op center tip color)
                   (ru/line-op base tip inner)]))
              (range 8)))))









;; RippleMarkRender: three flat quads on the aim point's XZ plane, staggered
;; across one 3.6-second cycle, each rising while it shrinks and fading in and
;; out over 1.6 seconds at either end. Drawn with depth test and depth write
;; off so the mark reads on top of the surface it lands on.
(def ^:private ripple-texture (modid/namespaced-path "textures/effects/ripple.png"))
(def ^:private ripple-cycle-seconds 3.6)
(def ^:private ripple-time-offsets [0.0 -1.2 -2.4])
(def ^:private ripple-blend-seconds 1.6)
(def ^:private ripple-size-from 1.9)
(def ^:private ripple-size-to 1.4)
(def ^:private ripple-rise-per-second 0.3)

(defn- ripple-alpha
  ^double [^double m]
  (cond
    (< m ripple-blend-seconds)
    (/ m ripple-blend-seconds)

    (> m (- ripple-cycle-seconds ripple-blend-seconds))
    (- 1.0 (/ (- m (- ripple-cycle-seconds ripple-blend-seconds)) ripple-blend-seconds))

    :else 1.0))

(defn- ripple-ops
  "One EntityRippleMark: its three staggered ripples at `seconds` since the
  mark appeared. getAlpha's curve REPLACES the mark colour's own alpha
  upstream (material.color.setAlpha), so 51/255/51's 179 never reaches the
  screen — the ripples run 0..255 on the fade curve."
  [^V3 target ^double seconds rgb]
  (vec
    (keep
      (fn [^double offset]
        (let [m (mod (- seconds offset) ripple-cycle-seconds)
              size (+ ripple-size-from
                      (* (- ripple-size-to ripple-size-from) (/ m ripple-cycle-seconds)))
              half (* 0.5 size)
              x (.-x target)
              y (+ (.-y target) (* m ripple-rise-per-second))
              z (.-z target)
              a (int (* 255.0 (clamp01 (ripple-alpha m))))]
          (when (pos? a)
            (assoc
              (ru/quad-op ripple-texture
                          (vec3/v3 (- x half) y (- z half))
                          (vec3/v3 (+ x half) y (- z half))
                          (vec3/v3 (+ x half) y (+ z half))
                          (vec3/v3 (- x half) y (+ z half))
                          (assoc rgb :a a))
              :no-depth-test? true))))
      ripple-time-offsets)))

(defn- ring-ops [^V3 target radius color]
  (let [segments 24
        tx (.-x target) tz (.-z target)
        y (+ (.-y target) 0.05)]
    (vec
      (for [idx (range segments)
            :let [a0 (/ (* 2.0 Math/PI idx) segments)
                  a1 (/ (* 2.0 Math/PI (inc idx)) segments)
                  p0 (vec3/v3 (+ tx (* radius (Math/cos a0))) y (+ tz (* radius (Math/sin a0))))
                  p1 (vec3/v3 (+ tx (* radius (Math/cos a1))) y (+ tz (* radius (Math/sin a1))))]]
        (ru/line-op p0 p1 color)))))

(defn- spawn-diamond-shield!
  []
  (let [entity-uuid (client-bridge/run-client-effect! local-scripted-effect-key
                                                       {:effect-id diamond-shield-effect-id})]
    (when (seq entity-uuid)
      entity-uuid)))

(defn- remove-diamond-shield!
  [entity-uuid]
  (when (seq entity-uuid)
    (client-bridge/run-client-effect! local-remove-scripted-effect-key
                                      {:entity-uuid entity-uuid})))

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:fx-state {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode start target pos owner-pos hold-ticks trigger-ticks shield-entity-uuid]} (or payload {})
        ;; Capture the effect owner at enqueue time (fx events run with the
        ;; client session bound) so the per-tick particle queueing in
        ;; tick-state! can resolve its session partition — the ClientTick
        ;; path that drives tick-state! has no session context of its own.
        queue-owner (client-particles/current-effect-owner)]
    (case mode
      :mark-start
      (do
        ;; TODO(sound): original JetEngine plays no skill sound; the previous
        ;; md.jet_charge loop was an unverified placeholder — restore once a
        ;; fitting sound is found.
        #_(client-sounds/queue-current-sound-effect!
           {:type :sound :sound-id (modid/namespaced-path "md.jet_charge") :volume 0.45 :pitch 1.0})
        (assoc-in store* [:fx-state owner-key*]
                  {:queue-owner queue-owner
                   :phase :marking
                   :target target
                   :hold-ticks (long (or hold-ticks 0))
                   :ttl mark-ttl}))

      :mark-update
      (assoc-in store* [:fx-state owner-key*]
                (merge (get-in store* [:fx-state owner-key*])
                       {:queue-owner queue-owner
                        :phase :marking
                        :target target
                        :hold-ticks (long (or hold-ticks 0))
                        :ttl mark-ttl}))

      :mark-end
      (let [st (get-in store* [:fx-state owner-key*])]
        (if (= :triggering (:phase st))
          store*
          (update store* :fx-state dissoc owner-key*)))

      :trigger-start
      (let [prev-state (get-in store* [:fx-state owner-key*])
            entering-trigger? (not= :triggering (:phase prev-state))
            spawned-uuid (when entering-trigger?
                           ;; Keep parity with upstream JetEngine: spawn diamond-shield once on trigger phase entry.
                           (spawn-diamond-shield!))]
        (when entering-trigger?
          nil
          ;; TODO(sound): original JetEngine plays no skill sound — restore
          ;; once a fitting sound is found.
          #_(client-sounds/queue-current-sound-effect!
             {:type :sound :sound-id (modid/namespaced-path "md.jet_engine") :volume 0.8 :pitch 1.0}))
        (assoc-in store* [:fx-state owner-key*]
                  (merge prev-state
                         {:queue-owner (or (:queue-owner prev-state) queue-owner)
                          :phase :triggering
                          :start start
                          :target target
                          :pos (or pos start)
                          :trigger-ticks (long (or trigger-ticks 0))
                          :ttl trigger-ttl
                          :shield-entity-uuid (or spawned-uuid
                                                  (:shield-entity-uuid prev-state))})))

      :trigger-update
      (assoc-in store* [:fx-state owner-key*]
                (merge (get-in store* [:fx-state owner-key*])
                       {:phase :triggering
                        :pos pos
                        :owner-pos owner-pos
                        :trigger-ticks (long (or trigger-ticks 0))
                        :shield-entity-uuid (or shield-entity-uuid
                                                (get-in store* [:fx-state owner-key* :shield-entity-uuid]))
                        :ttl trigger-ttl}))

      :trigger-end
      (let [st (get-in store* [:fx-state owner-key*])
            shield-entity-uuid (:shield-entity-uuid st)]
        (remove-diamond-shield! shield-entity-uuid)
        (update store* :fx-state dissoc owner-key*))

      store*)))

(defn- tick-state!
  [store]
  (let [store* (or store {:fx-state {}})]
    ;; c_tUpdateEffect: `for (i <- 0 to 10)` is 11 MdParticles per tick — soft
    ;; md_particle dots at player.pos +/- 0.3, not vanilla spark lines.
    (doseq [[_ st] (:fx-state store*)]
      (when (= :triggering (:phase st))
        (let [pos (or (:owner-pos st) (:pos st))]
          (dotimes [_ 11]
            (client-particles/queue-particle-effect! (:queue-owner st)
              {:type :particle :particle-type (modid/namespaced-path "md_particle")
               :x (+ (double (:x pos)) (- (rand 0.6) 0.3))
               :y (+ (double (:y pos)) (- (rand 0.6) 0.3))
               :z (+ (double (:z pos)) (- (rand 0.6) 0.3))
               ;; A single particle takes offset-* * speed as its velocity
               ;; verbatim (see the mcbase particle bridge); :motion-* is not
               ;; read, so every mote drifted the same fixed 0.002 diagonal
               ;; instead of the original's ranged(-.02, .02) per axis.
               :count 1 :speed 1.0
               :offset-x (- (rand 0.04) 0.02)
               :offset-y (- (rand 0.04) 0.02)
               :offset-z (- (rand 0.04) 0.02)})))))
    (update store* :fx-state
      (fn [states]
        (into {}
              (keep (fn [[owner-key st]]
                      (let [ttl (long (or (:ttl st) 0))]
                        (if (> ttl 1)
                          [owner-key (update st :ttl dec)]
                          (do
                            (remove-diamond-shield! (:shield-entity-uuid st))
                            nil)))))
              states)))))

(defn- build-plan
  [camera-pos _hand-center-pos _tick]
  (let [^V3 cam-v (when (map? camera-pos) (vec3/map->v3 camera-pos))
        states (vals (:fx-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :jet-engine)))
        marking-states (filter #(= :marking (:phase %)) states)
        triggering-states (filter #(= :triggering (:phase %)) states)
        mark-ops (vec
                   (mapcat (fn [st]
                             (when-let [target (:target st)]
                               ;; Green, matching JetEngine's mark.color.set(51, 255, 51, 179).
                               ;; The port drew a single pulsing line ring here; the original is
                               ;; EntityRippleMark, three textured ripples expanding out of the
                               ;; aim point on a 3.6s loop.
                               (ripple-ops (vec3/map->v3 target)
                                           (/ (double (or (:hold-ticks st) 0)) 20.0)
                                           {:r 51 :g 255 :b 51})))
                           marking-states))
        trigger-ops (vec
                      (mapcat (fn [st]
                                (let [ttl (long (or (:ttl st) 0))
                                      start (:start st)
                                      pos (:pos st)
                                      target (:target st)
                                      trigger-ticks (long (or (:trigger-ticks st) 0))
                                      alpha (int (* 215 (/ (double ttl) (double trigger-ttl))))
                                      impact-color {:r 210 :g 250 :b 255 :a (min 180 (+ 40 alpha))}
                                      impact-radius (+ 0.45 (* 0.18 (Math/sin (* 0.3 (double trigger-ticks)))))]
                                  (when (pos? ttl)
                                    (concat
                                      (when (and start pos)
                                        (trail-layer-ops (vec3/map->v3 start) (vec3/map->v3 pos) ttl trigger-ticks))
                                      (when target
                                        (let [target-v (vec3/map->v3 target)]
                                          (concat
                                            (ring-ops target-v impact-radius impact-color)
                                            (impact-spike-ops target-v ttl trigger-ticks)
                                            (impact-billboard-ops cam-v target-v ttl trigger-ticks))))))))
                              triggering-states))
        ;; Screen-flash intensity during trigger is computed by the content
        ;; layer's jet-engine-fx/flash-alpha (same formula, player-scoped) for
        ;; the 2D overlay — this :ops vector is pure 3D world-space geometry,
        ;; it has no route to a full-screen tint.
        ws (when (seq triggering-states) 0.07)  ;; walk speed during trigger (matching original)
        ops (into mark-ops trigger-ops)]
    (cond-> (when (seq ops) {:ops ops})
      ws (assoc :local-walk-speed (float ws)))))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:jet-engine :level] [_ _] {:fx-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:jet-engine :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:jet-engine :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :jet-engine
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :jet-engine [_ store owner-key]
  (update store :fx-state dissoc owner-key))
