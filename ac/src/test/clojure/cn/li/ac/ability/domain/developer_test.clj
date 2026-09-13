(ns cn.li.ac.ability.domain.developer-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.ability.model.develop :as develop]
            [cn.li.ac.block.developer.config :as developer-config]))

(deftest developer-specs-are-single-source-test
  (testing "energy pacing remains aligned with classic developer constants"
    (is (= 30.0 (developer/energy-per-tick :portable)))
    (is (= 35.0 (developer/energy-per-tick :normal)))
    (is (= 40.0 (developer/energy-per-tick :advanced)))
    (is (= (developer/energy-per-tick :normal)
           (develop/energy-per-tick :normal)))))

(deftest developer-tier-rules-test
  (testing "minimum tiers are derived from the centralized domain rules"
    (is (= :portable (developer/min-for-level 1)))
    (is (= :portable (developer/min-for-level 2)))
    (is (= :normal (developer/min-for-level 3)))
    (is (= :advanced (developer/min-for-level 4)))
    (is (= :advanced (developer/min-for-level 5))))
  (testing "tier comparison keeps the original enum ordering"
    (is (true? (developer/gte? :advanced :normal)))
    (is (true? (developer/gte? :normal :portable)))
    (is (false? (developer/gte? :portable :advanced)))
    (is (false? (developer/gte? :missing :portable)))))

(deftest developer-controller-block-mapping-test
  (testing "only controller block ids map to station developer tiers"
    (is (= :normal (developer/developer-type-for-block-id "developer-normal")))
    (is (= :advanced (developer/developer-type-for-block-id :developer-advanced)))
    (is (true? (developer/controller-block? "developer-normal")))
    (is (true? (developer/controller-block? :developer-advanced)))
    (is (false? (developer/controller-block? "developer-normal-part")))
    (is (nil? (developer/developer-type-for-block-id "developer-normal-part")))))

(deftest developer-block-config-follows-domain-specs-test
  (doseq [tier [:normal :advanced]]
    (let [spec (developer/developer-spec tier)
          config (developer-config/tier-config tier)]
      (is (= (:energy spec) (:max-energy config)))
      (is (= (:cps spec) (:energy-per-stimulation config)))
      (is (= (:tps spec) (:stimulation-interval-ticks config))))))
