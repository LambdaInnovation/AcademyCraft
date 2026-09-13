(ns cn.li.ac.ability.model-invariants-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [cn.li.ac.ability.model.cooldown :as cooldown]
            [cn.li.ac.ability.model.resource :as resource]
            [cn.li.ac.ability.rules.resource-rules :as resource-rules]))

(def ^:private op-gen
  (gen/vector
    (gen/one-of [(gen/fmap (fn [v] [:consume-cp v]) (gen/choose 0 20))
                 (gen/fmap (fn [v] [:add-overload v]) (gen/choose 0 20))
                 (gen/return [:tick nil])])
    1 120))

(defn- apply-op [res-data [op v]]
  (case op
    :consume-cp
    (if (:success? (resource-rules/perform-resource res-data 0.0 (double v) false 5 1.0 1.0))
      (:data (resource-rules/perform-resource res-data 0.0 (double v) false 5 1.0 1.0))
      res-data)

    :add-overload
    (if (:success? (resource-rules/perform-resource res-data (double v) 0.0 false 5 1.0 1.0))
      (:data (resource-rules/perform-resource res-data (double v) 0.0 false 5 1.0 1.0))
      res-data)

    :tick
    (:data (resource-rules/server-tick-recovery res-data 3.0 3.0))

    res-data))


(deftest resource-non-negative-invariant-property-test
  (let [result (tc/quick-check
                 100
                 (prop/for-all [ops op-gen]
                   (let [initial (resource/set-activated (resource/new-resource-data) true)
                         final-state (reduce apply-op initial ops)]
                     (and (>= (:cur-cp final-state) 0.0)
                          (>= (:cur-overload final-state) 0.0)
                          (>= (:max-cp final-state) 0.0)
                          (>= (:max-overload final-state) 0.0)))))]
    (is (:pass? result) (pr-str result))))

(deftest cooldown-tick-monotonic-invariant-property-test
  (let [result (tc/quick-check
                 100
                 (prop/for-all [ticks (gen/choose 1 500)]
                   (let [initial (cooldown/set-cooldown (cooldown/new-cooldown-data) :ctrl :main ticks)
                         next-state (cooldown/tick-cooldowns initial)]
                     (<= (cooldown/get-remaining next-state :ctrl :main)
                         (cooldown/get-remaining initial :ctrl :main)))))]
    (is (:pass? result) (pr-str result))))

(deftest resource-recover-all-restores-safe-runtime-state-test
  (let [d (-> (resource/new-resource-data)
              (assoc :cur-cp 0.0)
              (assoc :cur-overload 10.0)
              (assoc :overload-fine false)
              (assoc :until-recover 10)
              (assoc :until-overload-recover 10))
        recovered (resource/recover-all d)]
    (is (= (:max-cp d) (:cur-cp recovered)))
    (is (= 0.0 (:cur-overload recovered)))
    (is (true? (:overload-fine recovered)))
    (is (= 0 (:until-recover recovered)))
    (is (= 0 (:until-overload-recover recovered)))))
