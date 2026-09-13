(ns cn.li.ac.block.developer.session
  "Server-side timed development session for the developer block."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.ability.model.develop :as dev-model]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.rules.develop-rules :as develop-rules]
            [cn.li.ac.ability.rules.learning-rules :as learning-rules]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.item.special-items :as special-items]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.platform.entity :as entity]))


(defn clear-session
  [state]
  (-> state
      (assoc :is-developing false
             :development-progress 0.0
             :development-data nil
             :development-action nil
             :development-payload nil)))

(defn- developer-type-from-state [state]
  (keyword (or (:tier state) :normal)))

(defn- ability-state
  "Get or create player ability state. Accepts either:
   - [session-id pid] : explicit ids (used during tile tick, no player context)
   - [player]         : a player entity (used during request handling)"
  ([session-id pid]
   (store/get-or-create-player-state! session-id pid))
  ([player]
   (let [session-id (runtime-hooks/require-player-state-session-id "developer.session")
         pid (uuid/player-uuid player)]
     (store/get-or-create-player-state! session-id pid))))

(defn- validate-common [state player]
  (cond
    (not (:structure-valid state false)) {:ok? false :reason "invalid-structure"}
    (:is-developing state) {:ok? false :reason "already-developing"}
    :else
    (let [pid (uuid/player-uuid player)
          holder (str (:user-uuid state ""))]
      (if (and (not (str/blank? holder)) (not= holder pid))
        {:ok? false :reason "wrong-user"}
        {:ok? true :player-uuid pid}))))

(defn- begin-session [state developer-type action payload develop-data]
  (-> state
      (assoc :is-developing true
             :development-data develop-data
             :development-action action
             :development-payload payload
             :development-progress 0.0
             :tier (name developer-type))))

(defn- ok-session [state dev-type action payload develop-data]
  {:ok? true :state (begin-session state dev-type action payload develop-data)})

(defn- err [reason] {:ok? false :reason reason})

(defn- start-category-level-up [state dev-type ability-data cat-id level]
  ;; Start level-up development. Validation happens at completion time
  ;; matching original AcademyCraft DevelopData.tick which calls
  ;; type.validate() on the last stim tick, not at session start.
  (let [{:keys [develop-data error]}
        (develop-rules/start-level-up (dev-model/new-develop-data) dev-type level)]
    (if error
      (err (name error))
      (ok-session state dev-type :level-up
                  {:cat-id cat-id :level level}
                  develop-data))))

(defn resolve-awaken-category!
  "Decide the awaken target category (upstream DevelopActionLevel.chooseCategory):
   consume an induction factor and use its category when the player carries one,
   otherwise pick a random category. Returns the category keyword or nil.
   Shared by the block developer session and the portable developer handler."
  [player]
  (if-let [{:keys [item-id category]} (special-items/find-induction-factor player)]
    (when (entity/player-consume-item-by-id! player item-id 1)
      category)
    (let [categories (category/get-all-categories)]
      (when (seq categories)
        (:id (nth categories (rand-int (count categories))))))))

(defn- start-awaken-action [state player dev-type]
  (let [{:keys [develop-data error]}
        (develop-rules/start-level-up (dev-model/new-develop-data) dev-type 0)]
    (if error
      (err (name error))
      (if-let [category (resolve-awaken-category! player)]
        (ok-session state dev-type :awaken
                    {:target-category category}
                    develop-data)
        (err "no-awaken-category")))))

(defn- start-level-up-action [state player dev-type ability-data]
  (let [cat-id (:category-id ability-data)
        level (int (:level ability-data 1))]
    (if cat-id
      (start-category-level-up state dev-type ability-data cat-id level)
      (start-awaken-action state player dev-type))))

(defn- start-learn-skill-action [state dev-type ability-data skill-id]
  (let [cat-id (:category-id ability-data)
        level (int (:level ability-data 1))]
    (if-not cat-id
      (err "no-category")
      (if-let [skill-spec (skill-registry/get-skill (keyword skill-id))]
        (let [conditions (learning-rules/check-all-conditions skill-spec ability-data level dev-type)]
          (if-not (:pass? conditions)
            (err "conditions-not-met")
            (let [{:keys [develop-data error]}
                  (develop-rules/start-skill-learning
                    (dev-model/new-develop-data) dev-type (:id skill-spec))]
              (if error
                (err (name error))
                (ok-session state dev-type :learn-skill {:skill-id (:id skill-spec)} develop-data)))))
        (err "unknown-skill")))))

(defn- start-reset-action [state player dev-type ability-data]
  ;; Pre-validate reset conditions. Items are consumed at COMPLETION time,
  ;; matching original AcademyCraft DevelopActionReset.onLearned() which
  ;; consumes items AFTER successful timed development.
  (let [cat-id (:category-id ability-data)
        level (int (:level ability-data 1))]
    (cond
      (not (developer/gte? dev-type :advanced)) (err "requires-advanced-developer")
      (< level 3) (err "level-too-low")
      (not= special-items/magnetic-coil-item-id (entity/player-get-main-hand-item-id player)) (err "missing-magnetic-coil")
      :else
      (if-let [{:keys [item-id category]} (special-items/find-induction-factor player)]
        (if (= category cat-id)
          (err "same-category")
          (let [max-stim (* level 10)]  ;; reset-specific formula: level * 10 (matching original)
            (ok-session state dev-type :reset
                        {:target-category category
                         :new-level (max 1 (dec level))
                         :induction-item-id item-id
                         :player-uuid (uuid/player-uuid player)}
                        (dev-model/start-develop
                          (dev-model/new-develop-data) dev-type :reset
                          {:target-category category
                           :new-level (max 1 (dec level))
                           :induction-item-id item-id}
                          max-stim))))
        (err "missing-induction-factor")))))

