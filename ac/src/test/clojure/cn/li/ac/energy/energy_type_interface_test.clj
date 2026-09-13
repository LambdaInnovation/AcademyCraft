(ns cn.li.ac.energy.energy-type-interface-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.energy.energy-type-interface :as energy-type]))

(defn- reset-registry-fixture [f]
  (energy-type/reset-energy-types-for-test!)
  (try
    (f)
    (finally
      (energy-type/reset-energy-types-for-test!))))

(use-fixtures :each reset-registry-fixture)

(defn- test-energy-type
  [type-id label]
  {:energy-type-id type-id
   :energy-type-name label
   :supports-item? (fn [item-stack] (= (:energy-type item-stack) type-id))
   :get-energy* (fn [item-stack] (double (or (:energy item-stack) 0.0)))
   :get-capacity* (fn [item-stack] (double (or (:capacity item-stack) 0.0)))
   :get-bandwidth* (fn [item-stack] (double (or (:bandwidth item-stack) 0.0)))
   :set-energy*! (fn [item-stack amount] (assoc item-stack :energy amount))
   :charge-item*! (fn [_item-stack _amount _ignore-bandwidth] 0.0)
   :discharge-item*! (fn [_item-stack amount _ignore-bandwidth] amount)})

(deftest register-energy-type-is-idempotent-for-same-instance-test
  (let [imag (test-energy-type :imaginary "Imaginary")]
    (is (identical? imag (energy-type/register-energy-type! imag)))
    (is (identical? imag (energy-type/register-energy-type! imag)))
    (is (= [imag] (energy-type/list-energy-types)))
    (is (identical? imag (energy-type/get-energy-type :imaginary)))
    (is (identical? imag (energy-type/resolve-energy-type {:energy-type :imaginary})))))

(deftest register-energy-type-conflicting-id-fails-test
  (let [first-type (test-energy-type :imaginary "Imaginary")
        other-type (test-energy-type :imaginary "Other")]
    (energy-type/register-energy-type! first-type)
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Conflicting energy type id"
         (energy-type/register-energy-type! other-type)))))

(deftest frozen-energy-type-registry-rejects-mutations-test
  (let [imag (test-energy-type :imaginary "Imaginary")]
    (energy-type/register-energy-type! imag)
    (energy-type/freeze-energy-types!)
    (is (true? (:frozen? (energy-type/energy-types-snapshot))))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Energy type registry is frozen"
         (energy-type/register-energy-type! (test-energy-type :other "Other"))))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Energy type registry is frozen"
         (energy-type/unregister-energy-type! :imaginary)))))


