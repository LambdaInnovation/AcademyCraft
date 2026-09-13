(ns cn.li.ac.ability.queries.player-queries
  "Pure data queries for player ability state.
  
  These read-only functions extract data from player state maps
  without side effects or mutations."
  (:require [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.model.cooldown :as cdata]
            [cn.li.ac.ability.model.ability :as adata]))

;; ============================================================================
;; Resource Queries
;; ============================================================================

(defn get-current-cp
  "Get current CP value."
  [resource-data]
  (:cur-cp resource-data 0.0))

(defn get-max-cp
  "Get maximum CP value."
  [resource-data]
  (:max-cp resource-data 0.0))

(defn get-cp-percent
  "Get CP as percentage of max (0-100)."
  [resource-data]
  (if (zero? (:max-cp resource-data))
    100.0
    (* 100.0 (/ (:cur-cp resource-data) (:max-cp resource-data)))))

(defn get-current-overload
  "Get current overload value."
  [resource-data]
  (:cur-overload resource-data 0.0))

(defn get-max-overload
  "Get maximum overload value."
  [resource-data]
  (:max-overload resource-data 0.0))

(defn get-overload-percent
  "Get overload as percentage of max (0-100)."
  [resource-data]
  (if (zero? (:max-overload resource-data))
    0.0
    (* 100.0 (/ (:cur-overload resource-data) (:max-overload resource-data)))))

(defn is-resource-activated?
  "Check if ability resource is activated."
  [resource-data]
  (rdata/is-activated? resource-data))

(defn is-in-overload-recovery?
  "Check if currently in overload recovery (cannot use ability)."
  [resource-data]
  (not (get resource-data :overload-fine true)))

(defn has-resource-interference?
  "Check if any interference blocks resource usage."
  [resource-data]
  (not-empty (:interferences resource-data)))

