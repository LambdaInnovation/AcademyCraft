(ns cn.li.ac.content.ability.vecmanip.vecmanip-interaction-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.content.ability.vecmanip.arbitration :as arbitration]))

(defn- with-fresh-arbitration-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (ps-fix/seed-player-state! "p1" {})
      (ps-fix/seed-player-state! "p2" {})
      (try
        (f)
        (finally
          (arbitration/reset-projectile-locks-for-test! "p1")
          (arbitration/reset-projectile-locks-for-test! "p2"))))))

(use-fixtures :each with-fresh-arbitration-runtime)

(deftest arbitration-priority-defaults-to-reflection-test
  (with-redefs [skill-config/tunable-string-list
                (fn [_skill-id _field-id] [])]
    (is (= :vec-reflection (arbitration/preferred-skill-id)))
    (is (true? (arbitration/skill-allowed-in-dual-active? :vec-reflection)))
    (is (false? (arbitration/skill-allowed-in-dual-active? :vec-deviation)))))

(deftest arbitration-priority-config-switches-to-deviation-test
  (with-redefs [skill-config/tunable-string-list
                (fn [_skill-id _field-id] ["deviation-first"])]
    (is (= :vec-deviation (arbitration/preferred-skill-id)))
    (is (true? (arbitration/skill-allowed-in-dual-active? :vec-deviation)))
    (is (false? (arbitration/skill-allowed-in-dual-active? :vec-reflection)))))

(deftest same-projectile-same-tick-is-claimed-by-one-skill-only-test
  (with-redefs [cn.li.ac.content.ability.vecmanip.arbitration/current-tick (fn [] 42)]
    (is (true? (arbitration/claim-projectile! "p1" :vec-reflection "arrow-1")))
    (is (false? (arbitration/claim-projectile! "p1" :vec-deviation "arrow-1")))
    (is (true? (arbitration/claim-projectile! "p1" :vec-reflection "arrow-1")))))

(deftest same-projectile-can-be-reclaimed-on-next-tick-test
  (let [tick* (atom 100)]
    (with-redefs [cn.li.ac.content.ability.vecmanip.arbitration/current-tick (fn [] @tick*)]
      (is (true? (arbitration/claim-projectile! "p1" :vec-reflection "arrow-1")))
      (is (false? (arbitration/claim-projectile! "p1" :vec-deviation "arrow-1")))
      (swap! tick* inc)
      (is (true? (arbitration/claim-projectile! "p1" :vec-deviation "arrow-1"))))))

(deftest projectile-locks-can-clear-one-player-test
  (with-redefs [cn.li.ac.content.ability.vecmanip.arbitration/current-tick (fn [] 50)]
    (arbitration/claim-projectile! "p1" :vec-reflection "arrow-1")
    (arbitration/claim-projectile! "p2" :vec-reflection "arrow-1")
    (arbitration/clear-player-projectile-locks! "p1")
    (is (nil? (get-in (arbitration/projectile-locks-snapshot "p1") [:owners ["p1" "arrow-1"]])))
    (is (= :vec-reflection
           (get-in (arbitration/projectile-locks-snapshot "p2") [:owners ["p2" "arrow-1"]])))))

(deftest projectile-claims-are-per-player-test
  (with-redefs [cn.li.ac.content.ability.vecmanip.arbitration/current-tick (fn [] 77)]
    (is (true? (arbitration/claim-projectile! "p1" :vec-reflection "arrow-1")))
    (is (true? (arbitration/claim-projectile! "p2" :vec-deviation "arrow-1")))
    (is (= :vec-reflection
           (get-in (arbitration/projectile-locks-snapshot "p1") [:owners ["p1" "arrow-1"]])))
    (is (= :vec-deviation
           (get-in (arbitration/projectile-locks-snapshot "p2") [:owners ["p2" "arrow-1"]])))))
