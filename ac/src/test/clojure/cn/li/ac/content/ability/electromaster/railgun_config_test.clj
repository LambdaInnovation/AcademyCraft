(ns cn.li.ac.content.ability.electromaster.railgun-config-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
            [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.content.ability :as ability-content]
            [cn.li.ac.content.ability.electromaster.railgun :as railgun]
            [cn.li.ac.test.support.player-state :as ps-fix]
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

(deftest railgun-qte-down-cost-uses-action-tunable-curve-test
  (testing "railgun cost functions exposed through the public skill spec read action tunables"
    (with-test-state
      (fn []
        (ability-content/init-ability-content!)
        (let [player-id "railgun-config-test-player"]
          (seed-electromaster-config!
            {(skill-config/config-key :railgun :cost.down.cp) [1000.0 2000.0]
             (skill-config/config-key :railgun :cost.down.overload) [300.0 100.0]})
          (ps-fix/seed-player-state!
            player-id
            (-> (store/fresh-player-state)
                (assoc-in [:ability-data :skill-exps :railgun] 0.5)))
          (with-redefs [railgun/read-coin-qte-status (fn [_]
                                                        {:has-window? true
                                                         :active? true
                                                         :perform? true
                                                         :progress 0.75})]
            (let [spec (skill-registry/get-skill :railgun)
                  down-cp (get-in spec [:cost :down :cp])
                  down-overload (get-in spec [:cost :down :overload])]
              (is (fn? down-cp))
              (is (fn? down-overload))
              ;; The scaled cost wrapper reads exp off the ctx map — the
              ;; midpoint of each seeded curve.
              (is (= 1500.0 (down-cp {:player-id player-id :skill-id :railgun :exp 0.5})))
              (is (= 200.0 (down-overload {:player-id player-id :skill-id :railgun :exp 0.5}))))))))))


