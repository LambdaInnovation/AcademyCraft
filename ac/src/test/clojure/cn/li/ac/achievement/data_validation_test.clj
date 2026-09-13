(ns cn.li.ac.achievement.data-validation-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.achievement.data :as data]))

(deftest achievement-data-loads-with-validated-shape-test
  (testing "tabs and achievements are loadable and non-empty"
    (is (seq data/tabs))
    (is (seq data/achievements)))
  (testing "each achievement has required low-frequency metadata keys"
    (doseq [ach data/achievements]
      (is (string? (:id ach)))
      (is (keyword? (:tab ach)))
      (is (sequential? (:criteria ach)))
      (is (map? (:translation ach)))
      (is (contains? (:translation ach) :en_us))
      (is (contains? (:translation ach) :zh_cn)))))
