(ns cn.li.ac.item.item-energy-base-test
  (:require [clojure.test :refer [deftest is testing are]]
            [cn.li.ac.energy.imag-energy-item :as ie]
            [cn.li.ac.energy.service.item-manager :as im]
            [cn.li.ac.item.item-energy-base :as base]
            [cn.li.ac.test.support.nbt :as test-nbt]
            [cn.li.mcmod.platform.item :as item]
            [cn.li.mcmod.platform.structured-data :as sd]))

(defn- fake-stack [registry-id custom-data]
  {:reg registry-id :tag custom-data})

(deftest create-energy-item-test
  (let [b (base/create-energy-item :energy-unit)]
    (is (ie/imag-energy-item? b))
    (is (= 10000.0 (ie/get-max-energy b)))
    (is (= 20.0 (ie/get-bandwidth b))))
  (is (thrown? IllegalArgumentException (base/create-energy-item :nope))))

(deftest is-energy-item-by-registry-test
  (let [tag (test-nbt/atom-compound)
        stk (fake-stack "academy:energy_unit" tag)]
    (with-redefs [item/object identity
                  item/registry-name (fn [o] (:reg o))
                  item/custom-data (fn [o] (:tag o))
                  item/ensure-custom-data (fn [o] (:tag o))]
      (is (true? (im/is-energy-item-supported? stk)))
      (is (= 10000.0 (im/get-item-capacity stk))))))

(deftest is-energy-item-by-nbt-type-test
  (let [tag (test-nbt/atom-compound)
        _ (sd/set-string! tag "batteryType" "energy-unit")
        stk (fake-stack "other:item" tag)]
    (with-redefs [item/object identity
                  item/registry-name (fn [o] (:reg o))
                  item/custom-data (fn [o] (:tag o))
                  item/ensure-custom-data (fn [o] (:tag o))]
      (is (true? (im/is-energy-item-supported? stk))))))

(deftest set-and-clamp-energy-test
  (let [tag (test-nbt/atom-compound)
        stk (fake-stack "academy:developer_portable" tag)]
    (with-redefs [item/object identity
                  item/registry-name (fn [o] (:reg o))
                  item/custom-data (fn [o] (:tag o))
                  item/ensure-custom-data (fn [o] (:tag o))]
      (im/set-item-energy! stk 5000.0)
      (is (= 5000.0 (sd/get-double tag "energy")))
      (im/set-item-energy! stk -10.0)
      (is (= 0.0 (sd/get-double tag "energy")))
      (im/set-item-energy! stk 9.99e9)
      (is (= 10000.0 (sd/get-double tag "energy")))
      (is (= "developer-portable" (sd/get-string tag "batteryType"))))))

(deftest charge-and-pull-behavior-test
  (let [tag (doto (test-nbt/atom-compound) (sd/set-double! "energy" 0.0))
        stk (fake-stack "academy:energy_unit" tag)]
    (with-redefs [item/object identity
                  item/registry-name (fn [o] (:reg o))
                  item/custom-data (fn [o] (:tag o))
                  item/ensure-custom-data (fn [o] (:tag o))]
      (are [amt ig leftover] (= leftover (im/charge-energy-to-item stk amt ig))
        100.0 false 80.0
        5.0 true 0.0)
      (is (= 25.0 (sd/get-double tag "energy")))
      (is (= 20.0 (im/pull-energy-from-item stk 1000.0 false)))
      (is (= 5.0 (sd/get-double tag "energy")))
      (is (= 5.0 (im/pull-energy-from-item stk 100.0 true)))
      (is (= 0.0 (sd/get-double tag "energy"))))))
