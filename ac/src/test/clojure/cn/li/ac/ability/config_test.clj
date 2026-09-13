(ns cn.li.ac.ability.config-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]))

(defn- reset-ability-config-fixture [f]
  (try
    (cfg/init-config!)
    (config-reg/set-config-values! config-common/ability-domain cfg/default-values)
    (f)
    (finally
      (config-reg/set-config-values! config-common/ability-domain cfg/default-values))))

(use-fixtures :each reset-ability-config-fixture)

(deftest default-values-contract-test
  (testing "default-values mirrors descriptor defaults"
    (is (= (set (map :key cfg/descriptors))
           (set (keys cfg/default-values))))
    (doseq [{:keys [key default]} cfg/descriptors]
      (is (= default (get cfg/default-values key))))))

(deftest max-level-is-structural-test
  (testing "max level is an internal structural constant, not a player descriptor"
    (is (= cfg/expected-level-count (cfg/max-level)))
    (is (not (contains? (set (keys cfg/default-values)) :max-level)))
    (is (not (contains? (set (map :key cfg/descriptors)) :max-level)))))

(deftest runtime-config-override-and-fallback-test
  (testing "runtime getters use registry overrides"
    (config-reg/set-config-values!
     config-common/ability-domain
     {:damage-scale 2.5
      :attack-player false
      :destroy-blocks false
      :worlds-whitelisted-destroying-blocks ["minecraft:the_nether" "-1"]
      :normal-metal-blocks ["custom:metal"]
      :weak-metal-blocks ["custom:weak-metal"]
      :metal-entities ["custom:metal-entity"]
      :init-cp [10.0 11.0 12.0 13.0 14.0]
      :add-cp [100.0 101.0 102.0 103.0 104.0]
      :cp-recovery-rate-base 0.01
      :cp-recovery-lerp-start 0.7
      :cp-recovery-lerp-end 1.7
      :overload-recovery-min-rate 0.03
      :overload-recovery-active-rate 0.04
      :overload-recovery-lerp-start 0.8
      :overload-recovery-lerp-end 0.2
      :overload-recovery-ratio-divisor 3.0
      :max-overload-growth-per-event 7.0
      :reflected-damage-multiplier 0.25
      :reflection-search-radius 24.0
      :runtime-cp-consume-per-tick 3.0
      :runtime-overload-per-tick 4.0
      :level-threshold-skill-count-multiplier 2.0
      :level-threshold-all-mastered-discount 0.25
      :skill-learning-cost-base 4.0
      :skill-learning-cost-level-square-factor 0.75
      :level-up-stim-base 6})
    (is (= 2.5 (cfg/damage-scale)))
    (is (false? (cfg/attack-player-enabled?)))
    (is (false? (cfg/destroy-blocks-enabled?)))
    (is (= ["minecraft:the_nether" "-1"] (cfg/whitelisted-destroying-worlds)))
    (is (true? (cfg/is-normal-metal-block? "custom:metal")))
    (is (true? (cfg/is-weak-metal-block? "custom:weak-metal")))
    (is (true? (cfg/is-metal-entity? "custom:metal-entity")))
    (is (= 12.0 (cfg/get-init-cp 3)))
    (is (= 102.0 (cfg/get-add-cp 3)))
    ;; The level grant only. Adding get-add-cp (102) here as well would
    ;; double-count the growth allowance that :add-max-cp earns separately.
    (is (= 12.0 (cfg/base-max-cp-for-level 3)))
    ;; :init-overload is not overridden above, so this reads the shipped table
    ;; — which mirrors upstream default.conf init_overload[3].
    (is (= 240.0 (cfg/base-max-overload-for-level 3)))
    (is (= 0.01 (cfg/cp-recovery-rate-base)))
    (is (= 0.7 (cfg/cp-recovery-lerp-start)))
    (is (= 1.7 (cfg/cp-recovery-lerp-end)))
    (is (= 0.03 (cfg/overload-recovery-min-rate)))
    (is (= 0.04 (cfg/overload-recovery-active-rate)))
    (is (= 0.8 (cfg/overload-recovery-lerp-start)))
    (is (= 0.2 (cfg/overload-recovery-lerp-end)))
    (is (= 3.0 (cfg/overload-recovery-ratio-divisor)))
    (is (= 7.0 (cfg/max-overload-growth-per-event)))
    (is (= 0.25 (cfg/reflected-damage-multiplier)))
    (is (= 24.0 (cfg/reflection-search-radius)))
    (is (= 3.0 (cfg/runtime-cp-consume-per-tick)))
    (is (= 4.0 (cfg/runtime-overload-per-tick)))
    (is (= 2.0 (cfg/level-threshold-skill-count-multiplier)))
    (is (= 0.25 (cfg/level-threshold-all-mastered-discount)))
    (is (= 4.0 (cfg/skill-learning-cost-base)))
    (is (= 0.75 (cfg/skill-learning-cost-level-square-factor)))
    (is (= 6 (cfg/level-up-stim-base))))
  (testing "invalid level lists fall back to defaults"
    (config-reg/set-config-values!
     config-common/ability-domain
     {:init-cp [1.0 2.0]
      :add-cp [1.0 2.0 3.0 4.0 5.0]})
    (is (= 1800.0 (cfg/get-init-cp 1)))
    (is (= 3.0 (cfg/get-add-cp 3)))))

(deftest validation-contract-test
  (testing "valid defaults pass"
    (is (nil? (cfg/validate-config!))))
  (testing "invalid values fail validation"
    (config-reg/set-config-values!
     config-common/ability-domain
     {:init-cp [1.0 2.0]
      :normal-metal-blocks [1]
      :cp-recover-speed 0.0
      :damage-scale -1.0
      :runtime-cp-consume-per-tick -1.0
      :overload-recovery-ratio-divisor 0.0
      :level-up-stim-base 0
      :skill-learning-cost-base -1.0})
    (try
      (cfg/validate-config!)
      (is false)
      (catch clojure.lang.ExceptionInfo e
        (let [errors (:errors (ex-data e))]
          (is (some #(= % "init-cp must be a non-negative numeric list with 5 elements") errors))
          (is (some #(= % "normal-metal-blocks must be a string list") errors))
          (is (some #(= % "cp-recover-speed must be positive") errors))
          (is (some #(= % "damage-scale must be positive") errors))
          (is (some #(= % "runtime-cp-consume-per-tick must be non-negative") errors))
          (is (some #(= % "overload-recovery-ratio-divisor must be positive") errors))
          (is (some #(= % "level-up-stim-base must be positive") errors))
          (is (some #(= % "skill-learning-cost-base must be non-negative") errors)))))))