(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.directed-shock
  (:require [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.effects.beam-ops :as fx-beam]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
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

(def ^:private prepare-duration-ms 150.0)
(def ^:private punch-duration-ms 300.0)










(defn- now-ms [] (System/currentTimeMillis))

;; ---------------------------------------------------------------------------
;; Transform curves (timestamp-based animation)
;; ---------------------------------------------------------------------------

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

;; ---------------------------------------------------------------------------
;; Enqueue
;; ---------------------------------------------------------------------------

(defn- enqueue-state! [state ctx-id channel owner-key payload]
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

      ;; The punch sound itself is queued by directed-shock-fx.clj's
      ;; :immediate channel handler instead of here — that one is
      ;; world-positioned at the caster's coordinates (matching original's
      ;; unconditional, non-isLocal-gated ACSounds.playClient), so it plays
      ;; correctly for owner and nearby alike. Queuing it here too would
      ;; double it for the owner and mislocate it for bystanders (this
      ;; hand-effect enqueue always resolves to the LOCAL viewer's own
      ;; position, not the caster's).
      ;; :punch is the owner-only channel mode; :perform retained for direct
      ;; runtime enqueues (tests) — both start the punch stage.
      (:punch :perform)
      (update state* :effect-state assoc owner-key*
              (merge base-meta {:stage :punch :started-at (now-ms)}))

      :end
      (if performed?
        state*
        (update state* :effect-state dissoc owner-key*))

      state*)))

;; ---------------------------------------------------------------------------
;; Tick
;; ---------------------------------------------------------------------------

(defn- tick-state! [state]
  (let [state* (or state {:effect-state {}})]
    (update state* :effect-state
            (fn [states]
              (into {}
                    (remove (fn [[_ {:keys [stage started-at]}]]
                              (and (= stage :punch)
                                   (>= (- (now-ms) (long started-at)) punch-duration-ms))))
                    states)))))

;; ---------------------------------------------------------------------------
;; Transform
;; ---------------------------------------------------------------------------

(defn- transform []
  (when-let [[owner-key {:keys [stage started-at]}]
             (some (fn [[owner-key st]]
                     (when (:stage st)
                       [owner-key st]))
                   (:effect-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :directed-shock)))]
    (let [elapsed (- (now-ms) (long started-at))]
      (case stage
        :prepare
        (prepare-transform (min 1.0 (/ elapsed prepare-duration-ms)))
        :punch
        (let [progress (/ elapsed punch-duration-ms)]
          (if (>= progress 1.0)
            (do
              (cn.li.ac.ability.client.fx-templates.arc-beam/clear-owner! :directed-shock owner-key)
              nil)
            (punch-transform progress)))
        nil))))

;; ---------------------------------------------------------------------------
;; Registration
;; ---------------------------------------------------------------------------

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:directed-shock :hand] [_ _] {:effect-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:directed-shock :hand]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:directed-shock :hand] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-transform-fn :directed-shock [_effect-id] (transform))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :directed-shock [_ store owner-key]
  (update store :effect-state dissoc owner-key))
