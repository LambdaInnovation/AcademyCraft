(ns cn.li.ac.content.ability.electromaster.mine-detect-config-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.content.ability :as ability-content]            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.config.registry :as config-reg]))

(defn- with-test-state
  [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (let [descriptors (config-reg/get-descriptor-registry)
            values (config-reg/get-value-registry)]
        (try
          (config-reg/set-descriptor-registry! {})
          (config-reg/set-value-registry! {})
          (store/reset-store!)
          (f)
          (finally
            (config-reg/set-descriptor-registry! descriptors)
            (config-reg/set-value-registry! values)
            (store/reset-store!)))))))

(defn- seed-electromaster-config!
  [values]
  (let [domain (skill-config/category-domain :electromaster)]
    (config-reg/register-config-descriptors!
      domain
      (get skill-config/descriptors-by-category :electromaster))
    (config-reg/ensure-default-values!
      domain
      (get skill-config/default-values-by-category :electromaster))
    (config-reg/set-config-values! domain values)))

(deftest mine-detect-public-spec-uses-upstream-level-and-tunables-test
  (testing "MineDetect exposes the upstream level and cost curve through the public skill spec"
    (with-test-state
      (fn []
        (ability-content/init-ability-content!)
        (seed-electromaster-config!
          {(skill-config/config-key :mine-detect :cost.down.cp) [1500.0 1000.0]
           (skill-config/config-key :mine-detect :cost.down.overload) [200.0 180.0]
           (skill-config/config-key :mine-detect :cooldown.ticks) [900.0 400.0]
           (skill-config/config-key :mine-detect :progression.exp-cast) 0.008})
        (let [spec (skill-registry/get-skill :mine-detect)
              player-id "mine-detect-config-player"]
          (is (= 3 (:level spec)))
          (is (true? (:controllable? spec)))
          (is (= 1500.0 ((get-in spec [:cost :down :cp]) {:player-id player-id})))
          (is (= 200.0 ((get-in spec [:cost :down :overload]) {:player-id player-id})))
          (is (= 899.0 ((:cooldown-ticks spec)
                       {:player-id player-id :skill-id :mine-detect :exp 0.001})))
          (is (= 900 (skill-config/lerp-int :mine-detect :cooldown.ticks 0.0)))
          (is (= 0.008 (skill-config/tunable-double :mine-detect :progression.exp-cast))))))))

