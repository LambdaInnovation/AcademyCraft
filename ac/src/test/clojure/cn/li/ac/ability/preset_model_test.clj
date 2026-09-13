(ns cn.li.ac.ability.preset-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.model.preset :as preset]))

(deftest preset-core-edge-test
  (let [d0 (preset/new-preset-data)
        d1 (preset/set-active-preset d0 2)
        d2 (preset/set-slot d1 2 0 [:cat :ctrl-a])
        d3 (preset/set-slot d2 2 1 [:cat :ctrl-b])
        d4 (preset/set-slot d3 2 1 nil)]
    (is (= 0 (preset/get-active-preset d0)))
    (is (= 2 (preset/get-active-preset d1)))
    (is (= [:cat :ctrl-a] (preset/get-slot d3 2 0)))
    (is (= nil (preset/get-slot d4 2 1)))
    (is (= [[:cat :ctrl-a] nil nil nil] (preset/get-active-slots d4)))
    (is (thrown? IllegalArgumentException (preset/set-active-preset d0 -1)))
    (is (thrown? IllegalArgumentException (preset/set-active-preset d0 4)))))

(deftest preset-serialization-contract-test
  (let [d {:active-preset 3
           :slots {[3 0] [:ac :x]
                   [3 1] [:ac :y]}}
        v (preset/preset-data->vec d)
        r (preset/vec->preset-data v 3)]
    (is (= 3 (:active-preset r)))
    (is (= [:ac :x] (get-in r [:slots [3 0]])))
    (is (= [:ac :y] (get-in r [:slots [3 1]])))
    (is (= d r))))
