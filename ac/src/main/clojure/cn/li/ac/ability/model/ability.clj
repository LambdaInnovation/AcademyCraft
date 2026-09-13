(ns cn.li.ac.ability.model.ability
  "Pure-data functions for AbilityData.

  AbilityData is a plain Clojure map holding per-player category/skill state.
  All functions are pure (no side effects, no mutation).
  The platform stores and retrieves these maps; ac only computes on them.

  Map schema:
    {:category-id    keyword | nil
     :learned-skills #{keyword ...}
      :skill-exps     {keyword float}    ; 0.0-1.0
      :level          int                ; 1-5
      :level-progress float}             ; >= 0.0"
  (:require [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Constructors
;; ============================================================================

(defn new-ability-data
  "Create a fresh AbilityData map for a new player."
  []
  {:category-id    nil
   :learned-skills #{}
   :skill-exps     {}
   :level          1
   :level-progress 0.0})

;; ============================================================================
;; Category
;; ============================================================================

(defn get-category [d] (:category-id d))

(defn set-category
  "Returns updated AbilityData with category set.
  Clears all skill exps when category changes (behaviour parity with original)."
  [d cat-id]
  (if (= (:category-id d) cat-id)
    d
    (assoc d
           :category-id cat-id
           :learned-skills #{}
           :skill-exps {}
           :level 1
           :level-progress 0.0)))

;; ============================================================================
;; Skill Learning
;; ============================================================================

(defn is-learned? [d skill-id]
  (contains? (:learned-skills d) skill-id))

(defn learn-skill [d skill-id]
  (update d :learned-skills conj skill-id))

;; ============================================================================
;; Skill Experience
;; ============================================================================

(defn get-skill-exp [d skill-id]
  (get (:skill-exps d) skill-id 0.0))

(defn set-skill-exp
  "Clamp exp to [0.0, 1.0]."
  [d skill-id v]
  (assoc-in d [:skill-exps skill-id] (min 1.0 (max 0.0 (double v)))))

(defn add-skill-exp
  "Add amount to skill exp, clamped at 1.0. Returns updated data and delta actually added."
  [d skill-id amount]
  (let [cur  (get-skill-exp d skill-id)
        cap  (- 1.0 cur)
        real (min cap (max 0.0 (double amount)))]
    {:data  (set-skill-exp d skill-id (+ cur real))
     :delta real}))

(defn clear-skill-exps [d]
  (assoc d :skill-exps {}))

;; ============================================================================
;; Level & Progress
;; ============================================================================

(defn get-level [d] (:level d))

(defn get-level-progress [d] (:level-progress d))

(defn add-level-progress [d amount]
  (update d :level-progress + (double amount)))

(defn set-level-progress [d amount]
  (assoc d :level-progress (max 0.0 (double amount))))

(defn set-level [d level]
  (when-not (and (>= level 1) (<= level 5))
    (throw (IllegalArgumentException. "set-level: level must be 1-5")))
  (assoc d :level level :level-progress 0.0))
