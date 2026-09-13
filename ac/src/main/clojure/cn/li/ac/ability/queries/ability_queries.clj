(ns cn.li.ac.ability.queries.ability-queries
  "Pure data queries for skill and ability status.
  
  These read-only functions extract data from maps and registries
  without side effects or mutations."
  (:require [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as sq]
            [cn.li.ac.ability.registry.category :as category-registry]))

;; ============================================================================
;; Skill Queries
;; ============================================================================

(defn list-all-skills
  "Get all effective skill specs as a vector."
  []
  (sq/list-skills))

(defn get-skills-for-category
  "Get all skills for a category.
  
  Args:
    cat-id – keyword"
  [cat-id]
  (sq/get-skills-for-category cat-id))

(defn get-controllable-skills
  "Get all controllable skills for a category.
  
  Args:
    cat-id – keyword"
  [cat-id]
  (sq/get-controllable-skills-for-category cat-id))

(defn get-controllable-skills-at-level
  "Get controllable skills at a specific level.
  
  Args:
    cat-id – keyword
    level  – int"
  [cat-id level]
  (sq/get-controllable-skills-at-level cat-id level))

(defn get-skill-spec
  "Get full skill specification.
  
  Returns: skill-spec map or nil."
  [skill-id]
  (skill-registry/get-skill skill-id))

(defn is-skill-controllable?
  "Check if skill can be directly controlled by player."
  [skill-id]
  (sq/can-control? skill-id))

(defn get-skill-level
  "Get level requirement for a skill."
  [skill-id]
  (when-let [s (get-skill-spec skill-id)]
    (:level s)))

(defn get-skill-developer-type
  "Get minimum developer-type for a skill."
  [skill-id]
  (when-let [s (get-skill-spec skill-id)]
    (:developer-type s)))

(defn get-skill-prerequisites
  "Get list of prerequisite requirements.
  
  Returns: [{:skill-id :min-exp} ...]"
  [skill-id]
  (when-let [s (get-skill-spec skill-id)]
    (:prerequisites s [])))

(defn get-skill-full-identifier
  "Get full skill identifier (category/skill-id)."
  [skill-id]
  (sq/get-skill-full-id skill-id))

;; ============================================================================
;; Ability Data Queries
;; ============================================================================

(defn get-ability-level
  "Get current ability level."
  [ability-data]
  (:level ability-data 1))

(defn get-learned-skills
  "Get set of learned skill ids."
  [ability-data]
  (:learned-skills ability-data #{}))

(defn is-skill-learned?
  "Check if a skill has been learned."
  [ability-data skill-id]
  (contains? (get-learned-skills ability-data) skill-id))

(defn get-skill-exp
  "Get current exp for a skill."
  [ability-data skill-id]
  (adata/get-skill-exp ability-data skill-id))

(defn get-skill-exp-percent
  "Get skill exp as a percentage (0-100 for normal, >100 for overmastery)."
  [ability-data skill-id]
  (* 100.0 (get-skill-exp ability-data skill-id)))

(defn get-level-progress
  "Get accumulated progress toward next level."
  [ability-data]
  (:level-progress ability-data 0.0))

(defn get-level-progress-percent
  "Get level progress as a percentage."
  [ability-data threshold]
  (if (zero? threshold)
    0.0
    (* 100.0 (/ (get-level-progress ability-data) threshold))))

(defn get-category
  "Get category-id for this ability."
  [ability-data]
  (:category-id ability-data))

;; ============================================================================
;; Skill Availability
;; ============================================================================

(defn count-learned-skills
  "Count total learned skills."
  [ability-data]
  (count (get-learned-skills ability-data)))

(defn filter-learned-skills
  "Get learned skills that match a predicate.
  
  Args:
    ability-data – AbilityData map
    pred – function taking skill-id, returns bool"
  [ability-data pred]
  (->> (get-learned-skills ability-data)
       (filter pred)
       vec))

(defn list-mastered-skills
  "Get learned skills with exp >= 1.0.
  
  Returns: [skill-id ...]"
  [ability-data]
  (filter-learned-skills ability-data
                         (fn [sid]
                           (>= (get-skill-exp ability-data sid) 1.0))))

(defn count-mastered-skills
  "Count learned skills with exp >= 1.0."
  [ability-data]
  (count (list-mastered-skills ability-data)))

;; ============================================================================
;; Requirement Checking
;; ============================================================================

(defn meets-level-requirement?
  "Check if player-level meets skill's minimum level."
  [skill-id player-level]
  (if-let [required-level (get-skill-level skill-id)]
    (>= player-level required-level)
    false))

(defn meets-developer-type-requirement?
  "Check if developer-type satisfies skill's requirement.
  
  Args:
    skill-id – keyword
    developer-type – keyword :portable | :normal | :advanced"
  [skill-id developer-type]
  (if-let [required-type (get-skill-developer-type skill-id)]
    (>= (name developer-type) (name required-type))
    false))

(defn meets-prerequisites?
  "Check if all prerequisite skills have sufficient exp.
  
  Returns: bool.
  
  Args:
    ability-data – AbilityData map
    skill-id – keyword"
  [ability-data skill-id]
  (if-let [prereqs (get-skill-prerequisites skill-id)]
    (every? (fn [{:keys [skill-id min-exp]}]
              (>= (get-skill-exp ability-data skill-id) (double min-exp)))
            prereqs)
    true))

;; ============================================================================
;; Category Queries
;; ============================================================================

(defn get-category-name
  "Get display name for a category."
  [cat-id]
  (when-let [cat (category-registry/get-category cat-id)]
    (:name cat)))

(defn get-category-max-level
  "Get maximum level for a category (global cap or category-specific)."
  [cat-id]
  (if-let [cat (category-registry/get-category cat-id)]
    (:max-level cat 999)
    999))

(defn get-category-progression-rate
  "Get progression rate multiplier for a category."
  [cat-id]
  (if-let [cat (category-registry/get-category cat-id)]
    (:prog-incr-rate cat 1.0)
    1.0))

;; ============================================================================
;; Stats Summary
;; ============================================================================

(defn get-ability-summary
  "Get a quick summary of ability status.
  
  Returns: {:level :category-id :learned-count :mastered-count :progress}."
  [ability-data]
  {:level (get-ability-level ability-data)
   :category-id (get-category ability-data)
   :learned-count (count-learned-skills ability-data)
   :mastered-count (count-mastered-skills ability-data)
   :progress (get-level-progress ability-data)})
