(ns cn.li.ac.ability.util.charge-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.util.charge :as ch]))

(defn- approx=
  ([a b] (approx= a b 1e-9))
  ([a b tol]
   (< (Math/abs (- (double a) (double b))) (double tol))))

(deftest init-and-update-charge-test
  (let [s (ch/init-charge-state 5 20 10)]
    (is (= 0 (:charge-ticks s)))
    (is (= 5 (:min-ticks s)))
    (is (= 20 (:max-ticks s)))
    (is (= 10 (:optimal-ticks s)))
    (is (number? (:started-at s))))
  (is (= 3 (:charge-ticks (ch/update-charge-progress (assoc (ch/init-charge-state 1 10 5) :charge-ticks 2))))))

(deftest is-charge-complete-test
  (is (false? (ch/is-charge-complete? (assoc (ch/init-charge-state 5 20 10) :charge-ticks 4))))
  (is (true? (ch/is-charge-complete? (assoc (ch/init-charge-state 5 20 10) :charge-ticks 5)))))

(deftest get-charge-multiplier-branches-test
  (let [base (ch/init-charge-state 4 12 8)]
    (testing "before min"
      (is (= 0.8 (ch/get-charge-multiplier (assoc base :charge-ticks 0)))))
    (testing "at min"
      (is (= 0.8 (ch/get-charge-multiplier (assoc base :charge-ticks 4)))))
    (testing "mid min-opt"
      (is (< 0.8 (ch/get-charge-multiplier (assoc base :charge-ticks 6)))))
    (testing "at optimal"
      (is (approx= 1.2 (ch/get-charge-multiplier (assoc base :charge-ticks 8)))))
    (testing "mid opt-max"
      (is (< 0.8 (ch/get-charge-multiplier (assoc base :charge-ticks 10)))))
    (testing "at max"
      (is (approx= 0.8 (ch/get-charge-multiplier (assoc base :charge-ticks 12)))))
    (testing "over max"
      (is (= 0.8 (ch/get-charge-multiplier (assoc base :charge-ticks 99)))))))

(deftest get-charge-progress-ratio-test
  (is (= 0.0 (ch/get-charge-progress-ratio (assoc (ch/init-charge-state 1 10 5) :charge-ticks 0))))
  (is (= 0.5 (ch/get-charge-progress-ratio (assoc (ch/init-charge-state 1 10 5) :charge-ticks 5))))
  (is (= 1.0 (ch/get-charge-progress-ratio (assoc (ch/init-charge-state 1 10 5) :charge-ticks 10))))
  (is (= 1.0 (ch/get-charge-progress-ratio (assoc (ch/init-charge-state 1 10 5) :charge-ticks 100)))))
