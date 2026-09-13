(ns cn.li.ac.ability.cooldown-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [cn.li.ac.ability.model.cooldown :as cooldown]))

(deftest cooldown-core-edge-test
  (let [d0 (cooldown/new-cooldown-data)
        d1 (cooldown/set-cooldown d0 :cat :main 10)
        d2 (cooldown/set-cooldown d1 :cat :main 3)
        d3 (cooldown/set-cooldown d2 :cat :sub 1)
        d4 (cooldown/tick-cooldowns d3)]
    (is (cooldown/in-cooldown? d1 :cat :main))
    (is (= 10 (cooldown/get-remaining d2 :cat :main)))
    (is (= 9 (cooldown/get-remaining d4 :cat :main)))
    (is (= 0 (cooldown/get-remaining d4 :cat :sub)))
    (is (= 0 (cooldown/get-remaining d4 :none :none)))))

(deftest applied-duration-survives-ticking-test
  ;; Upstream SkillCooldown keeps maxTick as tickLeft counts down — it is what
  ;; the HUD's unavailable-wipe divides by (KeyHintUI: prog = tickLeft/maxTick).
  (let [d (cooldown/set-cooldown (cooldown/new-cooldown-data) :cat :main 40)
        ticked (nth (iterate cooldown/tick-cooldowns d) 10)]
    (is (= 30 (cooldown/get-remaining ticked :cat :main)))
    (is (= 40 (cooldown/get-max ticked :cat :main)))
    (testing "no entry → no recorded duration"
      (is (= 0 (cooldown/get-max d :other :main))))))

(deftest a-longer-cooldown-raises-both-halves-test
  ;; Upstream doSet: maxTick = max(cd, maxTick); tickLeft = max(cd, tickLeft).
  ;; Without raising max too, remaining could exceed it and the wipe would sit
  ;; pinned at full while the countdown ran.
  (let [d (-> (cooldown/new-cooldown-data)
              (cooldown/set-cooldown :cat :main 20)
              cooldown/tick-cooldowns
              (cooldown/set-cooldown :cat :main 60))]
    (is (= 60 (cooldown/get-remaining d :cat :main)))
    (is (= 60 (cooldown/get-max d :cat :main))))
  (testing "a shorter one changes neither"
    (let [d (-> (cooldown/new-cooldown-data)
                (cooldown/set-cooldown :cat :main 60)
                (cooldown/set-cooldown :cat :main 5))]
      (is (= 60 (cooldown/get-remaining d :cat :main)))
      (is (= 60 (cooldown/get-max d :cat :main))))))

(deftest cooldown-contract-test
  (let [d (-> (cooldown/new-cooldown-data)
              (cooldown/set-cooldown :a :main 5)
              (cooldown/set-cooldown :b :x 2))
        v (cooldown/cooldown-data->vec d)
        roundtrip (cooldown/vec->cooldown-data v)]
    (is (= d roundtrip))
    (is (= {[:a :main] {:ticks 5 :max 6}}
           (cooldown/tick-cooldowns {[:a :main] {:ticks 6 :max 6} :bad 1})))
    (testing "a 3-element row assumes the remaining ticks are the full duration"
      (is (= {[:a :main] {:ticks 1 :max 1}}
             (cooldown/vec->cooldown-data [["a" "main" 1]]))))))

(defspec tick-cooldowns-monotonic-property-test
  80
  (prop/for-all [ticks (gen/choose 1 200)]
    (let [d0 (cooldown/set-cooldown (cooldown/new-cooldown-data) :k :main ticks)
          d1 (cooldown/tick-cooldowns d0)]
      (and (<= (cooldown/get-remaining d1 :k :main)
               (cooldown/get-remaining d0 :k :main))
           ;; the recorded duration holds until the entry expires entirely
           (or (zero? (cooldown/get-remaining d1 :k :main))
               (= ticks (cooldown/get-max d1 :k :main)))))))
