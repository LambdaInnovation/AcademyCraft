(ns cn.li.ac.content.ability.teleporter.passive-hooks
  "Passive crit pipeline for teleporter dim-folding-theorem and space-fluct.

  Crit probability and damage multipliers are consulted from deal-magic-damage!
  in tp-skill-helper. Passive skill specs live in dim-folding-theorem and
  space-fluct namespaces; this ns owns the formulas and init registration.

  No Minecraft imports."
  (:require [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.achievement.dispatcher :as ach-dispatcher]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log]))

(def ^:private teleporter-critical-hit-message-key "ability.teleporter.critical_hit")

(defn- send-chat-message!
  [player-uuid message args translate?]
  (if-let [fw-atom (fw/fw-atom)]
    (boolean
     (platform/call-adapter fw-atom :player-feedback :send-player-feedback!
                            player-uuid
                            {:mode :chat
                             :message message
                             :args (vec (or args []))
                             :translate? (boolean translate?)}))
    (do
      (log/debug "Player feedback unavailable; dropping chat message" {:player-uuid player-uuid
                                                                       :message message})
      false)))

(defn- crit-rate-label
  [crit-rate]
  (format "x%.1f" (double crit-rate)))

(defn- learned?
  [player-id skill-id]
  (boolean
    (when-let [state (skill-effects/get-player-state player-id)]
      (adata/is-learned? (:ability-data state) skill-id))))

(defn- passive-cfg-lerp
  [skill-id field-id exp]
  (if (neg? exp)
    0.0
    (skill-config/lerp-double skill-id field-id exp)))

(defn- passive-exp
  [player-id skill-id]
  (if (learned? player-id skill-id)
    (skill-effects/skill-exp player-id skill-id)
    -1.0))

(defn crit-probability
  "Crit roll probability for teleporter passive level 0/1/2."
  [player-id level]
  (let [dim-exp (passive-exp player-id :dim-folding-theorem)
        space-exp (passive-exp player-id :space-fluct)]
    (cond
      (= level 0) (+ (passive-cfg-lerp :dim-folding-theorem :critical.level0-probability dim-exp)
                     (passive-cfg-lerp :space-fluct :critical.level0-probability space-exp))
      (= level 1) (passive-cfg-lerp :space-fluct :critical.level1-probability space-exp)
      (= level 2) (passive-cfg-lerp :space-fluct :critical.level2-probability space-exp)
      :else 0.0)))

(defn- roll-crit-level
  [player-id]
  (some (fn [lvl]
          (when (< (rand) (crit-probability player-id lvl))
            lvl))
        [0 1 2]))

(defn calc-teleporter-crit
  "Roll teleporter passive crit and return damage transform metadata."
  [player-id base-damage]
  (if-let [crit-level (roll-crit-level player-id)]
    (let [crit-rates (skill-config/tunable-double-list :dim-folding-theorem :critical.damage-multipliers)
          crit-rate (double (nth crit-rates crit-level 1.0))
          final-damage (* (double base-damage) crit-rate)
          events (cond-> ["teleporter.critical_attack"]
                    (= crit-level 2) (conj "teleporter.mastery"))]
      {:damage-before (double base-damage)
       :damage-after (double final-damage)
       :crit-level crit-level
       :crit-rate crit-rate
       :critical? true
       :message-key teleporter-critical-hit-message-key
       :message-args [(crit-rate-label crit-rate)]
       :events events})
    {:damage-before (double base-damage)
     :damage-after (double base-damage)
     :crit-level nil
     :crit-rate 1.0
     :critical? false
     :message-key nil
     :message-args []
     :events []}))

(defn apply-crit-side-effects!
  [player-id crit-level events {:keys [message-key message-args]}]
  (skill-effects/add-skill-exp! player-id :dim-folding-theorem
                                (* (skill-config/tunable-double :dim-folding-theorem :progression.exp-per-crit-level)
                                   (inc crit-level)))
  (skill-effects/add-skill-exp! player-id :space-fluct
                                (skill-config/tunable-double :space-fluct :progression.exp-critical))
  (when message-key
    (send-chat-message! player-id message-key message-args true))
  (doseq [event-id events]
    (ach-dispatcher/trigger-custom-event! player-id event-id)))

(defn register-passive-hooks!
  "Register teleporter passive wiring during ability content init.

  Crit is applied through tp-skill-helper/deal-magic-damage! using formulas
  in this namespace; no calc-event handlers are required for these passives."
  []
  nil)
