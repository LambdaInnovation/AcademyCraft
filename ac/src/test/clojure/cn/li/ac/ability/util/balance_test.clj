(ns cn.li.ac.ability.util.balance-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [cn.li.ac.ability.util.balance :as bal]))

(deftest lerp-test
  (testing "linear interpolation endpoints and midpoint"
    (is (= 0.0 (bal/lerp 0.0 10.0 0.0)))
    (is (= 10.0 (bal/lerp 0.0 10.0 1.0)))
    (is (= 5.0 (bal/lerp 0.0 10.0 0.5)))))

(deftest clamp01-test
  (testing "clamps doubles to unit interval"
    (is (= 0.0 (bal/clamp01 -99.0)))
    (is (= 1.0 (bal/clamp01 99.0)))
    (is (= 0.5 (bal/clamp01 0.5)))))

(deftest by-exp-test
  (testing "uses :exp from event with default 0"
    (let [f (bal/by-exp 10.0 20.0)]
      (is (= 10.0 (f {})))
      (is (= 10.0 (f {:exp 0.0})))
      (is (= 20.0 (f {:exp 1.0})))
      (is (= 15.0 (f {:exp 0.5}))))))

(deftest falloff-linear-test
  (testing "zero at and beyond radius, full at origin"
    (is (= 1.0 (bal/falloff-linear 0.0 5.0)))
    (is (= 0.0 (bal/falloff-linear 5.0 5.0)))
    (is (= 0.0 (bal/falloff-linear 10.0 5.0)))
    (is (= 0.5 (bal/falloff-linear 2.5 5.0))))
  (testing "avoids division by zero on tiny radius"
    (is (>= (bal/falloff-linear 0.0 1.0e-9) 0.0))))

(defspec clamp01-range-property-test
  100
  (prop/for-all [v (gen/double* {:infinite? false :NaN? false})]
    (let [out (bal/clamp01 v)]
      (and (<= 0.0 out) (<= out 1.0)))))
