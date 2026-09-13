(ns cn.li.ac.ability.model.develop
  "Pure-data functions for DevelopData (timed learning state machine).

  DevelopData tracks the progress of a learning session (learning a skill or
  leveling up) that requires the player to interact with a Developer device
  over time.

  Map schema:
    {:state          keyword ; :idle | :developing | :done | :failed
     :developer-type keyword ; :portable | :normal | :advanced
     :action-type    keyword ; :learn-skill | :level-up
     :action-data    map     ; {:skill-id kw} or {:target-level int}
     :stim           int     ; completed stim units
     :max-stim       int     ; total stim units required
     :tick-this-stim int     ; ticks accumulated in current stim unit
     :energy-consumed float} ; total energy consumed this session"
  (:require [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.ability.rules.progression :as progression]))

;; ============================================================================
;; Stim count formulas (matches original)
;; ============================================================================

(defn skill-learning-stims
  "Stim count to learn a skill: configured base + level² × configured factor."
  [skill-level]
  (progression/skill-learning-stims skill-level))

(defn level-up-stims
  "Stim count to level up: configured base × (current-level + 1)."
  [current-level]
  (progression/level-up-stims current-level))

;; ============================================================================
;; Constructors
;; ============================================================================

(defn new-develop-data
  "Create idle DevelopData."
  []
  {:state          :idle
   :developer-type nil
   :action-type    nil
   :action-data    nil
   :stim           0
   :max-stim       0
   :tick-this-stim 0
   :energy-consumed 0.0})

(defn start-develop
  "Begin a development session. Returns updated DevelopData.
  action-type: :learn-skill or :level-up
  action-data: {:skill-id kw} or {:target-level int}
  max-stim: computed from skill-learning-stims or level-up-stims"
  [d developer-type action-type action-data max-stim]
  (assoc d
         :state          :developing
         :developer-type developer-type
         :action-type    action-type
         :action-data    action-data
         :stim           0
         :max-stim       (int max-stim)
         :tick-this-stim 0
         :energy-consumed 0.0))

;; ============================================================================
;; Tick
;; ============================================================================

(defn energy-per-tick
  "Energy consumed per tick: CPS / TPS"
  [developer-type]
  (developer/energy-per-tick developer-type))

(defn tick-develop
  "Advance one tick of development.
  Returns updated DevelopData. When :stim reaches :max-stim, state → :done."
  [d]
  (if (not= :developing (:state d))
    d
        (let [dt        (developer/developer-spec (:developer-type d))
          tps       (int (:tps dt))
          ept       (developer/energy-per-tick (:developer-type d))
          new-tick  (inc (:tick-this-stim d))
          new-energy (+ (:energy-consumed d) ept)]
      (if (>= new-tick tps)
        ;; Completed one stim unit
        (let [new-stim (inc (:stim d))]
          (if (>= new-stim (:max-stim d))
            ;; Done
            (assoc d
                   :state          :done
                   :stim           new-stim
                   :tick-this-stim 0
                   :energy-consumed new-energy)
            ;; Continue
            (assoc d
                   :stim           new-stim
                   :tick-this-stim 0
                   :energy-consumed new-energy)))
        ;; Still accumulating ticks in this stim
        (assoc d
               :tick-this-stim new-tick
               :energy-consumed new-energy)))))

;; ============================================================================
;; State queries
;; ============================================================================

(defn developing? [d] (= :developing (:state d)))
(defn done?       [d] (= :done (:state d)))
(defn failed?     [d] (= :failed (:state d)))
(defn idle?       [d] (= :idle (:state d)))

(defn progress
  "Returns development progress as a float [0.0, 1.0].
   Matches original AcademyCraft DevelopData.getDevelopProgress():
   progress = (stim + tickThisStim/tps) / maxStim"
  [d]
  (if (<= (:max-stim d) 0)
    0.0
    (let [dt (developer/developer-spec (:developer-type d))
          tps (max 1 (int (:tps dt)))
          factional-stim (+ (double (:stim d))
                           (/ (double (:tick-this-stim d)) (double tps)))]
      (/ factional-stim (double (:max-stim d))))))

(defn abort
  "Abort development, returning to idle."
  [_d]
  (new-develop-data))

(defn fail
  "Mark development as failed."
  [d]
  (assoc d :state :failed))

(defn complete-and-reset
  "After processing a :done result, reset to idle."
  [_d]
  (new-develop-data))
