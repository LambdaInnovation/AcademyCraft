(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.shift-teleport
  (:require [cn.li.ac.ability.client.effects.billboard-particles :as bp]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

;; Upstream STContextC marker colors: CRL_BLOCK_MARKER (139,139,139,180) for
;; the destination block, CRL_ENTITY_MARKER (235,81,81,180) per target.
(def ^:private color-block-marker {:r 139 :g 139 :b 139 :a 180})
(def ^:private color-entity-marker {:r 235 :g 81 :b 81 :a 180})

;; Upstream TPParticleFactory template: the white tp_particle texture, unlit.
;; (The vanilla :portal alias renders the purple portal swirl instead.)
(def ^:private tp-particle-texture
  (modid/asset-path "textures/effects" "tp_particle.png"))

(defn- rand-range
  [a b]
  (+ a (rand (- b a))))

(defn- cube-edges
  "12 wireframe edges of an axis-aligned cube centered at c with half-extents."
  [c half-x half-y half-z color]
  (let [x (double (:x c)) y (double (:y c)) z (double (:z c))
        hx (double half-x) hy (double half-y) hz (double half-z)
        corners [[(- x hx) (- y hy) (- z hz)] [(+ x hx) (- y hy) (- z hz)]
                 [(+ x hx) (- y hy) (+ z hz)] [(- x hx) (- y hy) (+ z hz)]
                 [(- x hx) (+ y hy) (- z hz)] [(+ x hx) (+ y hy) (- z hz)]
                 [(+ x hx) (+ y hy) (+ z hz)] [(- x hx) (+ y hy) (+ z hz)]]
        edges [[0 1] [1 2] [2 3] [3 0]
               [4 5] [5 6] [6 7] [7 4]
               [0 4] [1 5] [2 6] [3 7]]]
    (mapv (fn [[a b]]
            (let [pa (nth corners a) pb (nth corners b)]
              (ru/line-op (rv3/v3 (double (nth pa 0)) (double (nth pa 1)) (double (nth pa 2)))
                          (rv3/v3 (double (nth pb 0)) (double (nth pb 1)) (double (nth pb 2)))
                          color)))
          edges)))

(defn- trail-particles
  "Upstream c_end: walk the player->destination ray, one TPParticleFactory
  particle per step. First step 1.0, then rand(0.6, 1.0); the destination is
  the block CENTER (dest[1] + 0.5); each particle drifts with the upstream
  random velocity and fades after (20, 20) ticks."
  [from-pos to-pos]
  (let [dx (- (double (:x to-pos)) (double (:x from-pos)))
        dy (- (double (:y to-pos)) (double (:y from-pos)))
        dz (- (double (:z to-pos)) (double (:z from-pos)))
        dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
        dv (if (pos? dist)
             {:x (/ dx dist) :y (/ dy dist) :z (/ dz dist)}
             {:x 0.0 :y 0.0 :z 0.0})]
    (loop [pos from-pos
           move 1.0
           x 1.0
           acc []]
      (if (> x dist)
        acc
        (let [p {:x (+ (double (:x pos)) (* (double (:x dv)) move))
                 :y (+ (double (:y pos)) (* (double (:y dv)) move))
                 :z (+ (double (:z pos)) (* (double (:z dv)) move))}]
          (recur p (double (rand-range 0.6 1.0)) (+ x move)
                 (conj acc (assoc p
                                  :vx (rand-range -0.05 0.05)
                                  :vy (rand-range -0.02 0.05)
                                  :vz (rand-range -0.05 0.05)
                                  :size (rand-range 0.1 0.2)
                                  :texture tp-particle-texture
                                  :start-alpha (long (rand-range 153 204))
                                  :age 0 :life 20 :fade-in 5 :fade-out 20))))))))

(defn- enqueue-state! [state ctx-id channel owner-key payload]
  (let [state* (or state {:fx-state {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [source-player-id world-id]} payload
        base-meta {:owner-key owner-key* :ctx-id ctx-id :channel channel
                   :source-player-id source-player-id :world-id world-id}
        target (fn [p]
                 {:x (double (or (:x p) 0.0))
                  :y (double (or (:y p) 0.0))
                  :z (double (or (:z p) 0.0))})]
    (case (:mode payload)
      :start
      (update state* :fx-state assoc owner-key*
              (merge base-meta {:active? true :ttl 0
                                :target (target payload)
                                :target-count (long (or (:target-count payload) 0))
                                :target-hit? (boolean (:target-hit? payload))
                                :hand-valid? (boolean (if (contains? payload :hand-valid?)
                                                        (:hand-valid? payload) true))
                                :entities (vec (:entities payload))}))
      :update
      (update state* :fx-state update owner-key*
              (fn [st]
                (assoc (merge base-meta (or st {:active? true :ttl 0}))
                       :active? true
                       :target (target payload)
                       :target-count (long (or (:target-count payload) 0))
                       :target-hit? (boolean (:target-hit? payload))
                       :hand-valid? (boolean (if (contains? payload :hand-valid?)
                                               (:hand-valid? payload) true))
                       :entities (vec (:entities payload)))))
      :perform
      ;; Upstream c_end: kill the block marker and every target marker, then
      ;; leave the white TPParticleFactory trail — the state lingers (active?
      ;; false) until the particles have faded out.
      (update state* :fx-state update owner-key*
              (fn [st]
                (-> (merge base-meta (or st {:active? true :ttl 0}))
                    (assoc :active? false)
                    (assoc :particles (trail-particles
                                       {:x (double (or (:from-x payload) 0.0))
                                        :y (double (or (:from-y payload) 0.0))
                                        :z (double (or (:from-z payload) 0.0))}
                                       {:x (double (or (:x payload) 0.0))
                                        :y (+ 0.5 (double (or (:y payload) 0.0)))
                                        :z (double (or (:z payload) 0.0))})))))
      :end
      (update state* :fx-state dissoc owner-key*)
      state*)))

(defn- tick-state! [state]
  (let [state* (or state {:fx-state {}})]
    (update state* :fx-state
            (fn [states]
              (reduce-kv
                (fn [acc owner-key st]
                  (if (:active? st)
                    ;; Markers are static during hold; nothing advances.
                    (assoc acc owner-key st)
                    (let [particles (bp/tick-particles! (:particles st))]
                      (if (seq particles)
                        (assoc acc owner-key (assoc st :particles particles))
                        acc))))
                {} states)))))

(defn- build-plan [camera-pos _hand-center-pos _tick]
  (let [cam (rv3/map->v3 camera-pos)
        states (vals (:fx-state (level-effects/effect-state-snapshot :shift-teleport)))
        marker-ops
        (vec
         (mapcat (fn [st]
                   (when (and (:active? st) (:hand-valid? st))
                     (concat
                       ;; Destination block marker: 1.2x1.2x1.2 cube whose
                       ;; BOTTOM sits at the destination block (upstream
                       ;; blockMarker.setPosition(dest[0]+.5, dest[1], ...)
                       ;; with height/width 1.2 — the entity position is the
                       ;; box's feet, so it spans dest[1]..dest[1]+1.2;
                       ;; drawing it centered at dest[1] sank the lower half
                       ;; into the block below).
                       (when-let [t (:target st)]
                         (cube-edges (assoc t :y (+ 0.6 (double (:y t))))
                                     0.6 0.6 0.6 color-block-marker))
                       ;; One red marker per entity in the line (upstream
                       ;; EntityMarker(e): a width x height x width box
                       ;; following the target's feet position).
                       (mapcat (fn [e]
                                 (let [w (double (or (:width e) 0.6))
                                       h (double (or (:height e) 1.8))]
                                   (cube-edges (assoc e :y (+ (/ h 2.0) (double (:y e))))
                                               (/ w 2.0) (/ h 2.0) (/ w 2.0)
                                               color-entity-marker)))
                               (:entities st)))))
                 states))
        ;; White TPParticleFactory trail left by a performed release — lingers
        ;; until the particles fade out.
        particle-ops
        (vec (mapcat (fn [st]
                       (bp/particle-ops cam (:particles st)))
                     states))]
    (when (seq (concat marker-ops particle-ops))
      {:ops (vec (concat marker-ops particle-ops))})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:shift-teleport :level] [_ _] {:fx-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:shift-teleport :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:shift-teleport :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :shift-teleport
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :shift-teleport [_ store owner-key]
  (update store :fx-state dissoc owner-key))
