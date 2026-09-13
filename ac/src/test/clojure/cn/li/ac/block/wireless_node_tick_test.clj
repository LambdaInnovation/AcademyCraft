(ns cn.li.ac.block.wireless-node-tick-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.wireless.config :as node-config]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.world :as world]))

;; :update-ticker stopped being a state key in "performance fix" — the ticker is
;; now a private mutable field on MachineState, and the sync-interval network
;; check is the observable consequence of it advancing.
(deftest node-tick-state-advances-the-machine-ticker-test
  (let [state (machine-runtime/ensure-machine-state {} node-logic/node-default-state)
        checks* (atom 0)]
    (with-redefs [node-logic/tick-charge-in identity
                  node-logic/tick-charge-out identity
                  node-logic/tick-check-network (fn [s _ _ _] (swap! checks* inc) s)]
      (dotimes [_ (node-config/sync-interval)]
        (node-logic/node-tick-state state :w :p nil :be))
      (is (= 1 @checks*) "network check runs once per sync interval")
      (dotimes [_ (node-config/sync-interval)]
        (node-logic/node-tick-state state :w :p nil :be))
      (is (= 2 @checks*)))))

(deftest node-scripted-tick-fn-uses-machine-wrapper-test
  (testing "tick fn commits via machine runtime on server"
    (let [saved (atom nil)
          blockstate-calls (atom 0)
          be {:id "wireless-node-basic"}]
      (with-redefs [world/client-side? (fn [_] false)
                    platform-be/get-block-id (fn [_] "wireless-node-basic")
                    platform-be/get-custom-state (fn [_] nil)
                    node-logic/tick-charge-in (fn [s] s)
                    node-logic/tick-charge-out (fn [s] s)
                    node-logic/tick-check-network (fn [s _ _ _] s)
                    node-logic/update-block-state!
                    (fn [_ _ _] (swap! blockstate-calls inc))
                    platform-be/set-custom-state! (fn [_ st] (reset! saved st))
                    platform-be/set-changed! (fn [_] nil)]
        (node-logic/node-scripted-tick-fn :level :pos nil be)
        (is (some? @saved))
        (is (machine-runtime/machine-state? @saved)
            "the wrapper commits the machine state itself")
        (is (= :basic (:node-type @saved)) "node fields survive the commit"))))

(deftest sync-blockstate-compares-broadcast-metadata-test
  (testing "after-commit updates blockstate when energy level changes on sync tick"
    (let [blockstate-calls (atom 0)
          old {:update-ticker (node-config/sync-interval)
               :node-type :basic
               :energy 0.0
               :enabled false}
          new (assoc old :energy (node-logic/node-max-energy old))]
      (with-redefs [node-logic/update-block-state!
                    (fn [_ _ _] (swap! blockstate-calls inc))]
        (#'node-logic/sync-blockstate-if-changed! nil :level :pos old new)
        (is (= 1 @blockstate-calls)))))))
