(ns cn.li.ac.block.ability-interferer.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.block.ability-interferer.config :as cfg]))

(deftest default-values-contract-test
  (testing "default-values mirrors descriptor defaults"
    (is (= (set (map :key cfg/descriptors))
           (set (keys cfg/default-values))))
    (doseq [{:keys [key default]} cfg/descriptors]
      (is (= default (get cfg/default-values key))))))

(deftest runtime-config-override-test
  (testing "range, energy, and performance getters read ability-devices overrides"
    (with-redefs [config-common/ability-devices-config
                  (fn []
                    {:ability-interferer-min-range 5.0
                     :ability-interferer-max-range 50.0
                     :ability-interferer-default-range 12.0
                     :ability-interferer-battery-pull-per-tick 7.5
                     :ability-interferer-max-energy 1234.0
                     :ability-interferer-energy-cost-per-range-squared 0.25
                     :ability-interferer-check-interval 9
                     :ability-interferer-sync-interval 17})]
      (is (= 5.0 (cfg/min-range)))
      (is (= 50.0 (cfg/max-range)))
      (is (= 12.0 (cfg/default-range)))
      (is (= 7.5 (cfg/battery-pull-per-tick)))
      (is (= 1234.0 (cfg/max-energy)))
      (is (= 9 (cfg/check-interval)))
      (is (= 17 (cfg/sync-interval)))
      (is (= 5.0 (cfg/clamp-range 1.0)))
      (is (= 50.0 (cfg/clamp-range 99.0)))
      (is (= 25.0 (cfg/calculate-energy-cost 10.0))))))