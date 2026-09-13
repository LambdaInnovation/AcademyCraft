(ns cn.li.ac.ability.model.resource
  "Pure-data functions for ResourceData (CP / Overload).

  Map schema:
    {:cur-cp                float   ; current CP
     :max-cp                float   ; maximum CP (from config×level + growth)
     :cur-overload          float   ; current overload
     :max-overload          float   ; maximum overload
     :add-max-cp            float   ; accumulated CP growth (reset on level-up)
     :add-max-overload      float   ; accumulated overload growth (reset on level-up)
     :activated             bool    ; ability activation toggle
     :overload-fine         bool    ; true when overload < max (not in recovery)
     :until-recover         int     ; ticks before CP recovery starts (0 = recovering)
     :until-overload-recover int    ; ticks before overload recovery starts (0 = recovering)
     :interferences         #{}     ; set of active interference source IDs (keywords)}"
  (:require [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.util.scaling :as scaling]
            [cn.li.ac.ability.util.resource-check :as resource-check]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Constructors
;; ============================================================================

(defn new-resource-data
  "Create fresh ResourceData. max-cp and max-overload come from config."
  ([]
   (new-resource-data (cfg/base-max-cp-for-level 1)
                      (cfg/base-max-overload-for-level 1)))
  ([max-cp max-overload]
   {:cur-cp                 max-cp
    :max-cp                 max-cp
    :cur-overload           0.0
    :max-overload           max-overload
    :add-max-cp             0.0
    :add-max-overload       0.0
    :activated              false
    :overload-fine          true
    :until-recover          0
    :until-overload-recover 0
    :interferences          #{}}))

;; ============================================================================
;; Activation
;; ============================================================================

(defn is-activated? [d] (boolean (:activated d)))

(defn set-activated [d v] (assoc d :activated (boolean v)))

(defn set-cur-cp [d v]
  (assoc d :cur-cp (max 0.0 (double v))))

(defn set-cur-overload [d v]
  (assoc d :cur-overload (max 0.0 (double v))))

(defn set-until-recover [d ticks]
  (assoc d :until-recover (max 0 (int ticks))))

(defn recover-all
  "Restore the player to a fully recovered runtime state without changing max values."
  [d]
  (assoc d
         :cur-cp (double (:max-cp d 0.0))
         :cur-overload 0.0
         :overload-fine true
         :until-recover 0
         :until-overload-recover 0))

;; ============================================================================
;; Usage Guard
;; ============================================================================

(defn can-use-ability?
  "True when activated && not in overload recovery && no interference."
  [d]
  (resource-check/can-use-resource-data? d))

;; ============================================================================
;; Resource Consumption
;; ============================================================================

(defn can-perform?
  "Returns true if (overload, cp) can be consumed. creative? bypasses CP check."
  [d _overload cp creative?]
  (and (or creative? (>= (:cur-cp d) (double cp)))
       (can-use-ability? d)))

(defn consume-cp
  "Deduct cp from cur-cp, set until-recover to cooldown ticks.
  Returns updated ResourceData."
  [d cp cp-recover-cooldown]
  (-> d
      (update :cur-cp #(max 0.0 (- % (double cp))))
      (assoc  :until-recover cp-recover-cooldown)))

(defn add-overload
  "Add amount to cur-overload. Sets until-overload-recover cooldown.
  Returns [updated-data, overloaded?]."
  [d amount overload-recover-cooldown]
  (let [new-val  (+ (:cur-overload d) (double amount))
        max-val  (:max-overload d)
        capped   (min new-val max-val)
        hit-cap? (>= new-val max-val)]
    [(assoc d
            :cur-overload           capped
            :overload-fine          (not hit-cap?)
            :until-overload-recover overload-recover-cooldown)
     hit-cap?]))

;; ============================================================================
;; Recovery Tick
;; ============================================================================

(defn tick-cp-recovery
  "One tick of CP recovery.
  Formula: delta = speed × 0.0003 × maxCP × lerp(1, 2, curCP/maxCP)
  Returns updated ResourceData."
  [d recover-speed]
  (let [until (:until-recover d)]
    (cond
      ;; Still in post-use cooldown
      (pos? until)
      (update d :until-recover dec)

      ;; Full – nothing to do
      (>= (:cur-cp d) (:max-cp d))
      d

      ;; Recovering
      :else
      (let [speed   (double recover-speed)
            max-cp  (double (:max-cp d))
            cur-cp  (double (:cur-cp d))
            ratio   (if (pos? max-cp) (/ cur-cp max-cp) 0.0)
            delta   (* speed
                       (cfg/cp-recovery-rate-base)
                       max-cp
                       (scaling/lerp (cfg/cp-recovery-lerp-start)
                                     (cfg/cp-recovery-lerp-end)
                                     ratio))]
        (update d :cur-cp #(min max-cp (+ (double %) delta)))))))

(defn tick-overload-recovery
  "One tick of overload recovery.
  Formula: delta = speed × max(0.002×maxOL, 0.007×maxOL×lerp(1, 0.5, curOL/maxOL/2))
  Returns updated ResourceData."
  [d recover-speed]
  (cond
    ;; Fine state — normal overload decay
    (:overload-fine d)
    (let [until (:until-overload-recover d 0)]
      (if (pos? until)
        ;; Still in post-overload cooldown
        (update d :until-overload-recover dec)
        ;; Decay normally
        (if (<= (:cur-overload d) 0.0)
          d
          (let [speed   (double recover-speed)
                max-ol  (double (:max-overload d))
                cur-ol  (double (:cur-overload d))
                 ratio   (if (pos? max-ol) (/ cur-ol max-ol (cfg/overload-recovery-ratio-divisor)) 0.0)
                 delta   (* speed (max (* (cfg/overload-recovery-min-rate) max-ol)
                                (* (cfg/overload-recovery-active-rate)
                                  max-ol
                                  (scaling/lerp (cfg/overload-recovery-lerp-start)
                                           (cfg/overload-recovery-lerp-end)
                                           ratio))))
                new-val (max 0.0 (- cur-ol delta))]
            (assoc d :cur-overload new-val)))))

    ;; Overloaded — also decay but can't use abilities
    :else
    (let [until (:until-overload-recover d 0)]
      (if (pos? until)
        (update d :until-overload-recover dec)
        (let [speed   (double recover-speed)
              max-ol  (double (:max-overload d))
              cur-ol  (double (:cur-overload d))
                ratio   (if (pos? max-ol) (/ cur-ol max-ol (cfg/overload-recovery-ratio-divisor)) 0.0)
                delta   (* speed (max (* (cfg/overload-recovery-min-rate) max-ol)
                              (* (cfg/overload-recovery-active-rate)
                                max-ol
                                (scaling/lerp (cfg/overload-recovery-lerp-start)
                                          (cfg/overload-recovery-lerp-end)
                                          ratio))))
              new-val (max 0.0 (- cur-ol delta))]
          (if (zero? new-val)
            (assoc d :cur-overload 0.0 :overload-fine true)
            (assoc d :cur-overload new-val)))))))

(defn recovery-idle?
  "True iff both tick-cp-recovery and tick-overload-recovery would return d
  unchanged this tick — i.e. CP is full, overload is at rest (not overloaded,
  no post-use cooldowns pending), so a :server-tick command would be a pure
  no-op. Must mirror those two fns branch-for-branch; guarded by an
  equivalence test (recovery_idle_equivalence_test) so drift is caught
  immediately if the recovery formulas ever change."
  [d]
  (and (boolean (:overload-fine d))
       (not (pos? (int (or (:until-recover d) 0))))
       (not (pos? (int (or (:until-overload-recover d) 0))))
       (>= (double (or (:cur-cp d) 0.0)) (double (or (:max-cp d) 0.0)))
       (<= (double (or (:cur-overload d) 0.0)) 0.0)))

;; ============================================================================
;; Interference
;; ============================================================================

(defn add-interference [d src-id]
  (update d :interferences conj src-id))

(defn remove-interference [d src-id]
  (update d :interferences disj src-id))

(defn has-interference? [d src-id]
  (contains? (:interferences d) src-id))

;; ============================================================================
;; Max-value recalculation (called on level change)
;; ============================================================================

(defn recalc-max-values
  "Recompute max-cp/max-overload from the level grant plus accumulated growth,
  then refill CP and clear overload.

  Full port of upstream CPData.recalcMaxValue:

    maxCP = getInitCP(level); maxOverload = getInitOverload(level);
    curCP = getMaxCP();       curOverload = 0;

  where getMaxCP() = maxCP + addMaxCP, so the growth allowance is reached
  only through :add-max-cp / :add-max-overload and never granted up front.

  Refilling rather than clamping is upstream's behaviour on all three of its
  triggers — level change, skill learn, category change — which are exactly
  the triggers wired here.

  :overload-fine is deliberately left alone, as upstream leaves overloadFine
  alone: a player who recalculates mid-overload keeps the locked-out state
  until tick-overload-recovery clears it, which it does on the first tick
  past :until-overload-recover now that :cur-overload is already 0."
  [d level]
  (let [add-cp  (double (:add-max-cp d 0.0))
        add-ol  (double (:add-max-overload d 0.0))
        new-max-cp (+ (double (cfg/base-max-cp-for-level level)) add-cp)
        new-max-ol (+ (double (cfg/base-max-overload-for-level level)) add-ol)]
    (assoc d
           :max-cp       new-max-cp
           :max-overload new-max-ol
           :cur-cp       new-max-cp
           :cur-overload 0.0)))

(defn reset-add-max
  "Reset accumulated growth (called on level-up)."
  [d]
  (assoc d :add-max-cp 0.0 :add-max-overload 0.0))

(defn grow-max-cp
  "Grow add-max-cp by (consumed-cp × rate), capped at add-cp ceiling for level."
  [d consumed-cp rate level]
  (let [ceiling   (double (cfg/add-cp-ceiling level))
        current   (double (:add-max-cp d 0.0))
        growth    (* (double consumed-cp) (double rate))
        new-val   (min ceiling (+ current growth))]
    (assoc d :add-max-cp new-val)))

(defn grow-max-overload
  "Grow add-max-overload by clamp(0,10, overload × rate), capped at add-overload ceiling for level."
  [d cur-overload rate level]
  (let [ceiling   (double (cfg/add-overload-ceiling level))
        current   (double (:add-max-overload d 0.0))
        growth    (max 0.0 (min (cfg/max-overload-growth-per-event)
              (* (double cur-overload) (double rate))))
        new-val   (min ceiling (+ current growth))]
    (assoc d :add-max-overload new-val)))
