(ns cn.li.ac.ability.server.damage.pipeline
  "Damage and action pipeline for ability skill effects.

  Provides helpers for:
    attack            – standard melee/projectile damage through calc pipeline
    attack-ignore-armor – damage bypass (some electric/supernatural skills)
    attack-range      – AOE range falloff damage
    can-attack-player? – PvP gate (global config toggle)
    can-break-block?  – griefing gate (global config + skill override)

  The attack-fn and attack-ignore-armor-fn vars are injected at startup
  by the Forge adapter so this namespace stays free of net.minecraft.*."
  (:require [cn.li.ac.ability.registry.event   :as evt]
            [cn.li.ac.ability.config  :as cfg]
            [cn.li.ac.ability.registry.skill   :as skill]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log     :as log]))

;; ============================================================================
;; Injected platform fns
;; ============================================================================

(def ^:private attack-fn nil)
(def ^:private attack-ignore-armor-fn nil)
(def ^:private nearby-players-fn nil)

(defn register-platform-fns!
  "Call from forge adapter during mod init."
  [{:keys [attack attack-ignore-armor nearby-players]}]
  (install/install-root! #'attack-fn             attack)
  (install/install-root! #'attack-ignore-armor-fn attack-ignore-armor)
  (install/install-root! #'nearby-players-fn      nearby-players))

;; ============================================================================
;; Attack pipeline
;; ============================================================================

(defn attack
  "Deal `base-damage` from `attacker-uuid` to `target-entity`.
  Runs CALC-SKILL-ATTACK event pipeline so skills can modify damage.
  Returns actual damage dealt."
  [attacker-uuid target-entity skill-id base-damage]
  (when-not attack-fn
    (log/warn "attack: no platform attack-fn registered"))
  (let [final-damage (evt/fire-calc-event!
                       evt/CALC-SKILL-ATTACK
                       base-damage
                       {:player-id attacker-uuid
                        :skill-id  skill-id})]
    (when attack-fn
      (attack-fn attacker-uuid target-entity final-damage))
    final-damage))

(defn attack-ignore-armor
  "Deal `damage` that bypasses armor and resistances."
  [attacker-uuid target-entity damage]
  (when attack-ignore-armor-fn
    (attack-ignore-armor-fn attacker-uuid target-entity damage))
  damage)

(defn attack-range
  "AOE damage: deal damage to all entities within `radius` of `origin`.
  Damage falls off linearly from `base-damage` at radius 0 to 0 at `radius`.
  Excludes `attacker-uuid`.
  Returns seq of [entity actual-damage] pairs."
  [attacker-uuid origin radius base-damage skill-id]
  (when-let [nearby (and nearby-players-fn
                         (nearby-players-fn origin radius))]
    (keep (fn [entity]
            (when (not= (str entity) attacker-uuid)
              (let [dist    (or (:distance entity) radius)
                    scale   (max 0.0 (- 1.0 (/ dist radius)))
                    dmg     (* base-damage scale)
                    actual  (attack attacker-uuid entity skill-id dmg)]
                [entity actual])))
          nearby)))

;; ============================================================================
;; Permission gates
;; ============================================================================

(defn can-attack-player?
  "Returns true if PvP through abilities is enabled."
  []
  (cfg/attack-player-enabled?))

(defn can-break-block?
  "Returns true if the ability system allows block breaking.
  Skill-level override: if skill has :can-break-blocks key, that wins."
  [skill-id]
  (if skill-id
    (let [sk (skill/get-skill skill-id)]
      (get sk :can-break-blocks (cfg/destroy-blocks-enabled?)))
    (cfg/destroy-blocks-enabled?)))
