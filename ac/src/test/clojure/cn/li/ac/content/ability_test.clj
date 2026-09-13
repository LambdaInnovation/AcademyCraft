(ns cn.li.ac.content.ability-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.discovery :as discovery]
            [cn.li.ac.ability.item-actions :as item-actions]
            [cn.li.ac.ability.passive :as passive]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.server.damage.handler :as damage-handler]
            [cn.li.ac.ability.server.damage.runtime :as damage-runtime]
            [cn.li.ac.ability.spi-lifecycle :as lifecycle]
            [cn.li.mcmod.util.log]
            [cn.li.ac.content.ability :as ability-content]
            [cn.li.ac.test.support.framework :as support-fw]))

(use-fixtures :each support-fw/with-fresh-framework)

(deftest init-ability-content-freezes-registries-once-test
  (let [freeze-calls* (atom [])]
    (let [one-pass [:discovery
                    :category
                    :skill
                    :item-actions
                    :attack-checks
                    :damage-handlers
                    :passive
                    :lifecycle]
          two-pass (vec (concat one-pass one-pass))]
                (with-redefs [discovery/discovered-skill-namespaces (fn [] [])
                      discovery/freeze-provider-discovery! (fn []
                                    (swap! freeze-calls* conj :discovery)
                                    nil)
                      category/register-category! (fn [_] nil)
                      item-actions/register-item-action! (fn [& _] nil)
                      category/freeze-category-registry! (fn []
                                  (swap! freeze-calls* conj :category)
                                  nil)
                      skill-registry/freeze-skill-registry! (fn []
                                  (swap! freeze-calls* conj :skill)
                                  nil)
                      item-actions/freeze-item-action-registries! (fn []
                                     (swap! freeze-calls* conj :item-actions)
                                     nil)
                      damage-handler/freeze-attack-check-registries! (fn []
                                        (swap! freeze-calls* conj :attack-checks)
                                        nil)
                      damage-runtime/freeze-damage-handler-registry! (fn []
                                        (swap! freeze-calls* conj :damage-handlers)
                                        nil)
                      passive/freeze-passive-handler-registry! (fn []
                                     (swap! freeze-calls* conj :passive)
                                     nil)
                      lifecycle/freeze-lifecycle-registry! (fn []
                                     (swap! freeze-calls* conj :lifecycle)
                                     nil)
                      cn.li.mcmod.util.log/info (fn [& _] nil)]
                  (ability-content/init-ability-content!)
                  (ability-content/init-ability-content!)
                  (is (contains? #{one-pass two-pass} @freeze-calls*))))))