(ns cn.li.ac.content.ability.vecmanip.vec-reflection-fx
  "Client FX for VecReflection: reflection wave billboards (upstream
  WaveEffect) at the affected entities."
  (:require
            [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.wave-effect :as wave]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(def ^:private vec-reflection-effect-id :vec-reflection)
(def ^:private sound-id (modid/namespaced-path "vecmanip.vec_reflection"))

(defn default-vec-reflection-fx-runtime-state
  []
  {:effect-state {}
   :wave-effects {}})

(defn vec-reflection-fx-snapshot
  []
  (or (level-effects/effect-state-snapshot vec-reflection-effect-id)
      (default-vec-reflection-fx-runtime-state)))

(defn reset-vec-reflection-fx-for-test!
  []
  (level-effects/reset-level-effect-state-for-test!
    vec-reflection-effect-id
    (default-vec-reflection-fx-runtime-state))
  nil)

(defn clear-vec-reflection-owner!
  [owner-key]
  (level-effects/update-effect-state!
    vec-reflection-effect-id
    (fn [state]
      (-> (or state (default-vec-reflection-fx-runtime-state))
          (update :effect-state dissoc owner-key)
          (update :wave-effects dissoc owner-key))))
  nil)

(defn- queue-reflection-sound!
  [x y z]
  (client-sounds/queue-current-sound-effect!
    {:type :sound :sound-id sound-id :volume 0.5 :pitch 1.0
     :x (double (or x 0.0))
     :y (double (or y 0.0))
     :z (double (or z 0.0))}))

;; VecReflection spawns WaveEffect(world, rings = 2, size = 1.1).
(def ^:private wave-rings 2)
(def ^:private wave-size 1.1)

(defn- enqueue-wave
  [store owner-key base-meta x y z yaw-rad pitch-rad]
  (update-in store [:wave-effects owner-key]
    (fnil conj [])
    (merge base-meta
           {:x (double (or x 0.0))
            :y (double (or y 0.0))
            :z (double (or z 0.0))
            ;; eff.rotationYaw = player.rotationYawHead, rotationPitch =
            ;; player.rotationPitch -- frozen at spawn, and the CASTER's, so it
            ;; travels with the message rather than being read off whoever
            ;; happens to be watching.
            :yaw-rad (double (or yaw-rad 0.0))
            :pitch-rad (double (or pitch-rad 0.0))
            :rings (wave/build-rings wave-rings wave-size)
            :ttl wave/life-ticks
            :max-ttl wave/life-ticks})))

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store (default-vec-reflection-fx-runtime-state))
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode x y z reflected? entity-uuid vx vy vz yaw-rad pitch-rad
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
      :reflect-entity
      (if reflected?
        (do
          ;; c_reflectEntity turns the projectile locally before doing anything
          ;; visual, so it never carries on straight for the tick or three
          ;; before the server's velocity update lands.
          (when (and entity-uuid vx vy vz)
            (client-bridge/run-client-effect!
              :mcmod/set-client-entity-motion
              {:entity-uuid (str entity-uuid)
               :vx (double vx) :vy (double vy) :vz (double vz)}))
          (queue-reflection-sound! x y z)
          (enqueue-wave store* owner-key* base-meta x y z yaw-rad pitch-rad))
        store*)
      :play
      (do
        (queue-reflection-sound! x y z)
        (enqueue-wave store* owner-key* base-meta x y z yaw-rad pitch-rad))
      store*)))

(defn- tick-state!
  [store]
  (let [state* (or store (default-vec-reflection-fx-runtime-state))]
    (assoc state*
           :effect-state (store-tick/keep-active-inc-ticks (:effect-state state*))
           :wave-effects (store-tick/tick-ttl-items-by-owner (:wave-effects state*)))))

(defn- build-plan
  [_camera-pos _hand-center-pos _tick _query-fn]
  (let [{:keys [wave-effects]} (vec-reflection-fx-snapshot)
        wave-plan (mapcat wave/ops (mapcat val wave-effects))]
    (when (seq wave-plan)
      {:ops (vec wave-plan)})))

(defn init!
  []
  (fx-spec/register!
    {:id vec-reflection-effect-id
     :level {:initial-state (default-vec-reflection-fx-runtime-state)
             :enqueue-state-fn enqueue-state!
             :tick-state-fn tick-state!
             :build-plan-fn build-plan}
     :channels {:start {:topic :vec-reflection/fx-start :mode :start}
                :end {:topic :vec-reflection/fx-end :mode :end}
                :reflect-entity {:topic :vec-reflection/fx-reflect-entity :mode :reflect-entity
                                 :level-payload (fn [_ _ p]
                                                  {:x (double (or (:x p) 0.0))
                                                   :y (double (or (:y p) 0.0))
                                                   :z (double (or (:z p) 0.0))
                                                   :reflected? (boolean (:reflected? p))
                                                   :entity-uuid (:entity-uuid p)
                                                   :vx (:vx p) :vy (:vy p) :vz (:vz p)
                                                   :yaw-rad (:yaw-rad p)
                                                   :pitch-rad (:pitch-rad p)})}
                :play {:topic :vec-reflection/fx-play :mode :play
                       :level-payload (fn [_ _ p]
                                        {:x (double (or (:x p) 0.0))
                                         :y (double (or (:y p) 0.0))
                                         :z (double (or (:z p) 0.0))
                                         :yaw-rad (:yaw-rad p)
                                         :pitch-rad (:pitch-rad p)})}}})
  nil)
