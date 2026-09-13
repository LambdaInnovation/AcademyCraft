(ns cn.li.ac.ability.util.charge
  "Charge mechanics for hold-to-charge abilities.

  Charge state is stored in context :skill-state map.
  No Minecraft imports.")

(defn init-charge-state
  "Initialize charge state in context.

  Args:
    min-ticks: minimum charge time (ticks)
    max-ticks: maximum charge time (ticks)
    optimal-ticks: optimal charge time for max multiplier (ticks)

  Returns: charge state map"
  [min-ticks max-ticks optimal-ticks]
  {:charge-ticks 0
   :min-ticks min-ticks
   :max-ticks max-ticks
   :optimal-ticks optimal-ticks
   :started-at (System/currentTimeMillis)})

(defn update-charge-progress
  "Increment charge ticks.

  Args:
    charge-state: current charge state map

  Returns: updated charge state map"
  [charge-state]
  (update charge-state :charge-ticks inc))

(defn is-charge-complete?
  "Check if minimum charge time reached.

  Args:
    charge-state: current charge state map

  Returns: true if charge >= min-ticks"
  [charge-state]
  (>= (:charge-ticks charge-state) (:min-ticks charge-state)))

(defn get-charge-multiplier
  "Calculate charge multiplier based on timing.

  Multiplier peaks at optimal-ticks (1.2x), drops to 0.8x at min/max.

  Args:
    charge-state: current charge state map

  Returns: multiplier as double (0.8 - 1.2)"
  [charge-state]
  (let [ticks (:charge-ticks charge-state)
        min-t (:min-ticks charge-state)
        max-t (:max-ticks charge-state)
        opt-t (:optimal-ticks charge-state)]
    (cond
      ;; Before minimum charge
      (< ticks min-t)
      0.8

      ;; Between min and optimal
      (<= min-t ticks opt-t)
      (let [progress (/ (- ticks min-t) (- opt-t min-t))]
        (+ 0.8 (* 0.4 progress)))

      ;; Between optimal and max
      (<= opt-t ticks max-t)
      (let [progress (/ (- ticks opt-t) (- max-t opt-t))]
        (- 1.2 (* 0.4 progress)))

      ;; Over max
      :else
      0.8)))

(defn get-charge-progress-ratio
  "Get charge progress as ratio (0.0 - 1.0).

  Args:
    charge-state: current charge state map

  Returns: progress ratio as double"
  [charge-state]
  (let [ticks (:charge-ticks charge-state)
        max-t (:max-ticks charge-state)]
    (min 1.0 (/ (double ticks) (double max-t)))))
