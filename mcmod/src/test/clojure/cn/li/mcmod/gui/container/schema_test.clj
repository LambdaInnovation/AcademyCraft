(ns cn.li.mcmod.gui.container.schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.gui.container.schema :as schema]))

(def sample-fields
  [{:key :energy
    :init (fn [state] (:energy state))
    :sync? true
    :payload-key :e
    :coerce int
    :close-reset 0}
   {:key :label
    :init (fn [state] (:label state))
    :sync? false
    :coerce str
    :close-reset ""}
   {:key :enabled
    :init (fn [_] false)
    :sync? true
    :coerce boolean
    :close-reset false}])

(deftest atoms-core-test
  (testing "build-atoms and get-sync-data only include sync fields"
    (let [container (schema/build-atoms sample-fields {:energy 7 :label "init"})]
      (is (= 7 @(get container :energy)))
      (is (= "init" @(get container :label)))
      (is (= {:energy 7 :enabled false}
             (schema/get-sync-data sample-fields container))))))

(deftest reset-and-apply-edge-cases-test
  (testing "reset-atoms! applies close-reset and apply-sync-data! coerces present keys"
    (let [container {:energy (atom 10)
                     :label (atom "x")
                     :enabled (atom false)}]
      ;; :coerce int coerces numbers; strings are not supported by clojure.core/int
      (schema/apply-sync-data! sample-fields container {:energy 42 :enabled 1})
      (is (= 42 @(get container :energy)))
      (is (= true @(get container :enabled)))
      (is (= "x" @(get container :label)))
      (schema/reset-atoms! sample-fields container)
      (is (= 0 @(get container :energy)))
      (is (= "" @(get container :label)))
      (is (= false @(get container :enabled))))))

(deftest payload-contract-test
  (testing "sync-field-mappings and packet payload precedence contract"
    (let [container {:energy (atom 9) :enabled (atom true)}]
      (is (= [[:energy :e] :enabled]
             (schema/sync-field-mappings sample-fields)))
      (is (= {:energy 9 :enabled true}
             (schema/build-sync-packet-fields sample-fields container)))
      (is (= {:e 9 :enabled true}
             (schema/build-sync-packet-payload sample-fields container {:energy 1})))
      (is (= {:e 6 :enabled false}
             (schema/build-sync-packet-payload sample-fields nil {:energy 6})))
      (is (= {:e 0 :enabled false}
             (schema/build-sync-packet-payload sample-fields nil nil))))))

(deftest sync-cache-test
  (testing "sync payload helpers detect changes and cache the latest value"
    (let [last-sent (atom nil)
          has-sent? (atom false)]
      (is (true? (schema/sync-payload-dirty? last-sent has-sent? {:energy 1})))
      (schema/cache-sync-payload! last-sent has-sent? {:energy 1})
      (is (= {:energy 1} @last-sent))
      (is (true? @has-sent?))
      (is (false? (schema/sync-payload-dirty? last-sent has-sent? {:energy 1})))
      (is (true? (schema/sync-payload-dirty? last-sent has-sent? {:energy 2}))))))
