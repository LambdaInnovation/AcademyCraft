(ns cn.li.ac.ability.service.reflection-damage-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.reflection-damage :as reflection-damage]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]))

(defn- with-empty-queue
  [f]
  (reflection-damage/reset-for-test!)
  (try
    (f)
    (finally
      (reflection-damage/reset-for-test!))))

(use-fixtures :each with-empty-queue)

(deftest enqueue-defers-and-deduplicates-multipart-hits-test
  (is (true? (reflection-damage/enqueue!
              {:world-id "the-end" :caster-id "player" :target-id "dragon" :damage 2.0})))
  (is (true? (reflection-damage/enqueue!
              {:world-id "the-end" :caster-id "player" :target-id "dragon" :damage 5.0})))
  (is (= [{:world-id "the-end" :caster-id "player" :target-id "dragon" :damage 5.0}]
         (reflection-damage/pending-tasks-snapshot))))

(deftest drain-applies-dedicated-reflection-damage-source-test
  (let [calls (atom [])]
    (with-redefs [entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage!
                  (fn [world-id target-id damage source-type options]
                    (swap! calls conj [world-id target-id damage source-type options])
                    true)]
      (reflection-damage/enqueue!
       {:world-id "the-end" :caster-id "player" :target-id "dragon" :damage 4.0})
      (is (empty? @calls) "damage is not applied inside the interception callback")
      (is (= [{:world-id "the-end"
               :caster-id "player"
               :target-id "dragon"
               :damage 4.0
               :applied? true}]
             (reflection-damage/drain!)))
      (is (= [["the-end" "dragon" 4.0 :vec-reflection {:attacker-uuid "player"}]]
             @calls))
      (is (empty? (reflection-damage/pending-tasks-snapshot))))))

(deftest nested-reflection-is-deferred-until-next-tick-test
  (let [calls (atom [])]
    (with-redefs [entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage!
                  (fn [_world-id target-id _damage _source-type _options]
                    (swap! calls conj target-id)
                    (when (= "first-target" target-id)
                      (reflection-damage/enqueue!
                       {:world-id "w"
                        :caster-id "foreign-handler"
                        :target-id "second-target"
                        :damage 3.0}))
                    true)]
      (reflection-damage/enqueue!
       {:world-id "w" :caster-id "player" :target-id "first-target" :damage 6.0})
      (reflection-damage/drain!)
      (is (= ["first-target"] @calls))
      (is (= ["second-target"]
             (mapv :target-id (reflection-damage/pending-tasks-snapshot))))
      (reflection-damage/drain!)
      (is (= ["first-target" "second-target"] @calls)))))

(deftest invalid-self-reflection-and-player-cleanup-test
  (is (false? (reflection-damage/enqueue!
               {:world-id "w" :caster-id "same" :target-id "same" :damage 5.0})))
  (reflection-damage/enqueue!
   {:world-id "w" :caster-id "leaving-player" :target-id "mob" :damage 5.0})
  (reflection-damage/enqueue!
   {:world-id "w" :caster-id "other" :target-id "leaving-player" :damage 4.0})
  (reflection-damage/clear-player-tasks! "leaving-player")
  (is (empty? (reflection-damage/pending-tasks-snapshot))))
