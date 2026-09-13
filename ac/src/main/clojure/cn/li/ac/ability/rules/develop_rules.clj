(ns cn.li.ac.ability.rules.develop-rules
  "Development (timed learning) rules and completion orchestration.

  Responsibilities:
  - Start a development session (learn-skill or level-up)
  - Tick development progress each server tick
  - Complete development and return updated data + events
  - Abort development"
  (:require [cn.li.ac.ability.model.develop :as dev]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.registry.event :as evt]))

(defn- recalc-max-for-level-with-calc
  [resource-data level uuid]
  (let [base (rdata/recalc-max-values resource-data level)
        calc-extra {:uuid uuid}
        max-cp (evt/fire-calc-event! evt/CALC-MAX-CP (:max-cp base) calc-extra)
        max-ol (evt/fire-calc-event! evt/CALC-MAX-OVERLOAD (:max-overload base) calc-extra)]
    ;; recalc-max-values already refilled to its own maximum; redo it against
    ;; the post-CALC one so passive bonuses are part of the refill rather than
    ;; headroom left empty.
    (assoc base
           :max-cp max-cp
           :max-overload max-ol
           :cur-cp max-cp
           :cur-overload 0.0)))

(defn start-skill-learning
  "Start learning a skill. Returns {:develop-data :error nil|keyword}.
  Caller must validate conditions before calling."
  [develop-data developer-type skill-id]
  (if (dev/developing? develop-data)
    {:develop-data develop-data :error :already-developing}
    (let [s (skill/get-skill skill-id)]
      (if-not s
        {:develop-data develop-data :error :unknown-skill}
        (let [max-stim (dev/skill-learning-stims (:level s))]
          {:develop-data (dev/start-develop develop-data
                                            developer-type
                                            :learn-skill
                                            {:skill-id skill-id}
                                            max-stim)
           :error nil})))))

(defn start-level-up
  "Start level-up process. Returns {:develop-data :error nil|keyword}."
  [develop-data developer-type current-level]
  (if (dev/developing? develop-data)
    {:develop-data develop-data :error :already-developing}
    (if (>= current-level (cfg/max-level))
      {:develop-data develop-data :error :max-level}
      (let [max-stim (dev/level-up-stims current-level)]
        {:develop-data (dev/start-develop develop-data
                                          developer-type
                                          :level-up
                                          {:target-level (inc current-level)}
                                          max-stim)
         :error nil}))))

(defn tick-develop
  "Advance one tick. Returns {:develop-data :completed? :action-type :action-data}.
  When :completed? is true, caller should apply the result."
  [develop-data]
  (if-not (dev/developing? develop-data)
    {:develop-data develop-data :completed? false}
    (let [ticked (dev/tick-develop develop-data)]
      (if (dev/done? ticked)
        {:develop-data ticked
         :completed? true
         :action-type (:action-type ticked)
         :action-data (:action-data ticked)}
        {:develop-data ticked :completed? false}))))

(defn apply-completion
  "Apply a completed development result to ability data.
  Returns {:ability-data :resource-data :events [...] :develop-data}."
  [develop-data ability-data resource-data uuid]
  (let [action-type (:action-type develop-data)
        action-data (:action-data develop-data)]
    (case action-type
      :learn-skill
      (let [skill-id (:skill-id action-data)
            already-learned? (adata/is-learned? ability-data skill-id)
            data (if already-learned?
                   ability-data
                   (adata/learn-skill ability-data skill-id))
            event (when-not already-learned?
                    (evt/make-skill-learn-event uuid skill-id))
            level (:level data)
            new-res (recalc-max-for-level-with-calc resource-data level uuid)]
        {:ability-data data
         :resource-data new-res
         :events (if event [event] [])
         :develop-data (dev/complete-and-reset develop-data)})

      :level-up
      (let [target-level (:target-level action-data)
            old-level (:level ability-data)
            data2 (adata/set-level ability-data target-level)
            new-res (-> resource-data
                        (rdata/reset-add-max)
                        (rdata/recalc-max-values target-level))
            event (evt/make-level-change-event uuid old-level target-level)]
        {:ability-data data2
         :resource-data new-res
         :events [event]
         :develop-data (dev/complete-and-reset develop-data)})

      :awaken
      ;; Category-less level-up (upstream DevelopActionLevel.onLearned with no
      ;; category): assign the category decided at start. set-category resets
      ;; level to 1 and clears skills; presets are empty pre-awakening.
      (let [target-category (:target-category action-data)
            old-category (:category-id ability-data)
            data2 (adata/set-category ability-data target-category)
            new-res (-> resource-data
                        (rdata/reset-add-max)
                        (rdata/recalc-max-values (:level data2 1)))
            event (evt/make-category-change-event uuid old-category target-category)]
        {:ability-data data2
         :resource-data new-res
         :events [event]
         :develop-data (dev/complete-and-reset develop-data)})

      {:ability-data ability-data
       :resource-data resource-data
       :events []
       :develop-data (dev/complete-and-reset develop-data)})))

(defn abort-develop
  "Abort current development. Returns {:develop-data}."
  [develop-data]
  {:develop-data (dev/abort develop-data)})