(defn validate-and-start
  [state player {:keys [action skill-id]}]
  (if-let [common-error (some-> (validate-common state player) (#(when-not (:ok? %) %)))]
    common-error
    (let [dev-type (developer-type-from-state state)
          ability-data (:ability-data (ability-state player))]
      (case (keyword (or action :level-up))
        :level-up (start-level-up-action state player dev-type ability-data)
        :learn-skill (start-learn-skill-action state dev-type ability-data skill-id)
        :reset (start-reset-action state player dev-type ability-data)
        (err "unknown-action")))))

(defn tick-development-state
  [state]
  (if-not (:is-developing state)
    state
    (let [dd (or (:development-data state) (dev-model/new-develop-data))]
      (if-not (dev-model/developing? dd)
        (clear-session state)
        (let [dev-type (developer-type-from-state state)
              ept (dev-model/energy-per-tick dev-type)
              energy (double (:energy state 0.0))]
          (if (< energy ept)
            (-> state
                (assoc :development-data (dev-model/fail dd)
                       :is-developing false
                       :development-progress (dev-model/progress dd)))
            (let [{:keys [develop-data]} (develop-rules/tick-develop dd)
                  ticked develop-data
                  prog (dev-model/progress ticked)]
              (if (dev-model/done? ticked)
                ;; Validate at completion (matching original AcademyCraft
                ;; DevelopData.tick → type.validate() on last stim tick)
                (let [ability-data (:ability-data (ability-state (:player-state-session-id state) (:user-uuid state)))
                      valid? (case (:development-action state)
                               :level-up
                               (let [cat-id (:category-id ability-data)
                                     skills (when cat-id
                                              (skill-query/get-controllable-skills-at-level
                                               cat-id (int (:level ability-data 1))))
                                     cat-rate (when cat-id (category/get-prog-incr-rate cat-id))]
                                 (or (not cat-id)  ;; awakening always succeeds
                                     (learning-rules/can-level-up?
                                      ability-data skills cat-rate
                                      (cfg/prog-incr-rate) (cfg/max-level))))
                               :learn-skill
                               (let [skill-id (some-> (:development-payload state) :skill-id keyword)
                                     skill-spec (when skill-id (skill-registry/get-skill skill-id))]
                                 (if skill-spec
                                   (:pass? (learning-rules/check-all-conditions
                                            skill-spec ability-data
                                            (int (:level ability-data 1))
                                            (developer-type-from-state state)))
                                   false))
                               :awaken true   ;; always validated at start
                               :reset
                               ;; Re-validation deferred to run-completion-command!
                               ;; (items consumed there, matching original onLearned timing)
                               true
                               true)]
                  (if valid?
                    (assoc state
                           :development-data ticked
                           :development-progress 1.0
                           :is-developing false
                           :development-complete? true)
                    ;; Validation failed at completion → mark as failed
                    (assoc state
                           :development-data (dev-model/fail ticked)
                           :development-progress (dev-model/progress ticked)
                           :is-developing false
                           :development-complete? false)))
                (-> state
                    (assoc :development-data ticked :development-progress prog)
                    (update :energy - ept))))))))))

(defn- run-completion-command! [player-uuid action payload session-id & [player]]
  (let [session-id (or session-id
                       (runtime-hooks/require-player-state-session-id "developer.session"))]
    (case action
      :awaken
      (command-rt/run-command-in-session!
        session-id player-uuid
        {:command :change-category :new-category (:target-category payload)})
      :level-up
      (command-rt/run-command-in-session!
        session-id player-uuid {:command :level-up :force? true})
      :learn-skill
      (command-rt/run-command-in-session!
        session-id player-uuid
        {:command :learn-skill
         :skill-id (:skill-id payload)
         :check-conditions? false})
      :reset
      ;; Items consumed at COMPLETION time (matching original onLearned timing).
      ;; Re-validate: magnetic coil in main hand + induction factor in inventory.
      (if player
        (let [item-id (:induction-item-id payload)
              coil-ok? (= special-items/magnetic-coil-item-id
                          (entity/player-get-main-hand-item-id player))
              factor-ok? (and item-id
                              (pos? (entity/player-count-item-by-id player item-id)))]
          (if (and coil-ok? factor-ok?)
            (do
              (entity/player-consume-main-hand-item! player 1)
              (entity/player-consume-item-by-id! player item-id 1)
              (command-rt/run-command-in-session!
                session-id player-uuid
                {:command :change-category-with-level
                 :new-category (:target-category payload)
                 :new-level (:new-level payload)}))
            nil))
        ;; Fallback: no player reference, apply anyway (backward compat)
        (command-rt/run-command-in-session!
          session-id player-uuid
          {:command :change-category-with-level
           :new-category (:target-category payload)
           :new-level (:new-level payload)}))
      nil)))

(defn apply-completion!
  ;; Apply completed development. For :reset action, consumes magnetic coil +
  ;; induction factor at completion time (matching original onLearned timing).
  ([state] (apply-completion! state nil))
  ([state player]
   (when (:development-complete? state)
     (let [pid (str (:user-uuid state ""))]
       (when-not (str/blank? pid)
         (run-completion-command! pid
                                  (:development-action state)
                                  (:development-payload state)
                                  (:player-state-session-id state)
                                  player))))))
