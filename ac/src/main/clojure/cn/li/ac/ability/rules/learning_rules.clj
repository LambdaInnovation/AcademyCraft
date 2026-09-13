(ns cn.li.ac.ability.rules.learning-rules
  "Pure business logic for skill learning, exp gain, and level progression.
  
  No atoms, no event firing, no side effects. Reducer layer combines these
  with event generation."
  (:require [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.ability.util.level-formula :as level-formula]
            [cn.li.ac.ability.registry.skill :as skill]))

;; ============================================================================
;; Learning Condition Checks (Pure Predicates)
;; ============================================================================

(defn check-level-condition
  "Returns true if player-level ≥ min-level."
  [player-level min-level]
  (>= player-level min-level))

(defn check-developer-type-condition
  "Returns true if provided developer-type satisfies minimum requirement."
  [developer-type required-type]
  (developer/gte? developer-type required-type))

(defn check-dependency-condition
  "Returns true if prerequisite skill has sufficient exp."
  [ability-data prereq-skill required-exp]
  (>= (adata/get-skill-exp ability-data prereq-skill) (double required-exp)))

(defn check-any-skill-level-condition
  "Returns true if player has learned any skill at target-level."
  [ability-data target-level]
  (boolean (some (fn [sid]
                   (let [s (skill/get-skill sid)]
                     (= target-level (:level s))))
                 (:learned-skills ability-data))))

;; ============================================================================
;; Learning Eligibility
;; ============================================================================

(defn check-all-conditions
  "Comprehensive learning eligibility check.
  
  Returns: {:pass? bool :failures [failure-maps]}."
  [skill-spec ability-data player-level developer-type]
  (let [failures (transient [])]
    (when-not (check-level-condition player-level (:level skill-spec))
      (conj! failures
             {:type :level :required (:level skill-spec) :actual player-level}))
    
    (when-not (check-developer-type-condition developer-type (:developer-type skill-spec))
      (conj! failures
             {:type :developer-type
              :required (:developer-type skill-spec)
              :actual developer-type}))
    
    (doseq [{:keys [skill-id min-exp]} (:prerequisites skill-spec)]
      (when-not (check-dependency-condition ability-data skill-id min-exp)
        (conj! failures
               {:type :prerequisite
                :skill-id skill-id
                :required min-exp
                :actual (adata/get-skill-exp ability-data skill-id)})))
    
    (doseq [condition (:conditions skill-spec)]
      (when (= (:type condition) :any-skill-level)
        (when-not (check-any-skill-level-condition ability-data (:level condition))
          (conj! failures
                 {:type :any-skill-level :required-level (:level condition)}))))
    
    (let [f (persistent! failures)]
      {:pass? (empty? f) :failures f})))

(defn conditions-with-status
  "Displayable learn conditions for a skill, each with an :accepted flag. Mirrors
   upstream: the level condition has shouldDisplay=false so it is omitted; only
   developer-type / prerequisite / any-skill-level conditions are shown."
  [skill-spec ability-data player-level developer-type]
  (vec
   (concat
    (when (:developer-type skill-spec)
      [{:type :developer-type :required (:developer-type skill-spec)
        :accepted (check-developer-type-condition developer-type (:developer-type skill-spec))}])
    (for [{:keys [skill-id min-exp]} (:prerequisites skill-spec)]
      {:type :prerequisite :skill-id skill-id :required min-exp
       :accepted (check-dependency-condition ability-data skill-id min-exp)})
    (for [c (:conditions skill-spec) :when (= (:type c) :any-skill-level)]
      {:type :any-skill-level :required-level (:level c)
       :accepted (check-any-skill-level-condition ability-data (:level c))}))))

(defn can-learn?
  "Convenience predicate—returns boolean. Returns false for nil skill-spec."
  [skill-spec ability-data player-level developer-type]
  (boolean (when skill-spec
    (:pass? (check-all-conditions skill-spec ability-data player-level developer-type))))
)
;; ============================================================================
;; Experience Gain
;; ============================================================================

