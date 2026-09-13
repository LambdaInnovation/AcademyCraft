(ns cn.li.ac.command.actions-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is use-fixtures]]            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.command.actions :as ac-actions]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.command.actions :as command-actions]))

(use-fixtures :each ps-fix/clean-player-states-fixture)

(deftest install-command-actions-registers-ac-manifest-test
  (ac-actions/install-command-actions!)
  (is (contains? (command-actions/valid-action-types) :learn-node))
  (is (contains? (command-actions/valid-action-types) :clear-cooldowns))
  (is (fn? (command-actions/get-action-executor :set-level))))

(deftest registered-command-actions-mutate-ac-player-state-test
  (ac-actions/install-command-actions!)
  (let [feedbacks (atom [])
        context {:metadata {:send-feedback-fn (fn [message translate? args error?]
                                                (swap! feedbacks conj [message translate? args error?]))}}
        uuid "command-player"
        initial (-> (store/fresh-player-state)
                    (assoc-in [:ability-data :category-id] :electromaster)
                    (assoc-in [:resource-data :cur-cp] 1.0)
                    (assoc-in [:resource-data :max-cp] 42.0)
                    (assoc :cooldown-data {[:railgun :main] {:ticks 20 :max 20}}))]
    (store/set-player-state! ps-fix/test-session-id uuid initial)

    (is (:success? (command-actions/execute {:action :set-level
                                             :level 3
                                             :player-uuid uuid}
                                            context)))
    (is (= 3 (get-in (store/get-player-state ps-fix/test-session-id uuid) [:ability-data :level])))

    (is (:success? (command-actions/execute {:action :restore-cp
                                             :player-uuid uuid}
                                            context)))
    (is (= 42.0 (get-in (store/get-player-state ps-fix/test-session-id uuid) [:resource-data :cur-cp])))

    (is (:success? (command-actions/execute {:action :clear-cooldowns
                                             :player-uuid uuid}
                                            context)))
    (is (= {} (:cooldown-data (store/get-player-state ps-fix/test-session-id uuid))))
    ;; dirty tracking moved off the state map onto the runtime's bitmask
    (is (pos? (store/dirty-mask ps-fix/test-session-id uuid)))
    (is (some #(= "command.academy.aim.level.success" (first %)) @feedbacks))))

(deftest registered-command-actions-delegate-stateful-mutations-to-service-test
  (ac-actions/install-command-actions!)
  (let [calls (atom [])
        context {:metadata {:player-state-owner {:server-session-id ps-fix/test-session-id}}}
        uuid "command-player"]
    (with-redefs [command-rt/run-command-in-session! (fn [session-id player-uuid command]
                                                        (swap! calls conj [:command session-id player-uuid command])
                                                        {:state {}})
                  command-rt/run-commands-in-session! (fn [session-id player-uuid commands]
                                                        (swap! calls conj [:commands session-id player-uuid (vec commands)])
                                                        {:state {}})
                  store/get-or-create-player-state! (fn [_ _]
                                                  {:ability-data {:category-id :electromaster}})
                  skill-query/get-skills-for-category (fn [_]
                                                        [{:id :railgun}
                                                         {:id :arc-gen}])]
      (doseq [action [{:action :learn-node :node-id :railgun :player-uuid uuid}
                      {:action :unlearn-node :node-id :railgun :player-uuid uuid}
                      {:action :learn-all-nodes :player-uuid uuid}
                      {:action :set-level :level 3 :player-uuid uuid}
                      {:action :set-node-exp :node-id :railgun :exp 0.5 :player-uuid uuid}
                      {:action :restore-cp :player-uuid uuid}
                      {:action :clear-cooldowns :player-uuid uuid}
                      {:action :reset-abilities :player-uuid uuid}
                      {:action :maxout-progression :player-uuid uuid}
                      {:action :max-all-progression :player-uuid uuid}]]
        (is (:success? (command-actions/execute action context))))
      (is (= [[:command ps-fix/test-session-id uuid {:command :learn-skill :skill-id :railgun :check-conditions? false}]
              [:command ps-fix/test-session-id uuid {:command :unlearn-skill :skill-id :railgun}]
              ;; learn_all learns and nothing else, matching upstream
              ;; AbilityData.learnAllSkills — no :set-skill-exp, so the skills
              ;; land at exp 0 exactly as a single `learn` leaves them.
              [:commands ps-fix/test-session-id uuid [{:command :learn-skill :skill-id :arc-gen :check-conditions? false}
                                                      {:command :learn-skill :skill-id :railgun :check-conditions? false}]]
              [:command ps-fix/test-session-id uuid {:command :set-level :level 3}]
              [:command ps-fix/test-session-id uuid {:command :set-skill-exp :skill-id :railgun :amount 0.5}]
              [:command ps-fix/test-session-id uuid {:command :recover-all}]
              [:command ps-fix/test-session-id uuid {:command :clear-all-cooldowns}]
              [:command ps-fix/test-session-id uuid {:command :reset-abilities}]
              ;; maxout only fills the current level's progress bar, matching
              ;; upstream maxOutLevelProgress.
              [:command ps-fix/test-session-id uuid {:command :maxout-level-progress}]
              ;; max_all is the non-upstream shortcut maxout used to be, and
              ;; must learn before setting exp because set-skill-exp rejects
              ;; unlearned skills.
              [:commands ps-fix/test-session-id uuid [{:command :set-level :level 5}
                                                      {:command :learn-skill :skill-id :arc-gen :check-conditions? false}
                                                      {:command :learn-skill :skill-id :railgun :check-conditions? false}
                                                      {:command :set-skill-exp :skill-id :arc-gen :amount 1.0}
                                                      {:command :set-skill-exp :skill-id :railgun :amount 1.0}]]]
             @calls)))))



