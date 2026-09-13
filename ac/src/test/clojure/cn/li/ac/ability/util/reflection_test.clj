(ns cn.li.ac.ability.util.reflection-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.util.reflection :as rf]))

(deftest reflection-state-lifecycle-test
  (let [s0 (rf/init-reflection-state 3)
        s1 (rf/add-reflection-hit s0 "a")
        s2 (rf/add-reflection-hit s1 "b")]
    (is (= #{} (:hit-entities s0)))
    (is (= #{"a"} (:hit-entities s1)))
    (is (= 1 (:reflection-count s1)))
    (is (true? (rf/can-reflect? s1)))
    (is (true? (rf/is-entity-hit? s2 "a")))
    (is (false? (rf/is-entity-hit? s2 "c")))))

(deftest can-reflect-at-limit-test
  (let [s (-> (rf/init-reflection-state 2)
              (rf/add-reflection-hit "x")
              (rf/add-reflection-hit "y"))]
    (is (false? (rf/can-reflect? s)))))

(deftest calculate-reflection-damage-test
  (is (= 10.0 (rf/calculate-reflection-damage 10.0 0)))
  (is (= 5.0 (rf/calculate-reflection-damage 10.0 1)))
  (is (= 2.5 (rf/calculate-reflection-damage 10.0 2))))

(deftest get-reflection-targets-test
  (let [s (rf/add-reflection-hit (rf/init-reflection-state 5) "u1")
        entities [{:uuid "u1"} {:uuid "u2"} {:uuid "u3"}]]
    (is (= [{:uuid "u2"} {:uuid "u3"}] (vec (rf/get-reflection-targets entities s))))))
