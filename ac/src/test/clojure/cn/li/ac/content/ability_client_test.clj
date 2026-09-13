(ns cn.li.ac.content.ability-client-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.keybinds :as keybinds]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability-client :as ability-client]
            [cn.li.ac.test.support.framework :as support-fw]))

(use-fixtures :each support-fw/with-fresh-framework)

(deftest init-client-fx-loads-discovered-namespaces-once-test
  (let [inited* (atom [])
        freeze-calls* (atom [])]
    (with-redefs [cn.li.ac.ability.discovery/discovered-fx-namespaces
                  (fn []
                    '[cn.li.ac.content.ability.electromaster.arc-gen-fx
                      cn.li.ac.content.ability.electromaster.thunder-bolt-fx])
                  cn.li.ac.content.ability-client/init-fx-namespace!
                  (fn [ns-sym]
                    (swap! inited* conj ns-sym)
                    nil)
                  fx-registry/freeze-fx-registry! (fn []
                                                   (swap! freeze-calls* conj :fx)
                                                   nil)
                  keybinds/freeze-keybind-registries! (fn []
                                                        (swap! freeze-calls* conj :keybinds)
                                                        nil)
                  level-effects/freeze-level-effect-registry! (fn []
                                                                 (swap! freeze-calls* conj :level)
                                                                 nil)
                  hand-effects/freeze-hand-effect-registry! (fn []
                                                               (swap! freeze-calls* conj :hand)
                                                               nil)
                  cn.li.mcmod.util.log/info (fn [& _] nil)]
      (ability-client/init-client-fx!)
      (ability-client/init-client-fx!)
      (is (= '[cn.li.ac.content.ability.electromaster.arc-gen-fx
               cn.li.ac.content.ability.electromaster.thunder-bolt-fx]
             @inited*))
      (is (= [:fx :keybinds :level :hand]
             @freeze-calls*)))))
