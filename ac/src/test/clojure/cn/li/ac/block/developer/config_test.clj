(ns cn.li.ac.block.developer.config-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.block.developer.config :as cfg]))

(deftest default-values-contract-test
  (is (= (set (map :key cfg/descriptors))
         (set (keys cfg/default-values))))
  (doseq [{:keys [key default]} cfg/descriptors]
    (is (= default (get cfg/default-values key)))))

(deftest tier-config-default-and-known-tier-test
  (is (= 50000.0 (get-in (cfg/tier-config :normal) [:max-energy])))
  (is (= 200000.0 (get-in (cfg/tier-config :advanced) [:max-energy])))
  (is (= (cfg/tier-config :normal) (cfg/tier-config :missing))))

(deftest tier-config-override-test
  (with-redefs [config-common/ability-devices-config
                (fn []
                  {:developer-validate-interval 37
                   :developer-normal-max-energy 1111.0
                   :developer-normal-energy-per-stimulation 22.0
                   :developer-normal-stimulation-interval-ticks 7
                   :developer-normal-wireless-bandwidth 333.0
                   :developer-advanced-max-energy 9999.0})]
    (is (= 37 (cfg/validate-interval)))
    (is (= {:max-energy 1111.0
            :energy-per-stimulation 22.0
            :stimulation-interval-ticks 7
            :wireless-bandwidth 333.0}
           (cfg/tier-config :normal)))
    (is (= 9999.0 (:max-energy (cfg/tier-config :advanced))))))
