(ns cn.li.ac.content.ability.electromaster.thunder-bolt-config-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.content.ability :as ability-content]
            [cn.li.ac.content.ability.electromaster.thunder-bolt]
            [cn.li.mcmod.config.registry :as config-reg]))

(defn- with-test-state [f]
  (let [descriptors (config-reg/get-descriptor-registry)
        values (config-reg/get-value-registry)]
    (try
      (config-reg/set-descriptor-registry! {})
      (config-reg/set-value-registry! {})
      (f)
      (finally
        (config-reg/set-descriptor-registry! descriptors)
        (config-reg/set-value-registry! values)))))

(defn- seed-electromaster-config! [values]
  (let [domain (skill-config/category-domain :electromaster)]
    (config-reg/register-config-descriptors!
      domain
      (get skill-config/descriptors-by-category :electromaster))
    (config-reg/ensure-default-values!
      domain
      (get skill-config/default-values-by-category :electromaster))
    (config-reg/set-config-values! domain values)))

(deftest thunder-bolt-public-spec-uses-tunables-test
  (testing "ThunderBolt public skill spec should consume runtime tunables"
    (with-test-state
      (fn []
        (ability-content/init-ability-content!)
        (seed-electromaster-config!
          {(skill-config/config-key :thunder-bolt :cost.down.cp) [101.0 220.0]
           (skill-config/config-key :thunder-bolt :cost.down.overload) [10.0 40.0]
           (skill-config/config-key :thunder-bolt :cooldown.ticks) [91.0 30.0]})
        (let [spec (skill-registry/get-skill :thunder-bolt)
              cp-fn (get-in spec [:cost :down :cp])
              overload-fn (get-in spec [:cost :down :overload])
              cooldown-fn (:cooldown-ticks spec)]
          (is (fn? cp-fn))
          (is (fn? overload-fn))
          (is (fn? cooldown-fn))
          (is (= 160.0 (double (cp-fn {:exp 0.5})))
              "original truncates fractional CP cost")
          (is (= 25.0 (overload-fn {:exp 0.5})))
          (is (= 60.0 (double (cooldown-fn {:exp 0.5})))
              "original Float#toInt truncates cooldown instead of rounding"))))))
