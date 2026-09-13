(ns cn.li.ac.ability.server.damage.runtime-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.server.damage.runtime :as rt]))

(defn- reset-handlers! [f]
  (rt/reset-damage-handler-registry-for-test!)
  (try
    (f)
    (finally
      (rt/reset-damage-handler-registry-for-test!))))

(use-fixtures :each reset-handlers!)

(deftest register-and-priority-order-test
  ;; sort-by priority ascending: lower number runs first (+1 → *2)
  (is (true? (rt/register-damage-handler! :mul (fn [_ _ d _] (* d 2)) 10)))
  (is (true? (rt/register-damage-handler! :add (fn [_ _ d _] (+ d 1)) 1)))
  (is (= 8.0 (rt/process-damage! "p" "a" 3.0 :src))
      "priority 1 before 10: 3+1=4, then *2=8"))

(deftest unregister-test
  (rt/register-damage-handler! :x (fn [_ _ d _] (* d 2)) 0)
  (is (true? (rt/unregister-damage-handler! :x)))
  (is (= 5.0 (rt/process-damage! "p" "a" 5.0 :src))))

(deftest handler-exception-falls-back-test
  (rt/register-damage-handler! :bad (fn [_ _ _ _] (throw (Exception. "boom"))) 0)
  (is (= 4.0 (rt/process-damage! "p" "a" 4.0 :src))))

(deftest handler-invalid-return-vector-test
  (rt/register-damage-handler! :bad-vec (fn [_ _ _ _] [:not-a-number nil]) 0)
  (is (= 9.0 (rt/process-damage! "p" "a" 9.0 :src))))

(deftest handler-invalid-return-non-vector-test
  (rt/register-damage-handler! :bad-ret (fn [_ _ _ _] :oops) 0)
  (is (= 8.0 (rt/process-damage! "p" "a" 8.0 :src))))

(deftest handler-valid-vector-metadata-test
  (rt/register-damage-handler! :ok (fn [_ _ d _] [(* d 2) {:meta true}]) 0)
  (is (= 10.0 (rt/process-damage! "p" "a" 5.0 :src))))

(deftest damage-handler-registry-duplicate-and-freeze-policy-test
  (is (true? (rt/register-damage-handler! :dup (fn [_ _ d _] (+ d 1)) 0)))
  (is (true? (rt/register-damage-handler! :dup (fn [_ _ d _] (* d 10)) 0)))
  (is (= 3.0 (rt/process-damage! "p" "a" 2.0 :src))
      "duplicate handler id with same priority preserves the first handler")
  (is (false? (rt/register-damage-handler! :dup (fn [_ _ d _] d) 99)))
  (rt/freeze-damage-handler-registry!)
  (is (false? (rt/register-damage-handler! :new-handler (fn [_ _ d _] d) 0)))
  (is (false? (rt/unregister-damage-handler! :dup))))


