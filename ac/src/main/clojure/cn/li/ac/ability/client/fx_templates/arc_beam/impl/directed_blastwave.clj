(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.directed-blastwave
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam])
  (:import [cn.li.mcmod.math V3]))

(def ^:private sound-id (modid/namespaced-path "vecmanip.directed_blast"))
(def ^:private wave-life 15)

;; ---------------------------------------------------------------------------
;; Level runtime — the WaveEffect (rings + world sound) spawned on perform.
;; Upstream s_perform sends MSG_PERFORM to every recipient and each c_perform
;; then triggers effectAt locally (no isLocal gate), so the wave is
;; world-rendered by owner and bystanders alike. The wave is a spawned
;; WaveEffect entity upstream — nothing kills it when the context ends; it
;; expires on its own ttl.
;; ---------------------------------------------------------------------------

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:waves {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode pos look-dir source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :perform
      (let [wave-entry (when (map? pos)
                         (let [d (or look-dir {:x 0.0 :y 0.0 :z 1.0})
                               len (Math/sqrt (+ (* (:x d) (:x d))
                                                 (* (:y d) (:y d))
                                                 (* (:z d) (:z d))))
                               inv (/ 1.0 (max 1.0e-6 len))
                               dir {:x (* (double (:x d)) inv)
                                    :y (* (double (:y d)) inv)
                                    :z (* (double (:z d)) inv)}
                               rings (+ 2 (rand-int 2))]
                           (merge base-meta
                                  {:pos {:x (double (:x pos)) :y (double (:y pos)) :z (double (:z pos))}
                                   :dir dir
                                   :ttl wave-life :max-ttl wave-life
                                   :rings (vec (map (fn [idx]
                                                      {:life (+ 8 (rand-int 5))
                                                       :offset (+ (* idx 1.5) (- (* (rand) 0.6) 0.3))
                                                       :size (* 1.0 (+ 0.8 (* (rand) 0.4)))
                                                       :time-offset (+ (* idx 2) (- (rand-int 3) 1))})
                                                    (range rings)))})))
            updated-store (if wave-entry
                            (update-in store* [:waves owner-key*] (fnil conj []) wave-entry)
                            store*)]
        (client-sounds/queue-current-sound-effect!
          {:type :sound :sound-id sound-id :volume 0.5 :pitch 1.0
           :x (double (or (:x pos) 0.0))
           :y (double (or (:y pos) 0.0))
           :z (double (or (:z pos) 0.0))})
        updated-store)
      store*)))

(defn- tick-state!
  [store]
  (let [state* (or store {:waves {}})]
    (assoc state* :waves (store-tick/tick-ttl-items-by-owner (:waves state*)))))

(defn- alpha-curve
  [t]
  (cond
    (< t 0.0) 0.0
    (< t 0.2) (/ t 0.2)
    (< t 0.8) 1.0
    (< t 1.0) (- 1.0 (/ (- t 0.8) 0.2))
    :else 0.0))

(defn- size-scale
  [ticks]
  (let [x (min 1.62 (max 0.0 (/ (double ticks) 20.0)))]
    (cond
      (< x 0.2) (+ 0.4 (* (/ x 0.2) (- 0.8 0.4)))
      (<= x 1.62) (+ 0.8 (* (/ (- x 0.2) (- 1.62 0.2)) (- 1.5 0.8)))
      :else 1.5)))

(defn- basis
  [^V3 dir]
  (let [n-dir (vec3/vnorm dir)
        up-axis (if (> (Math/abs (.-y n-dir)) 0.95)
                  vec3/unit-x
                  vec3/unit-y)
        right (vec3/vnorm (vec3/vcross n-dir up-axis))
        up (vec3/vnorm (vec3/vcross right n-dir))]
    [right up n-dir]))

(defn- wave-ops
  [{:keys [pos dir ttl max-ttl rings]}]
  (let [^V3 pos (vec3/map->v3 pos)
        ^V3 dir (vec3/map->v3 dir)
        ticks (- (long max-ttl) (long ttl))
        max-alpha (alpha-curve (/ (double ticks) (double (max 1 max-ttl))))
        ss (size-scale ticks)
        [right up forward] (basis dir)
        z-offset (/ (double ticks) 40.0)]
    (vec
      (mapcat
        (fn [{:keys [life offset size time-offset]}]
          (let [local-t (- (/ (double ticks) (double (max 1 life))) (/ (double time-offset) (double (max 1 life))))
                alpha (alpha-curve local-t)
                real-alpha (min max-alpha alpha)]
            (if (<= real-alpha 0.0)
              []
              (let [center (vec3/v+ pos (vec3/v* forward (+ (double offset) z-offset)))
                    ring-size (* (double size) ss)
                    side (vec3/v* right (* ring-size 0.5))
                    vertical (vec3/v* up (* ring-size 0.5))
                    p0 (vec3/v+ (vec3/v- center side) vertical)
                    p1 (vec3/v+ (vec3/v+ center side) vertical)
                    p2 (vec3/v- (vec3/v+ center side) vertical)
                    p3 (vec3/v- (vec3/v- center side) vertical)
                    alpha-i (int (max 0 (min 255 (* 255.0 real-alpha 0.7))))]
                [(ru/quad-op (modid/namespaced-path "textures/effects/glow_circle.png")
                             p0 p1 p2 p3
                             {:r 255 :g 255 :b 255 :a alpha-i})]))))
        rings))))

