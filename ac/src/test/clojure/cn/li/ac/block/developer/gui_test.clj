(ns cn.li.ac.block.developer.gui-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.block.developer.gui-reactive :as developer-gui-reactive]
            [cn.li.ac.block.developer.logic :as developer-logic]
            [cn.li.ac.block.developer.panel-reactive :as panel-reactive]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.test.support.framework :refer [with-fresh-framework]]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.gui.spec :as gui-reg]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- with-player-state-owner
  [player-uuid f]
  (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id :test-session
                                                  :player-uuid player-uuid}} (fn [] (f))))

(deftest developer-open-gui-forwards-to-platform-gui-test
  (let [calls (atom [])]
    (with-redefs [cn.li.ac.gui.open/open-gui-by-type
                  (fn [& args] (swap! calls conj args))
                  bdsl/get-block-spec (fn [_] nil)
                  world/client-side? (fn [_] true)]
      ;; make-open-gui-handler-with-predicate handlers take positional args
      ;; (player world pos block-id & {:keys [sneaking item-stack]}).
      ((developer-logic/open-developer-gui-for "developer-normal")
       :player-1 :client-world [10 20 30] "developer-normal" :sneaking false)
      (is (= [[:player-1
               :developer
               :client-world
               [10 20 30]]]
             @calls)))))

(deftest developer-gui-init-registers-once-test
  (with-fresh-framework
    (fn []
      (with-redefs [gui-reg/register-block-gui! (fn [& _] nil)]
        ;; init-developer-reactive! is guarded by install/framework-once! —
        ;; calling it repeatedly across the test suite is safe and a no-op after
        ;; the first successful registration.
        (developer-gui-reactive/init-developer-reactive!)
        (developer-gui-reactive/init-developer-reactive!)
        (is true)))))

(deftest developer-on-close-clears-user-session-state-test
  (let [saved (atom nil)]
    (with-redefs [world/client-side? (fn [_] false)
                  entity/player-get-level (fn [_] :server-level)
                  platform-be/get-custom-state (fn [_] {:user-uuid "u1"
                                                        :user-name "Player"
                                                        :is-developing true})
                  platform-be/set-custom-state! (fn [_ st] (reset! saved st))
                  platform-be/set-changed! (fn [_] nil)]
      (developer-gui-reactive/on-close {:tile-entity :tile-1
                                        :player :player-1})
      (is (= "" (:user-uuid @saved)))
      (is (= "" (:user-name @saved)))
      (is (false? (:is-developing @saved))))))

(deftest right-panel-mode-no-category-returns-console-test
  "When player has no category, right panel mode should be :console."
  (with-fresh-framework   ;; magnetic-coil check probes entity-ops availability
    (fn []
      (with-redefs [store/get-player-state
                    (fn [_ _]{:ability-data {:category-id nil}})
                    uuid/player-uuid (fn [_] "player-uuid")]
        (with-player-state-owner
          "player-uuid"
          (fn []
            (let [mode (panel-reactive/right-panel-mode nil nil :player)]
              (is (= :console mode)))))))))

(deftest right-panel-mode-with-category-returns-skill-tree-test
  "When player has a category, right panel mode should be :skill-tree."
  (with-fresh-framework
    (fn []
      (with-redefs [store/get-player-state
                    (fn [_ _]{:ability-data {:category-id :electromaster}})
                    uuid/player-uuid (fn [_] "player-uuid")]
        (with-player-state-owner
          "player-uuid"
          (fn []
            (let [mode (panel-reactive/right-panel-mode nil nil :player)]
              (is (= :skill-tree mode)))))))))
