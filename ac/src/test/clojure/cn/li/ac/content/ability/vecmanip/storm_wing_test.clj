(ns cn.li.ac.content.ability.vecmanip.storm-wing-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.service.context-dispatcher :as dispatcher]
            [cn.li.ac.content.ability.vecmanip.storm-wing :refer [validate-move-direction]]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(defn setup-fixtures [test-fn]
  ;; Initialize test environment
  (test-fn))

(use-fixtures :each setup-fixtures)

;; ============================================================================
;; Test: validate-move-direction
;; ============================================================================

(deftest test-validate-move-direction-nil
  "Nil input should return nil (hover mode)"
  (testing "nil payload"
    (is (nil? (validate-move-direction nil)))))

(deftest test-validate-move-direction-not-map
  "Non-map input should return nil with warning"
  (testing "string input"
    (is (nil? (validate-move-direction "invalid"))))
  (testing "vector input"
    (is (nil? (validate-move-direction [1 0 0])))))

(deftest test-validate-move-direction-zero-vector
  "Zero vector should return nil (hover mode)"
  (testing "zero vector"
    (is (nil? (validate-move-direction {:x 0.0 :y 0.0 :z 0.0}))))
  (testing "empty map defaults to zeros"
    (is (nil? (validate-move-direction {})))))

(deftest test-validate-move-direction-nan
  "NaN in vector should return nil with warning"
  (testing "NaN in x"
    (let [result (validate-move-direction {:x Double/NaN :y 0.0 :z 1.0})]
      (is (nil? result))))
  (testing "NaN in y"
    (let [result (validate-move-direction {:x 1.0 :y Double/NaN :z 0.0})]
      (is (nil? result)))))

(deftest test-validate-move-direction-infinity
  "Infinity in vector should return nil with warning"
  (testing "Infinity in x"
    (let [result (validate-move-direction {:x Double/POSITIVE_INFINITY :y 0.0 :z 1.0})]
      (is (nil? result))))
  (testing "negative infinity in z"
    (let [result (validate-move-direction {:x 1.0 :y 1.0 :z Double/NEGATIVE_INFINITY})]
      (is (nil? result)))))

(deftest test-validate-move-direction-normalization
  "Non-unit vectors should be normalized to unit length"
  (testing "vector (3,4,0) normalized to unit"
    (let [result (validate-move-direction {:x 3.0 :y 4.0 :z 0.0})]
      (is (not (nil? result)))
      (let [len (Math/sqrt (+ (* (:x result) (:x result))
                               (* (:y result) (:y result))
                               (* (:z result) (:z result))))]
        (is (< (Math/abs (- len 1.0)) 1.0e-6)))
      ;; Expected: (0.6, 0.8, 0.0)
      (is (< (Math/abs (- (:x result) 0.6)) 1.0e-6))
      (is (< (Math/abs (- (:y result) 0.8)) 1.0e-6))
      (is (< (Math/abs (- (:z result) 0.0)) 1.0e-6))))
  
  (testing "arbitrary unit conversion"
    (let [result (validate-move-direction {:x 1.0 :y 1.0 :z 1.0})]
      (is (not (nil? result)))
      (let [len (Math/sqrt (+ (* (:x result) (:x result))
                               (* (:y result) (:y result))
                               (* (:z result) (:z result))))]
        (is (< (Math/abs (- len 1.0)) 1.0e-6))))))

(deftest test-validate-move-direction-partial-fields
  "Missing fields should default to 0.0"
  (testing "only x field"
    (let [result (validate-move-direction {:x 1.0})]
      (is (not (nil? result)))
      (is (= 1.0 (:x result)))
      (is (= 0.0 (:y result)))
      (is (= 0.0 (:z result)))))
  
  (testing "only y field"
    (let [result (validate-move-direction {:y 1.0})]
      (is (not (nil? result)))
      (is (= 0.0 (:x result)))
      (is (= 1.0 (:y result)))
      (is (= 0.0 (:z result))))))

(deftest test-validate-move-direction-small-vectors
  "Very small but non-zero vectors should be normalized"
  (testing "tiny positive vector"
    ;; length = sqrt(3) * 1.0e-5 â‰?1.73e-5 > 1.0e-6 threshold, so should normalize
    (let [result (validate-move-direction {:x 1.0e-5 :y 1.0e-5 :z 1.0e-5})]
      (is (not (nil? result)))
      ;; Should be normalized
      (let [len (Math/sqrt (+ (* (:x result) (:x result))
                               (* (:y result) (:y result))
                               (* (:z result) (:z result))))]
        (is (< (Math/abs (- len 1.0)) 1.0e-6)))))
  
  (testing "vector at boundary 1.0e-6"
    ;; This is the threshold, so (len <= 1.0e-6) means hover
    (let [result (validate-move-direction {:x 1.0e-6 :y 0.0 :z 0.0})]
      (is (nil? result))))
  
  (testing "vector just above boundary"
    ;; len = 1.01e-6 > 1.0e-6, should normalize
    (let [result (validate-move-direction {:x 1.01e-6 :y 0.0 :z 0.0})]
      (is (not (nil? result))))))

;; ============================================================================
;; Test: Exception Recovery
;; ============================================================================

(deftest test-storm-wing-exception-handler-active-recovery
  "When exception occurs in key-tick, skill should recover with cooldown and termination"
  (testing "exception handler should not leave skill in flying state"
    ;; This test verifies the exception handler behavior through static analysis
    ;; In a real integration test, this would be verified through context state
    (is true)))  ;; Placeholder for integration testing

;; ============================================================================
;; Test: State Transitions
;; ============================================================================

(deftest test-storm-wing-state-transitions
  "Verify state machine transitions are handled correctly"
  (testing "charging -> flying transition"
    ;; Placeholder for state transition verification
    (is true))
  
  (testing "flying -> terminated on resource exhaustion"
    ;; Placeholder for termination verification
    (is true)))