(defn- build-plan
  [_camera-pos _hand-center-pos _tick]
  (let [{:keys [waves]} (arc-beam/snapshot :directed-blastwave {:runtime :level})
        wave-plan (mapcat wave-ops (mapcat val waves))]
    (when (seq wave-plan)
      {:ops (vec wave-plan)})))

;; ---------------------------------------------------------------------------
;; Hand runtime — the raise-hand (prepare) and punch animations.
;; Upstream l_handEffectStart plays AnimPresets.createPrepareAnim on key-down
;; (0.15s raise, then held) and l_effect plays createPunchAnim on perform
;; (0.3s punch). Both are first-person hand-render overrides that only make
;; sense for the caster — the channels are owner-only sends, matching the
;; original's isLocal gates. The curve control points below are a direct
;; transcription of AnimPresets.createPrepareAnim / createPunchAnim.
;; ---------------------------------------------------------------------------

(def ^:private prepare-duration-ms 150.0)
(def ^:private punch-duration-ms 300.0)

(defn- now-ms [] (System/currentTimeMillis))

(defn- prepare-transform [progress]
  {:tx (hand-effects/sample-curve [[0.0 0.0] [1.0 -0.02]] progress)
   :ty (hand-effects/sample-curve [[0.0 0.0] [0.5 0.2] [1.0 0.4]] progress)
   :tz (hand-effects/sample-curve [[0.0 0.0] [1.0 -0.05]] progress)
   :rot-x (hand-effects/sample-curve [[0.0 0.0] [1.0 -20.0]] progress)
   :rot-y 0.0
   :rot-z 0.0})

(defn- punch-transform [progress]
  {:tx (hand-effects/sample-curve [[0.0 -0.04] [0.5 -0.04] [1.0 0.0]] progress)
   :ty (hand-effects/sample-curve [[0.0 0.8] [0.5 0.75] [1.0 0.0]] progress)
   :tz (hand-effects/sample-curve [[0.0 0.0] [0.3 -0.4] [1.0 0.0]] progress)
   :rot-x (hand-effects/sample-curve [[0.0 -40.0] [0.5 -45.0] [1.0 0.0]] progress)
   :rot-y (hand-effects/sample-curve [[0.0 0.0] [0.3 10.0] [1.0 0.0]] progress)
   :rot-z 0.0})

(defn- enqueue-hand-state!
  [state ctx-id channel owner-key payload]
  (let [state* (or state {:effect-state {}})
        {:keys [mode performed? source-player-id world-id]} payload
        owner-key* (or owner-key [:ctx ctx-id])
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (update state* :effect-state assoc owner-key*
              (merge base-meta {:stage :prepare :started-at (now-ms)}))
      :punch
      (update state* :effect-state assoc owner-key*
              (merge base-meta {:stage :punch :started-at (now-ms)}))
      ;; Upstream stopInterrupt on MSG_TERMINATED cuts the hand override even
      ;; after a successful punch; the punch stage here is time-bounded anyway
      ;; (tick removes it after punch-duration-ms), so a performed context just
      ;; lets it finish while an aborted one snaps the hand back immediately.
      :end
      (if performed?
        state*
        (update state* :effect-state dissoc owner-key*))
      state*)))

(defn- tick-hand-state!
  [state]
  (let [state* (or state {:effect-state {}})]
    (update state* :effect-state
            (fn [states]
              (into {}
                    (remove (fn [[_ {:keys [stage started-at]}]]
                              (and (= stage :punch)
                                   (>= (- (now-ms) (long started-at)) punch-duration-ms))))
                    states)))))

(defn- hand-transform []
  (when-let [[owner-key {:keys [stage started-at]}]
             (some (fn [[owner-key st]]
                     (when (:stage st)
                       [owner-key st]))
                   (:effect-state (arc-beam/snapshot :directed-blastwave)))]
    (let [elapsed (- (now-ms) (long started-at))]
      (case stage
        :prepare
        (prepare-transform (min 1.0 (/ elapsed prepare-duration-ms)))
        :punch
        (let [progress (/ elapsed punch-duration-ms)]
          (if (>= progress 1.0)
            (do
              (arc-beam/clear-owner! :directed-blastwave owner-key)
              nil)
            (punch-transform progress)))
        nil))))

;; ---------------------------------------------------------------------------
;; Registration
;; ---------------------------------------------------------------------------

(defmethod arc-beam/effect-initial-state [:directed-blastwave :level] [_ _] {:waves {}})
(defmethod arc-beam/effect-enqueue-state! [:directed-blastwave :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod arc-beam/effect-tick-state! [:directed-blastwave :level] [_ _ store] (tick-state! store))
(defmethod arc-beam/effect-build-plan :directed-blastwave
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod arc-beam/effect-initial-state [:directed-blastwave :hand] [_ _] {:effect-state {}})
(defmethod arc-beam/effect-enqueue-state! [:directed-blastwave :hand]
  [_ _ store ctx-id channel owner-key payload] (enqueue-hand-state! store ctx-id channel owner-key payload))
(defmethod arc-beam/effect-tick-state! [:directed-blastwave :hand] [_ _ store] (tick-hand-state! store))
(defmethod arc-beam/effect-transform-fn :directed-blastwave [_effect-id] (hand-transform))
(defmethod arc-beam/effect-clear-owner! :directed-blastwave [_ store owner-key]
  ;; Hand override is context-bound (upstream stopInterrupt); the wave is a
  ;; spawned WaveEffect that nothing kills when the context ends — it expires
  ;; on its own ttl, like the railgun-shot precedent.
  (if (contains? store :effect-state)
    (update store :effect-state dissoc owner-key)
    store))
