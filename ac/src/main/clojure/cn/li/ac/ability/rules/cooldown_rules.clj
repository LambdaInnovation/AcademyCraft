(ns cn.li.ac.ability.rules.cooldown-rules
  "Pure business logic for skill cooldown management.
  
  No atoms, no event firing, no side effects. Reducer layer combines these
  with event generation.
  
  Cooldown key = [ctrl-id sub-id] (both keywords).
  Main cooldown uses sub-id :main."
  (:require [cn.li.ac.ability.model.cooldown :as cdata]))

;; ============================================================================
;; Cooldown Checks
;; ============================================================================

(defn in-cooldown?
  "Check if a skill is in cooldown.
  
  Args:
    cooldown-data – CooldownData map
    ctrl-id       – keyword, controller id
    sub-id        – keyword, sub-cooldown id (default :main)"
  ([cooldown-data ctrl-id]
   (in-cooldown? cooldown-data ctrl-id :main))
  ([cooldown-data ctrl-id sub-id]
   (cdata/in-cooldown? cooldown-data ctrl-id sub-id)))

(defn get-remaining-ticks
  "Get remaining ticks for a cooldown.
  
  Returns: int (0 if not in cooldown)."
  ([cooldown-data ctrl-id]
   (get-remaining-ticks cooldown-data ctrl-id :main))
  ([cooldown-data ctrl-id sub-id]
   (cdata/get-remaining cooldown-data ctrl-id sub-id)))

;; ============================================================================
;; Duration resolution (Pure)
;;
;; One place decides how long a skill's cooldown is. It used to be three:
;; skill-effects/apply-cooldown! (:cooldown-policy/:ticks, else :cooldown-ticks,
;; else 1), context-state/apply-main-cooldown! (:cooldown-ticks only, else 1,
;; and it would have thrown on the fn-valued specs most skills declare), and the
;; HUD's own estimate (same order as the first, but defaulting to 100). Any
;; disagreement showed up as a cooldown wipe that did not match its countdown.
;; ============================================================================

(defn ticks-spec
  "The cooldown duration a skill spec declares: :cooldown-policy/:ticks wins
  over :cooldown-ticks. Either may be a number or a fn; nil when undeclared."
  [skill-spec]
  (or (get-in skill-spec [:cooldown-policy :ticks])
      (:cooldown-ticks skill-spec)))

(defn resolve-ticks-value
  "Evaluate a `ticks-spec` declaration to whole ticks (>= 1).

  A fn declaration is called with (player-id skill-id exp), falling back to a
  single {:player-id :skill-id :exp} map — skills author both shapes. An absent
  or unusable declaration is one tick, i.e. effectively no cooldown."
  ^long [v {:keys [player-id skill-id exp]}]
  (let [exp (double (or exp 0.0))
        raw (cond
              (number? v) (double v)
              (fn? v) (double (or (try
                                    (v player-id skill-id exp)
                                    (catch clojure.lang.ArityException _
                                      (v {:player-id player-id :skill-id skill-id :exp exp})))
                                  0.0))
              :else 0.0)]
    (max 1 (Math/round (double raw)))))

(defn resolve-ticks
  "Whole ticks to apply for `skill-spec`. See ticks-spec / resolve-ticks-value."
  ^long [skill-spec ctx]
  (resolve-ticks-value (ticks-spec skill-spec) ctx))

;; ============================================================================
;; Cooldown Management (Pure)
;; ============================================================================

(defn set-cooldown
  "Set a cooldown to ticks (never decreases existing).
  
  Returns: {:data updated :set? bool}."
  ([cooldown-data ctrl-id ticks]
   (set-cooldown cooldown-data ctrl-id ticks :main))
  ([cooldown-data ctrl-id ticks sub-id]
   (let [existing (get-remaining-ticks cooldown-data ctrl-id sub-id)
         set? (> ticks existing)
         data (cdata/set-cooldown cooldown-data ctrl-id sub-id ticks)]
     {:data data :set? set?})))

(defn clear-cooldown
  "Immediately remove a cooldown.
  
  Returns: {:data updated :cleared? bool}."
  ([cooldown-data ctrl-id]
   (clear-cooldown cooldown-data ctrl-id :main))
  ([cooldown-data ctrl-id sub-id]
   (let [key [ctrl-id sub-id]
         cleared? (contains? cooldown-data key)
         data (dissoc cooldown-data key)]
     {:data data :cleared? cleared?})))

(defn server-tick
  "Advance one server tick: decrement all cooldowns.
  
  Returns: {:data updated :ticked? bool}."
  [cooldown-data]
  (let [ticked? (not-empty cooldown-data)
        data (cdata/tick-cooldowns cooldown-data)]
    {:data data :ticked? ticked?}))

;; ============================================================================
;; Batch Operations
;; ============================================================================

(defn clear-all-cooldowns
  "Remove all active cooldowns.
  
  Returns: {:data updated :count int}."
  [cooldown-data]
  (let [count (count cooldown-data)]
    {:data {} :count count}))

(defn batch-set-cooldowns
  "Set multiple cooldowns at once.
  
  Args:
    cooldown-data – CooldownData map
    cooldown-list – [{:ctrl-id keyword :sub-id keyword :ticks int} ...]
  
  Returns: {:data updated :set-count int}"
  [cooldown-data cooldown-list]
  (let [data (reduce (fn [acc {:keys [ctrl-id sub-id ticks]}]
                       (cdata/set-cooldown acc ctrl-id sub-id ticks))
                     cooldown-data
                     cooldown-list)]
    {:data data :set-count (count cooldown-list)}))

;; ============================================================================
;; Queries (Pure)
;; ============================================================================

(defn get-all-cooldowns
  "Get all active cooldowns as a vector of {:ctrl-id :sub-id :ticks :max}."
  [cooldown-data]
  (mapv (fn [[[ctrl-id sub-id] {:keys [ticks max]}]]
          {:ctrl-id ctrl-id :sub-id sub-id :ticks ticks :max max})
        cooldown-data))

(defn get-cooldowns-for-controller
  "Get all sub-cooldowns for a specific controller.
  
  Returns: [{:sub-id :ticks} ...]"
  [cooldown-data ctrl-id]
  (->> cooldown-data
       (filter (fn [[[c _] _]] (= c ctrl-id)))
       (mapv (fn [[[_ s] {:keys [ticks max]}]]
               {:sub-id s :ticks ticks :max max}))
       vec))

(defn cooldown-data-size
  "Get number of active cooldowns."
  [cooldown-data]
  (count cooldown-data))

(defn has-any-cooldown?
  "Check if any cooldown is active."
  [cooldown-data]
  (not-empty cooldown-data))
