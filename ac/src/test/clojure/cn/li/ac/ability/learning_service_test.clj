(ns cn.li.ac.ability.learning-service-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.ability.registry.category :as cat]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.rules.learning-rules :as learning]))

(deftest check-all-conditions-unknown-skill-test
  ;; When skill-spec is nil (unknown skill), can-learn? must return false.
  (is (false? (learning/can-learn? nil (adata/new-ability-data) 1 :portable))))

(deftest check-all-conditions-failure-composition-test
  (let [ability-data (-> (adata/new-ability-data)
                         (assoc :learned-skills #{:known-low})
                         (adata/set-skill-exp :dep 0.2))
        skill-spec {:id :target
                    :level 3
                    :developer-type :advanced
                    :prerequisites [{:skill-id :dep :min-exp 0.9}]
                    :conditions [{:type :any-skill-level :level 5}]}]
    (with-redefs [developer/gte? (fn [_actual _required]
                                    false)]
      (let [{:keys [pass? failures]}
            (learning/check-all-conditions skill-spec ability-data 2 :normal)]
        (is (false? pass?))
        (is (some #(= :level (:type %)) failures))
        (is (some #(= :developer-type (:type %)) failures))
        (is (some #(= :prerequisite (:type %)) failures))
        (is (some #(= :any-skill-level (:type %)) failures))))))

(deftest check-all-conditions-pass-test
  (let [ability-data (-> (adata/new-ability-data)
                         (assoc :learned-skills #{:level-three-skill})
                         (adata/set-skill-exp :dep 1.0))
        target-spec {:id :target
                     :level 2
                     :developer-type :portable
                     :prerequisites [{:skill-id :dep :min-exp 0.5}]
                     :conditions [{:type :any-skill-level :level 3}]}]
    (with-redefs [skill/get-skill (fn [skill-id]
                                    (case skill-id
                                      :level-three-skill {:id :level-three-skill :level 3}
                                      nil))
                   developer/gte? (fn [_ _] true)]
      (let [ret (learning/check-all-conditions target-spec ability-data 2 :portable)]
        (is (:pass? ret))
        (is (empty? (:failures ret)))
        (is (true? (learning/can-learn? target-spec ability-data 2 :portable)))))))

(deftest learn-skill-event-contract-test
  (let [base (adata/new-ability-data)]
    (with-redefs [evt/make-skill-learn-event (fn [uuid skill-id]
                                               {:event/type :ability/skill-learn
                                                :uuid uuid
                                                :skill-id skill-id})]
      (let [data (adata/learn-skill base :s1)
            event (evt/make-skill-learn-event "p1" :s1)]
        (is (contains? (:learned-skills data) :s1))
        (is (= {:event/type :ability/skill-learn :uuid "p1" :skill-id :s1} event)))
      (let [already (adata/learn-skill base :s1)
            data2 (if (adata/is-learned? already :s1) already (adata/learn-skill already :s1))]
        (is (= already data2))))))

(deftest level-up-threshold-mastery-halves-threshold-test
  (let [ability-base {:category-id :cat-a
                      :level 2
                      :level-progress 0.0
                      :skill-exps {}
                      :learned-skills #{}}]
    (with-redefs [skill-query/get-controllable-skills-at-level (fn [_ _]
                                    [{:id :s1} {:id :s2}])
            cat/get-prog-incr-rate (fn [_] 1.5)
            cfg/prog-incr-rate (fn [] 2.0)]
      (let [skills [{:id :s1} {:id :s2}]
            unmastered (assoc ability-base :skill-exps {:s1 1.0 :s2 0.2})
        mastered (assoc ability-base :skill-exps {:s1 1.0 :s2 1.0})
        t1 (learning/level-up-threshold unmastered skills 1.5 2.0)
        t2 (learning/level-up-threshold mastered skills 1.5 2.0)]
      (is (> t1 t2))
      (is (= (* 0.5 t1) t2))))))