(ns cn.li.mcmod.block.inventory-helpers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.block.inventory-helpers :as helpers]
            [cn.li.mcmod.platform.be :as pbe]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.structured-data :as sd]))

(deftest load-inventory-test
  (let [default [nil nil nil]
        inv-tag [{:Slot 0 :id "iron_ingot"}
                 {:Slot 2 :id "EMPTY"}
                 {:Slot 9 :id "out_of_range"}]]
    (testing "returns default when key not present"
      (with-redefs [sd/has-key-safe? (fn [_ _] false)]
        (is (= default (helpers/load-inventory :tag "inv" default)))))
    (testing "loads valid slots and ignores empty/out-of-range entries"
      (with-redefs [sd/has-key-safe? (fn [_ _] true)
                    sd/get-list (fn [_ _] inv-tag)
                    sd/list-size count
                    sd/list-structured (fn [xs i] (nth xs i))
                    sd/get-int (fn [compound _] (:Slot compound))
                    pitem/from-data identity
                    pitem/empty? (fn [item] (= "EMPTY" (:id item)))]
        (is (= [{:Slot 0 :id "iron_ingot"} nil nil]
               (helpers/load-inventory :tag "inv" default)))))))

(deftest save-inventory-test
  (let [state {:inventory [nil {:id "a"} nil {:id "b"}]}
        writes (atom [])]
    (with-redefs [sd/create-list (fn [] (atom []))
                  sd/create-structured (fn [] (atom {}))
                  sd/set-int! (fn [st k v] (swap! st assoc (keyword k) v))
                  pitem/save-to-data (fn [item st] (swap! st assoc :item item))
                  sd/append! (fn [lst st] (swap! lst conj @st))
                  sd/set-entry! (fn [_ key lst] (swap! writes conj [key @lst]))]
      (helpers/save-inventory state :tag "inv")
      (is (= [["inv" [{:Slot 1 :item {:id "a"}}
                      {:Slot 3 :item {:id "b"}}]]]
             @writes)))))

(deftest update-be-field-test
  (let [changed (atom 0)
        saved (atom nil)]
    (with-redefs [pbe/get-custom-state (fn [_] {:a 1})
                  pbe/set-custom-state! (fn [be st] (reset! saved [be st]))
                  pbe/set-changed! (fn [_] (swap! changed inc))]
      (is (= :be (helpers/update-be-field! :be :b 2)))
      (is (= [:be {:a 1 :b 2}] @saved))
      (is (= 1 @changed)))
    (testing "set-changed failure is swallowed"
      (with-redefs [pbe/get-custom-state (fn [_] nil)
                    pbe/set-custom-state! (fn [be st] (reset! saved [be st]))
                    pbe/set-changed! (fn [_] (throw (ex-info "ignore" {})))]
        (is (= :be2 (helpers/update-be-field! :be2 :v 9)))
        (is (= [:be2 {:v 9}] @saved))))))

