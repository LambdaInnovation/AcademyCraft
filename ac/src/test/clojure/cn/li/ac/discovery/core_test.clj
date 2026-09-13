(ns cn.li.ac.discovery.core-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.discovery.core :as core]))

(deftest normalize-provider-contract-test
  (is (= {:id :demo
          :priority 42
          :skill-namespaces ['cn.li.ac.content.ability.electromaster.railgun]
          :fx-namespaces []}
         (core/normalize-provider
           {:id :demo
            :priority 42
            :skill-namespaces ['cn.li.ac.content.ability.electromaster.railgun
                               'cn.li.ac.content.ability.electromaster.railgun]
            :fx-namespaces []}))))

(deftest provider-sort-key-orders-priority-then-id-test
  (is (= [10 "a"] (core/provider-sort-key {:id :a :priority 10})))
  (is (< (compare (core/provider-sort-key {:id :a :priority 10})
                  (core/provider-sort-key {:id :l :priority 20}))
         0)))

(deftest base-family-and-fx-namespace-landscan-test
  (is (= :electromaster
         (core/base-family 'cn.li.ac.content.ability.electromaster/railgun))
      "family segment follows the ability path segment")
  (is (true? (core/fx-namespace? 'cn.li.ac.content.ability.electromaster/railgun-fx)))
  (is (false? (core/fx-namespace? 'cn.li.ac.content.ability.electromaster/railgun))))
