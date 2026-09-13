(ns cn.li.ac.ability.resource-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [cn.li.ac.ability.model.resource :as resource]
            [cn.li.ac.ability.config :as cfg]))

(deftest resource-core-and-edge-test
  (with-redefs [cfg/base-max-cp-for-level (fn [level] (* 100.0 level))
                cfg/base-max-overload-for-level (fn [level] (* 10.0 level))
                cfg/add-cp-ceiling (fn [_level] 50.0)
                cfg/add-overload-ceiling (fn [_level] 10.0)]
    (let [d0 (resource/new-resource-data)]
      (is (= 100.0 (:max-cp d0)))
      (is (= 10.0 (:max-overload d0)))
      (is (false? (resource/can-use-ability? d0)))
      (let [d1 (-> d0
                   (resource/set-activated true)
                   (resource/add-interference :jammer))
            d2 (resource/remove-interference d1 :jammer)]
        (is (false? (resource/can-use-ability? d1)))
        (is (true? (resource/can-use-ability? d2))))
      (let [d3 (resource/consume-cp (assoc d0 :cur-cp 20.0) 7.0 15)]
        (is (= 13.0 (:cur-cp d3)))
        (is (= 15 (:until-recover d3))))
      (let [[d4 overloaded?] (resource/add-overload (assoc d0 :max-overload 30.0) 50.0 20)]
        (is (true? overloaded?))
        (is (= 30.0 (:cur-overload d4)))
        (is (false? (:overload-fine d4)))
        (is (= 20 (:until-overload-recover d4))))
      (let [d5 (resource/tick-cp-recovery (assoc d0 :cur-cp 50.0 :max-cp 100.0 :until-recover 0) 1.0)]
        (is (> (:cur-cp d5) 50.0)))
      (let [d6 (resource/tick-overload-recovery
                {:cur-overload 5.0 :max-overload 10.0 :overload-fine true :until-overload-recover 0}
                1.0)]
        (is (< (:cur-overload d6) 5.0)))
      (let [d7 (resource/recalc-max-values
                {:cur-cp 999.0 :cur-overload 99.0 :add-max-cp 10.0 :add-max-overload 2.0}
                2)]
        (is (= 210.0 (:max-cp d7)))
        (is (= 22.0 (:max-overload d7)))
        (is (= 210.0 (:cur-cp d7)))
        (is (= 0.0 (:cur-overload d7))
            "upstream recalcMaxValue sets curOverload = 0, it does not clamp"))
      ;; Starting below the new maximum proves the refill: clamping would have
      ;; left cur-cp at 1.0.
      (let [d7b (resource/recalc-max-values
                 {:cur-cp 1.0 :cur-overload 0.0 :add-max-cp 10.0 :add-max-overload 2.0}
                 2)]
        (is (= 210.0 (:cur-cp d7b))
            "upstream recalcMaxValue sets curCP = getMaxCP()"))
      ;; overloadFine is untouched, exactly as upstream leaves it; the recovery
      ;; tick is what clears the lockout once cur-overload has reached 0.
      (let [d7c (resource/recalc-max-values
                 {:cur-cp 0.0 :cur-overload 22.0 :overload-fine false
                  :until-overload-recover 0}
                 2)]
        (is (= 0.0 (:cur-overload d7c)))
        (is (false? (:overload-fine d7c)))
        (is (true? (:overload-fine (resource/tick-overload-recovery d7c 1.0)))
            "and the next recovery tick lifts the lockout"))
      (is (= 50.0 (:add-max-cp (resource/grow-max-cp {:add-max-cp 0.0} 10000 1.0 2))))
      (is (= 10.0 (:add-max-overload (resource/grow-max-overload {:add-max-overload 0.0} 9999 1.0 2)))))))

(deftest resource-contract-test
  (let [d {:activated true :overload-fine true :interferences #{}}]
    (is (true? (resource/can-perform? (assoc d :cur-cp 5.0) 0 2.0 false)))
    (is (false? (resource/can-perform? (assoc d :cur-cp 1.0) 0 2.0 false)))
    (is (true? (resource/can-perform? (assoc d :cur-cp 0.0) 0 2.0 true)))
    (is (= 0.0 (:cur-cp (resource/set-cur-cp {} -10))))
    (is (= 0 (:until-recover (resource/set-until-recover {} -1))))))

(defspec consume-cp-never-negative-property-test
  100
  (prop/for-all [cur (gen/double* {:min 0.0 :max 10000.0 :infinite? false :NaN? false})
                 used (gen/double* {:min 0.0 :max 10000.0 :infinite? false :NaN? false})]
    (let [d (resource/consume-cp {:cur-cp cur} used 0)]
      (>= (:cur-cp d) 0.0))))
