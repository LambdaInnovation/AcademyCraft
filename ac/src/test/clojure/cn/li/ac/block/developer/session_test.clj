(ns cn.li.ac.block.developer.session-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.develop :as dev-model]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.rules.develop-rules :as develop-rules]
            [cn.li.ac.ability.rules.learning-rules :as learning-rules]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.developer.session :as session]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest tick-development-progress-is-clamped-test
  (let [dd (dev-model/start-develop (dev-model/new-develop-data) :normal :level-up {:target-level 2} 1)
        state {:is-developing true
               :development-data dd
               :energy 100000.0
               :tier "normal"
               ;; completion validation reads player state by these ids —
               ;; nil ids NPE the runtime store's ConcurrentHashMap
               :player-state-session-id :test-session
               :user-uuid "p1"}
        completed (loop [s state n 0]
                      (cond
                        (:development-complete? s) s
                        (>= n 500) s
                        :else (recur (session/tick-development-state s) (inc n))))]
    (is (some? completed))
    (is (<= 0.0 (:development-progress completed) 1.0))
    (is (true? (:development-complete? completed)))))

(deftest tick-development-fails-when-energy-insufficient-test
  (let [dd (dev-model/start-develop (dev-model/new-develop-data) :normal :level-up {:target-level 2} 10)
        state {:is-developing true :development-data dd :energy 0.0 :tier "normal"}
        next (session/tick-development-state state)]
    (is (false? (:is-developing next)))
    (is (= :failed (:state (:development-data next))))))

(deftest validate-and-start-rejects-already-developing-test
  (let [player :stub-player]
    (with-redefs [uuid/player-uuid (constantly "p1")]
      (is (= {:ok? false :reason "already-developing"}
             (session/validate-and-start
               {:structure-valid true :is-developing true}
               player
               {:action :level-up}))))))

(deftest validate-and-start-learn-skill-starts-session-test
  (let [player :stub-player
        ability (assoc (adata/new-ability-data) :category-id :generic :level 3)
        skill-spec {:id :generic/foo :level 3 :prerequisites []}]
    (with-redefs [runtime-hooks/require-player-state-session-id (constantly :test-session)
                  store/get-or-create-player-state! (fn [_ _] {:ability-data ability})
                  learning-rules/check-all-conditions (fn [_ _ _ _] {:pass? true :failures []})
                  skill-registry/get-skill (fn [_] skill-spec)
                  develop-rules/start-skill-learning
                  (fn [_ _ sid]
                    {:develop-data (dev-model/start-develop (dev-model/new-develop-data)
                                                            :normal :learn-skill {:skill-id sid} 1)
                     :error nil})]
      (let [{:keys [ok? state]} (session/validate-and-start
                                  {:structure-valid true :tier "normal"}
                                  player
                                  {:action :learn-skill :skill-id "generic/foo"})]
        (is (true? ok?))
        (is (= :learn-skill (:development-action state)))
        (is (= :generic/foo (get-in state [:development-payload :skill-id])))))))

(deftest validate-and-start-level-up-defers-progress-validation-test
  ;; Progress validation happens at COMPLETION time (upstream DevelopData.tick
  ;; → type.validate() on the last stim tick), so starting a level-up session
  ;; succeeds even when can-level-up? is currently false.
  (let [player :stub-player
        ability (assoc (adata/new-ability-data) :category-id :generic :level 3)]
    (with-redefs [runtime-hooks/require-player-state-session-id (constantly :test-session)
                  store/get-or-create-player-state! (fn [_ _] {:ability-data ability})
                  skill-query/get-controllable-skills-at-level (constantly [])
                  category/get-prog-incr-rate (constantly 1.0)
                  learning-rules/can-level-up? (constantly false)]
      (let [{:keys [ok? state]} (session/validate-and-start
                                  {:structure-valid true :tier "normal"}
                                  player
                                  {:action :level-up})]
        (is (true? ok?))
        (is (= :level-up (:development-action state)))
        (is (true? (:is-developing state)))))))

(deftest clear-session-resets-fields-test
  (is (= {:is-developing false
          :development-progress 0.0
          :development-data nil
          :development-action nil
          :development-payload nil}
         (session/clear-session {:is-developing true
                                 :development-progress 0.5
                                 :development-data {}
                                 :development-action :level-up
                                 :development-payload {}}))))
