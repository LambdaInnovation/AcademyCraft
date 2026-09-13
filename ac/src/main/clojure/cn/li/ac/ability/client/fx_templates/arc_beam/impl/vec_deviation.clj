(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.vec-deviation
  "Client FX for VecDeviation: the same WaveEffect VecReflection spawns, with
  one ring instead of two and 0.6 instead of 1.1."
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.effects.wave-effect :as wave]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private sound-id (modid/namespaced-path "vecmanip.vec_deviation"))

;; VecDeviation spawns WaveEffect(world, rings = 1, size = 0.6).
(def ^:private wave-rings 1)
(def ^:private wave-size 0.6)

(defn- queue-deviation-sound!
  [x y z]
  (client-sounds/queue-current-sound-effect!
    {:type :sound :sound-id sound-id :volume 0.5 :pitch 1.0
     :x (double (or x 0.0)) :y (double (or y 0.0)) :z (double (or z 0.0))}))









(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:effect-state {} :wave-effects {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode x y z marked? entity-uuid yaw-rad pitch-rad
                source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (assoc-in store* [:effect-state owner-key*]
                (merge base-meta {:active? true :ticks 0}))
      :end
      (assoc-in store* [:effect-state owner-key*]
                (merge base-meta {:active? false :ticks 0}))
      :stop-entity
      (do
        ;; c_stopEntity zeroes the entity's motion OUTSIDE the isMarked branch:
        ;; the projectile stops locally the instant the message lands, marked
        ;; or not, instead of coasting until the next velocity sync.
        (when entity-uuid
          (client-bridge/run-client-effect!
            :mcmod/set-client-entity-motion
            {:entity-uuid (str entity-uuid) :vx 0.0 :vy 0.0 :vz 0.0}))
        (if marked?
          (do
            ;; ...and the wave and its sound only when it is.
            (queue-deviation-sound! x y z)
            (update-in store* [:wave-effects owner-key*]
              (fnil conj [])
              (merge base-meta
                     {:x (double (or x 0.0))
                      :y (double (or y 0.0))
                      :z (double (or z 0.0))
                      :yaw-rad (double (or yaw-rad 0.0))
                      :pitch-rad (double (or pitch-rad 0.0))
                      :rings (wave/build-rings wave-rings wave-size)
                      :ttl wave/life-ticks
                      :max-ttl wave/life-ticks})))
          store*))
      :play
      (do
        (queue-deviation-sound! x y z)
        store*)
      store*)))

(defn- tick-state!
  [store]
  (let [state* (or store {:effect-state {} :wave-effects {}})]
    (assoc state*
           :effect-state (store-tick/keep-active-inc-ticks (:effect-state state*))
           :wave-effects (store-tick/tick-ttl-items-by-owner (:wave-effects state*)))))

(defn- build-plan
  [_camera-pos _hand-center-pos _tick]
  (let [{:keys [wave-effects]} (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :vec-deviation)
        wave-plan (mapcat wave/ops (mapcat val wave-effects))]
    (when (seq wave-plan)
      {:ops (vec wave-plan)})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:vec-deviation :level] [_ _] {:effect-state {} :wave-effects {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:vec-deviation :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:vec-deviation :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :vec-deviation
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :vec-deviation [_ store owner-key]
  (-> store (update :effect-state dissoc owner-key) (update :wave-effects dissoc owner-key)))
