(ns cn.li.ac.ability.ability-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.model.ability :as ability]))

(deftest ability-core-edge-test
  (let [d0 (ability/new-ability-data)
        d1 (ability/set-category d0 :electro)
        d2 (-> d1 (ability/learn-skill :railgun) (ability/set-skill-exp :railgun 0.4))
        {:keys [data delta]} (ability/add-skill-exp d2 :railgun 0.8)]
    (is (= nil (:category-id d0)))
    (is (= :electro (:category-id d1)))
    (is (ability/is-learned? d2 :railgun))
    (is (= 0.4 (ability/get-skill-exp d2 :railgun)))
    (is (= 0.6 delta))
    (is (= 1.0 (ability/get-skill-exp data :railgun)))
    (is (= 0.0 (ability/get-skill-exp (ability/set-skill-exp d2 :a -1) :a)))
    (is (= 1.0 (ability/get-skill-exp (ability/set-skill-exp d2 :a 10) :a)))))

(deftest ability-contract-test
  (let [d0 (-> (ability/new-ability-data)
               (ability/set-category :a)
               (ability/learn-skill :s1)
               (ability/set-skill-exp :s1 0.5)
               (ability/add-level-progress 3.5))
        d1 (ability/set-category d0 :b)
        d2 (ability/set-level d1 3)
        d3 (ability/set-level-progress d2 -1)]
    (is (= #{} (:learned-skills d1)))
    (is (= {} (:skill-exps d1)))
    (is (= 1 (:level d1)))
    (is (= 3 (:level d2)))
    (is (= 0.0 (:level-progress d2)))
    (is (= 0.0 (:level-progress d3)))
    (is (thrown? IllegalArgumentException (ability/set-level d1 0)))
    (is (thrown? IllegalArgumentException (ability/set-level d1 6)))))