(defn add-skill-exp
  "Add experience to a skill and accumulate level progress.

  Upstream AbilityData.addSkillExp semantics:
  - skill exp gains scaled-exp (amount × skill exp-incr-speed), clamped at 1.0
  - level progress always gains the FULL scaled-exp (× global rate) — even
    when the skill is already mastered, using it still advances the level-up
    progress (upstream addLevelProgress(amt) runs unconditionally)
  - the global prog-incr-rate scales level progress only, never the skill exp
    (upstream mul1 enters addLevelProgress alone)

  Returns: {:data updated-ability-data
            :exp-delta float (clamped amount actually added to skill exp)
            :exp-scaled float (pre-clamp input, what events/level-progress use)
            :events-needed []}."
  [ability-data skill-id raw-amount exp-incr-rate global-rate]
  (let [scaled-exp (* (double raw-amount)
                      (double (or exp-incr-rate 1.0)))
        {:keys [data delta]} (adata/add-skill-exp ability-data skill-id scaled-exp)
        data2 (adata/add-level-progress data (* scaled-exp (double global-rate)))
        events-needed (if (pos? scaled-exp) [:skill-exp-added :skill-exp-changed] [])]
    {:data data2 :exp-delta delta :exp-scaled scaled-exp :events-needed events-needed}))

;; ============================================================================
;; Level-Up Progression
;; ============================================================================

(defn level-up-threshold
  "Calculate exp points needed to advance to next level."
  [ability-data controllable-skills cat-prog-rate global-prog-rate]
  (let [skill-count (count controllable-skills)
        all-mastered? (and (pos? skill-count)
                           (every? #(>= (adata/get-skill-exp ability-data (:id %))
                                        1.0)
                                   controllable-skills))]
    (level-formula/level-up-threshold skill-count all-mastered? cat-prog-rate global-prog-rate)))

(defn can-level-up?
  "Check if level-up conditions are met."
  [ability-data controllable-skills cat-prog-rate global-prog-rate max-level]
  (let [current-level (:level ability-data)
        progress (:level-progress ability-data 0.0)
        threshold (level-up-threshold ability-data controllable-skills cat-prog-rate global-prog-rate)]
    (and (< current-level max-level)
         (>= progress threshold))))

(defn perform-level-up
  "Execute a level-up.
  
  Returns: {:data updated-ability-data :old-level int :new-level int :events-needed []}."
  [ability-data]
  (let [old-level (:level ability-data)
        new-level (inc old-level)
        data2 (adata/set-level ability-data new-level)]
    {:data data2
     :old-level old-level
     :new-level new-level
     :events-needed [:level-up-complete]}))

;; ============================================================================
;; Queries (Pure)
;; ============================================================================

;; ============================================================================
;; Skill Tree Visibility Filter
;; ============================================================================

(defn can-be-potentially-learned?
  "Returns true if skill should be shown in the skill tree UI.
   Matching original LearningHelper.canBePotentiallyLearned (SkillTree.scala:257-260).

   A skill is visible when:
   - Player level >= skill level, OR
   - Skill is already learned, OR
   - Parent is learned (or skill has no parent)"
  [ability-data skill-spec]
  (let [parent-id (some-> (:prerequisites skill-spec) first :skill-id)]
    (or (>= (:level ability-data) (:level skill-spec))
        (adata/is-learned? ability-data (:id skill-spec))
        (nil? parent-id)
        (adata/is-learned? ability-data parent-id))))

(defn get-current-level
  "Get current ability level."
  [ability-data]
  (:level ability-data))

(defn get-level-progress
  "Get accumulated progress toward next level."
  [ability-data]
  (:level-progress ability-data 0.0))

(defn get-learned-skills
  "Get set of learned skill ids."
  [ability-data]
  (:learned-skills ability-data #{}))

(defn is-skill-learned?
  "Check if skill is in learned-skills set."
  [ability-data skill-id]
  (contains? (get-learned-skills ability-data) skill-id))

(defn get-skill-exp
  "Get current exp for a skill."
  [ability-data skill-id]
  (adata/get-skill-exp ability-data skill-id))

(defn get-skill-mastery
  "Get skill exp as a mastery progress (0.0 to 1.0+ for overmastery)."
  [ability-data skill-id]
  (let [exp (adata/get-skill-exp ability-data skill-id)]
    (min 1.0 (max 0.0 exp))))
