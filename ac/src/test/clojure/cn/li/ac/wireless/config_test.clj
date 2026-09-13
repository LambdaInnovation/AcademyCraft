(ns cn.li.ac.wireless.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.item.mat-core :as mat-core]
            [cn.li.ac.block.wireless-matrix.stats :as matrix-stats]
            [cn.li.ac.wireless.config :as wireless-config]
            [cn.li.mcmod.config.registry :as config-reg]))

(deftest default-values-contract-test
  (testing "default-values mirrors descriptor defaults"
    (is (= (set (map :key wireless-config/descriptors))
           (set (keys wireless-config/default-values))))
    (doseq [{:keys [key default]} wireless-config/descriptors]
      (is (= default (get wireless-config/default-values key))))))

(deftest runtime-config-override-and-fallback-test
  (testing "network runtime getters use overrides and defaults"
    (with-redefs [config-reg/get-config-values
                  (fn [domain]
                    (is (= config-common/wireless-domain domain))
                    {:network-update-interval-ticks 77})]
      (is (= 77 (wireless-config/update-interval-ticks)))
      (is (= 2000.0 (wireless-config/buffer-max)))))
  (testing "empty config map falls back to built-in network defaults"
    (with-redefs [config-reg/get-config-values (fn [_] {})]
      (is (= 40 (wireless-config/update-interval-ticks)))
      (is (= 2000.0 (wireless-config/buffer-max))))))

(deftest search-config-override-and-fallback-test
  (testing "search getters return explicit override values when present"
    (with-redefs [config-reg/get-config-values
                  (fn [domain]
                    (is (= config-common/wireless-domain domain))
                    {:search-node-range 48.0
                     :search-max-results 333})]
      (is (= 48.0 (wireless-config/node-search-range)))
      (is (= 333 (wireless-config/max-results)))))
  (testing "missing search key in config keeps the module default"
    (with-redefs [config-reg/get-config-values (fn [_] {:search-max-results 256})]
      (is (= 20.0 (wireless-config/node-search-range)))
      (is (= 256 (wireless-config/max-results))))))

(deftest wireless-performance-paths-are-player-facing-test
  (testing "wireless core no longer exposes orphan tick.* matrix validate settings"
    (let [paths (set (map :path wireless-config/descriptors))]
      (is (contains? paths "performance.node-sync-interval"))
      (is (contains? paths "performance.matrix-gui-sync-interval"))
      (is (not (contains? paths "tick.matrix-validate-interval")))
      (is (not (contains? (set (map :key wireless-config/descriptors))
                          :matrix-validate-interval))))))

(deftest matrix-stats-formula-uses-wireless-config-test
  (testing "shared Matrix formula honors wireless config overrides"
    (with-redefs [matrix-stats/required-plate-count (constantly 3)
                  config-reg/get-config-values
                  (fn [domain]
                    (is (= config-common/wireless-domain domain))
                    {:matrix-capacity-per-core-level 10
                     :matrix-bandwidth-factor 20
                     :matrix-range-base 30.0})]
      (is (= {:capacity 40
              :bandwidth 320.0
              :range 60.0}
             (matrix-stats/matrix-stats-for-counts 4 3)))
      (is (= {:capacity 0 :bandwidth 0.0 :range 0.0}
             (matrix-stats/matrix-stats-for-counts 4 0))))))

(deftest matrix-core-tooltip-uses-wireless-config-test
  (testing "Matrix Core tooltip is derived from the shared config-backed Matrix formula"
    (with-redefs [config-reg/get-config-values
                  (fn [domain]
                    (is (= config-common/wireless-domain domain))
                    {:matrix-capacity-per-core-level 10
                     :matrix-bandwidth-factor 20
                     :matrix-range-base 30.0})]
      (is (= ["等级: 2"
              "容量倍率: 20"
              "带宽倍率: 80"
              "范围倍率: 42.4"]
             (mat-core/matrix-core-tooltip 2))))))