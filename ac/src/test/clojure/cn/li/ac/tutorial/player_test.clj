(ns cn.li.ac.tutorial.player-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.tutorial.model :as model]
            [cn.li.ac.tutorial.player :as player]))

(deftest mark-conditions-dirty-marks-flags-and-dirty-test
  (let [s (player/mark-conditions-dirty (model/fresh-state) [0 2])]
    (is (= #{0 2} (:condition-flags s)))
    (is (true? (:dirty? s)))
    ;; Idempotent: re-marking the same conditions keeps the flags.
    (is (= #{0 2} (:condition-flags (player/mark-conditions-dirty s [0]))))))

(deftest mark-conditions-dirty-state-stays-a-map-test
  ;; Regression: the old `->` threading passed the state map as the reduce
  ;; FN — `(reduce <state-map> mark-condition! <conditions>)` returns the
  ;; last condition index (a Long) and mark-dirty! assoc'd onto it. The
  ;; result must always be the updated state map.
  (is (map? (player/mark-conditions-dirty (model/fresh-state) [1])))
  (is (map? (player/mark-conditions-dirty (model/fresh-state) [1 2 3]))))
