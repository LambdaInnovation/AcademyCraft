(ns cn.li.ac.ability.learning-test
  "Integration checks against loaded skill registry (cn.li.ac.content.ability)."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.rules.learning-rules :as learning-rules]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.content.ability :as ability-content]))

(def ^:private learn-flow-cases
  [{:skill-id :arc-gen
    :category-id :electromaster}
   {:skill-id :electron-bomb
    :category-id :meltdowner}
   {:skill-id :threatening-teleport
    :category-id :teleporter}])

(deftest integration-registry-learn-flow-table-test
  (testing "learn + optional level-up event shape across representative skills"
    (ability-content/init-ability-content!)
    (doseq [{:keys [skill-id category-id]} learn-flow-cases]
      (let [d0 (-> (ad/new-ability-data)
                   (assoc :category-id category-id :level 3 :level-progress 10.0))
            skill-spec (skill-registry/get-skill skill-id)
            {:keys [pass?]} (learning-rules/check-all-conditions skill-spec d0 3 :advanced)
            data (if (ad/is-learned? d0 skill-id)
                   d0
                   (ad/learn-skill d0 skill-id))
            {:keys [old-level new-level]} (when (learning-rules/can-level-up?
                                                  data
                                                  []
                                                  1.0
                                                  1.0
                                                  5)
                                            (learning-rules/perform-level-up data))]
        (is (true? pass?) (str skill-id " should be learnable at level 3 / :advanced"))
        (is (contains? (:learned-skills data) skill-id)
            (str skill-id " should appear in :learned-skills"))
        (is (or (nil? old-level)
                (and (int? old-level) (int? new-level) (= 1 (- new-level old-level))))
            (str skill-id " level-up branch should be nil or produce consecutive levels"))))))