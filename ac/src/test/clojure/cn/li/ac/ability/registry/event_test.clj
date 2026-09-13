(ns cn.li.ac.ability.registry.event-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.registry.event :as evt]))

(defn- reset-subs! [f]
  (evt/reset-ability-event-subscribers-for-test!)
  (try
    (f)
    (finally
      (evt/reset-ability-event-subscribers-for-test!))))

(use-fixtures :each reset-subs!)

(deftest fire-ability-event-dispatches-test
  (let [seen (atom [])]
    (evt/subscribe-ability-event! :ability/demo (fn [e] (swap! seen conj (:n e))))
    (is (= {:event/type :ability/demo :n 1}
           (evt/fire-ability-event! {:event/type :ability/demo :n 1})))
    (is (= [1] @seen))))

(deftest fire-ability-event-multiple-subscribers-test
  (let [a (atom 0)]
    (evt/subscribe-ability-event! :ability/x (fn [_] (swap! a + 1)))
    (evt/subscribe-ability-event! :ability/x (fn [_] (swap! a + 10)))
    (evt/fire-ability-event! {:event/type :ability/x})
    (is (= 11 @a))))

(deftest fire-calc-event-chains-test
  (evt/subscribe-ability-event! evt/CALC-SKILL-ATTACK (fn [m] (* 2.0 (:value m))))
  (evt/subscribe-ability-event! evt/CALC-SKILL-ATTACK (fn [m] (+ 1.0 (:value m))))
  (is (= 7.0 (evt/fire-calc-event! evt/CALC-SKILL-ATTACK 3.0 {:player-id "p"}))))

(deftest fire-calc-event-ignores-non-number-test
  (evt/subscribe-ability-event! evt/CALC-MAX-CP (fn [_m] :not-a-number))
  (is (= 50.0 (evt/fire-calc-event! evt/CALC-MAX-CP 50.0 {:uuid "u"}))))

(deftest subscriber-registry-freeze-policy-test
  (evt/subscribe-ability-event! :ability/demo (fn [_] nil))
  (is (= [:ability/demo] (keys (evt/subscriber-registry-snapshot))))
  (evt/freeze-ability-event-subscribers!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Ability event subscriber registry is frozen"
                        (evt/subscribe-ability-event! :ability/other (fn [_] nil)))))