(defn get-resource-interferences
  "Get set of active interference source ids."
  [resource-data]
  (:interferences resource-data #{}))

(defn get-resource-summary
  "Get quick summary of resource status.
  
  Returns: {:cur-cp :max-cp :cp-pct :cur-ol :max-ol :ol-pct :activated? :in-recovery?}."
  [resource-data]
  {:cur-cp (get-current-cp resource-data)
   :max-cp (get-max-cp resource-data)
   :cp-pct (get-cp-percent resource-data)
   :cur-ol (get-current-overload resource-data)
   :max-ol (get-max-overload resource-data)
   :ol-pct (get-overload-percent resource-data)
   :activated? (is-resource-activated? resource-data)
   :in-recovery? (is-in-overload-recovery? resource-data)})

;; ============================================================================
;; Cooldown Queries
;; ============================================================================

(defn get-cooldown
  "Get remaining ticks for a cooldown.
  
  Args:
    cooldown-data – CooldownData map
    ctrl-id       – keyword
    sub-id        – keyword (default :main)"
  ([cooldown-data ctrl-id]
   (get-cooldown cooldown-data ctrl-id :main))
  ([cooldown-data ctrl-id sub-id]
   (cdata/get-remaining cooldown-data ctrl-id sub-id)))

(defn is-on-cooldown?
  "Check if a skill is on cooldown.
  
  Args:
    cooldown-data – CooldownData map
    ctrl-id       – keyword
    sub-id        – keyword (default :main)"
  ([cooldown-data ctrl-id]
   (is-on-cooldown? cooldown-data ctrl-id :main))
  ([cooldown-data ctrl-id sub-id]
   (cdata/in-cooldown? cooldown-data ctrl-id sub-id)))

(defn get-all-cooldowns
  "Get list of all active cooldowns.
  
  Returns: [{:ctrl-id :sub-id :ticks :max} ...]"
  [cooldown-data]
  (mapv (fn [[[ctrl-id sub-id] {:keys [ticks max]}]]
          {:ctrl-id ctrl-id :sub-id sub-id :ticks ticks :max max})
        cooldown-data))

(defn count-active-cooldowns
  "Count total active cooldowns."
  [cooldown-data]
  (count cooldown-data))

(defn has-any-cooldown?
  "Check if any cooldown is active."
  [cooldown-data]
  (not-empty cooldown-data))

(defn get-cooldowns-for-skill
  "Get all sub-cooldowns for a specific controller.
  
  Returns: [{:sub-id :ticks :max} ...]"
  [cooldown-data ctrl-id]
  (->> cooldown-data
       (filter (fn [[[c _] _]] (= c ctrl-id)))
       (mapv (fn [[[_ s] {:keys [ticks max]}]]
               {:sub-id s :ticks ticks :max max}))
       vec))

;; ============================================================================
;; Ability State Queries
;; ============================================================================

(defn get-ability-level
  "Get ability level."
  [ability-data]
  (:level ability-data 1))

(defn get-ability-category
  "Get ability category-id."
  [ability-data]
  (:category-id ability-data))

(defn get-learned-skills-count
  "Get count of learned skills."
  [ability-data]
  (count (:learned-skills ability-data #{})))

(defn is-skill-learned?
  "Check if a skill is in learned set."
  [ability-data skill-id]
  (contains? (:learned-skills ability-data #{}) skill-id))

(defn get-skill-experience
  "Get exp value for a skill."
  [ability-data skill-id]
  (adata/get-skill-exp ability-data skill-id))

(defn get-level-progress
  "Get progress toward next level."
  [ability-data]
  (:level-progress ability-data 0.0))

(defn get-ability-summary
  "Get quick summary of ability state.
  
  Returns: {:level :category-id :learned-count :progress}."
  [ability-data]
  {:level (get-ability-level ability-data)
   :category-id (get-ability-category ability-data)
   :learned-count (get-learned-skills-count ability-data)
   :progress (get-level-progress ability-data)})

;; ============================================================================
;; Composite Queries (Multiple Data Sources)
;; ============================================================================

(defn can-use-skill?
  "Check if player can currently use a skill (combined guard).
  
  Args:
    resource-data  – ResourceData map
    cooldown-data  – CooldownData map
    ability-data   – AbilityData map
    ctrl-id        – keyword (controller id of skill)
    skill-id       – keyword (skill id, for learned check)"
  [resource-data cooldown-data ability-data ctrl-id skill-id]
  (and (not (is-in-overload-recovery? resource-data))
       (is-resource-activated? resource-data)
       (not (is-on-cooldown? cooldown-data ctrl-id :main))
       (is-skill-learned? ability-data skill-id)))

(defn get-player-state-summary
  "Get comprehensive summary of player ability state.
  
  Returns: {:resource :cooldown :ability}."
  [resource-data cooldown-data ability-data]
  {:resource (get-resource-summary resource-data)
   :cooldown {:total-active (count-active-cooldowns cooldown-data)
              :cooldowns (get-all-cooldowns cooldown-data)}
   :ability (get-ability-summary ability-data)})

;; ============================================================================
;; Persistence Queries
;; ============================================================================

(defn get-resource-data-for-save
  "Extract resource data for persistence (serialization-safe).
  
  Returns: map suitable for NBT or JSON storage."
  [resource-data]
  {:cur-cp (:cur-cp resource-data)
   :max-cp (:max-cp resource-data)
   :cur-overload (:cur-overload resource-data)
   :max-overload (:max-overload resource-data)
   :add-max-cp (:add-max-cp resource-data)
   :add-max-overload (:add-max-overload resource-data)
   :activated (:activated resource-data)
   :until-recover (:until-recover resource-data)
   :until-overload-recover (:until-overload-recover resource-data)})

(defn get-cooldown-data-for-save
  "Extract cooldown data for persistence.
  
  Returns: vector of [ctrl-id sub-id ticks] suitable for NBT."
  [cooldown-data]
  (mapv (fn [[[ctrl-id sub-id] ticks]]
          [ctrl-id sub-id ticks])
        cooldown-data))

(defn get-ability-data-for-save
  "Extract ability data for persistence.
  
  Returns: map suitable for NBT or JSON storage."
  [ability-data]
  {:level (:level ability-data)
   :category-id (:category-id ability-data)
   :learned-skills (vec (:learned-skills ability-data))
   :level-progress (:level-progress ability-data 0.0)})
