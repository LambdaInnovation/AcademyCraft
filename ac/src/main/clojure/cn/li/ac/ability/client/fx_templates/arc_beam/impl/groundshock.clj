(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.groundshock
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
            [clojure.string :as str]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private sound-id (modid/namespaced-path "vecmanip.groundshock"))


;; ---------------------------------------------------------------------------
;; Level effect: particle burst on perform
;; ---------------------------------------------------------------------------



(defn- level-enqueue!
  [store ctx-id channel owner-key payload]
  (let [{:keys [mode affected-blocks]} payload]
    (case mode
      :perform
      (do
        (client-sounds/queue-current-sound-effect!
          {:type :sound :sound-id sound-id :volume 2.0 :pitch 1.0})
        (doseq [{:keys [x y z block-id]} affected-blocks]
          (client-particles/queue-current-particle-effect!
            {:type :particle :particle-type :block-crack
             :block-id (or block-id "minecraft:stone")
             :x (+ (double x) 0.5) :y (+ (double y) 1.0) :z (+ (double z) 0.5)
             :count (+ 4 (rand-int 4)) :speed 0.2
             :offset-x 1.0 :offset-y 0.6 :offset-z 1.0})
          ;; Upstream spawns a SmokeEffect entity per affected block (0.5
          ;; prob): a ~0.6-block-wide dark gray smoke blob at the block top
          ;; that drifts upward (motionY 0.03-0.06). The earlier port spawned
          ;; ONE vanilla smoke particle with a 0.04 offset — a single wisp
          ;; that was invisible in practice. Spawn a burst of vanilla smoke
          ;; particles spread over the same 0.6-block volume with a modest
          ;; upward velocity instead.
          (when (< (rand) 0.5)
            (client-particles/queue-current-particle-effect!
              {:type :particle :particle-type :smoke
               :x (+ (double x) 0.5 (- (* (rand) 0.6) 0.3))
               :y (+ (double y) 1.0 (* (rand) 0.2))
               :z (+ (double z) 0.5 (- (* (rand) 0.6) 0.3))
               :count 8 :speed 0.06
               :offset-x 0.3 :offset-y 0.4 :offset-z 0.3})))
        (or store {:hand-state {}}))
      (or store {:hand-state {}}))))

(defn- level-tick!
  [store]
  (or store {:hand-state {}}))

;; ---------------------------------------------------------------------------
;; Hand effect: first-person camera shake + punch animation
;; ---------------------------------------------------------------------------



(def ^:private perform-step 3.4)
(def ^:private perform-ticks-count 4)







(defn- hand-enqueue-state! [state ctx-id channel owner-key payload]
  (let [state* (or state {:hand-state {}})
        {:keys [mode charge-ticks performed? source-player-id world-id]} payload
        owner-key* (or owner-key [:ctx ctx-id])
        base-meta {:owner-key owner-key*
                   :queue-owner (client-sounds/current-effect-owner)
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (update state* :hand-state assoc owner-key*
              (merge base-meta {:charge-ticks 0 :perform-ticks 0 :active? true}))

      :update
      (update state* :hand-state update owner-key*
              (fn [owner-state]
            (let [prev (long (or (:charge-ticks owner-state) 0))
                next-val (long (max 0 (or charge-ticks 0))) ]
              (doseq [tick (range (inc prev) (inc next-val))]
              (let [pitch-factor (cond
                         (< tick 4) (/ tick 4.0)
                         (<= tick 20) 1.0
                         (<= tick 25) (- 1.0 (/ (- tick 20) 5.0))
                         :else 0.0)]
                (hand-effects/add-camera-pitch-delta! (:queue-owner base-meta) (* -0.2 pitch-factor))))
              (merge base-meta
                 {:charge-ticks next-val
                  :perform-ticks (long (or (:perform-ticks owner-state) 0))
                  :active? true}))))

      :perform
      (update state* :hand-state update owner-key*
              (fn [owner-state]
            (merge base-meta
                 {:charge-ticks (long (or (:charge-ticks owner-state) 0))
                :perform-ticks perform-ticks-count
                :active? false})))

      :end
      (when-not performed?
        (update state* :hand-state dissoc owner-key*))

      state*)))

(defn- hand-tick-state! [state]
  (let [state* (or state {:hand-state {}})]
    (update state* :hand-state
            (fn [states]
              (into {}
                    (keep (fn [[owner-key owner-state]]
                            (let [remaining (long (or (:perform-ticks owner-state) 0))]
                      (when (pos? remaining)
                        (hand-effects/add-camera-pitch-delta! (:queue-owner owner-state) perform-step))
                      (let [next-state (if (> remaining 1)
                                         (assoc owner-state :perform-ticks (dec remaining))
                                         (when (or (:active? owner-state) (pos? remaining))
                                           (assoc owner-state :perform-ticks 0)))]
                        (when next-state
                          [owner-key next-state])))))
                    states)))))

;; ---------------------------------------------------------------------------
;; FX channel registration
;; ---------------------------------------------------------------------------

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:groundshock :level] [_ _] {})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:groundshock :hand] [_ _] {:hand-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:groundshock :level]
  [_ _ store ctx-id channel owner-key payload] (level-enqueue! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:groundshock :hand]
  [_ _ store ctx-id channel owner-key payload] (hand-enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:groundshock :level] [_ _ store] (level-tick! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:groundshock :hand] [_ _ store] (hand-tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :groundshock [_ store owner-key] (update store :hand-state dissoc owner-key))
