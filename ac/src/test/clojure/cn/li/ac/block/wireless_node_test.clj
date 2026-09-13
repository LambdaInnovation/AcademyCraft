(ns cn.li.ac.block.wireless-node-test
  "Unit tests for wireless node pure logic."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.block.wireless-node.gui-reactive :as node-gui]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.block.wireless-node.handlers :as node-handlers]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.wireless.config :as node-config]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.ac.wireless.core.capability-resolver :as resolver])
  (:import [cn.li.acapi.wireless IWirelessNode]))

(use-fixtures :each support-fw/with-fresh-framework)

(deftest node-types-source-of-truth-test
  (testing "node-types mirrors config contract"
    (is (= (node-config/node-types)
           (node-logic/node-types))))

  (testing "all required tiers exist with positive max-energy"
    (let [tiers (node-logic/node-types)]
      (doseq [tier [:basic :standard :advanced]]
        (is (contains? tiers tier))
        (is (pos? (get-in tiers [tier :max-energy])))))))

(deftest node-max-energy-test
  (testing "node-max-energy follows tier"
    (is (= (node-config/max-energy :basic)
           (node-logic/node-max-energy {:node-type :basic})))
    (is (= (node-config/max-energy :standard)
           (node-logic/node-max-energy {:node-type :standard})))
    (is (= (node-config/max-energy :advanced)
           (node-logic/node-max-energy {:node-type :advanced}))))

  (testing "missing tier falls back to basic"
    (is (= (node-config/max-energy :basic)
           (node-logic/node-max-energy {})))))

(deftest energy-to-blockstate-level-test
  (testing "energy maps to level range 0..4"
    (let [state {:node-type :basic}
          max-energy (double (node-logic/node-max-energy state))]
      (is (= 0 (node-logic/energy->blockstate-level 0.0 state)))
      (is (= 2 (node-logic/energy->blockstate-level (/ max-energy 2.0) state)))
      (is (= 4 (node-logic/energy->blockstate-level max-energy state)))
      (is (= 4 (node-logic/energy->blockstate-level (* max-energy 2.0) state))))))

(deftest inventory-namespace-owns-slot-metadata-test
  (testing "inventory namespace owns slot metadata"
    (node-logic/ensure-node-slot-schema!)
    (is (= 2 (node-logic/node-slot-count)))
    (is (= 0 (node-logic/node-input-slot-index)))
    (is (= 1 (node-logic/node-output-slot-index)))))

(deftest node-range-prefers-capability-range-test
  (let [cap (reify IWirelessNode
              (getEnergy [_] 0.0)
              (setEnergy [_ _] nil)
              (getMaxEnergy [_] 0.0)
              (getBandwidth [_] 0.0)
              (getCapacity [_] 0)
              (getRange [_] 42.5)
              (getNodeName [_] "")
              (getPassword [_] "")
              (getBlockPos [_] nil))]
    (testing "reads node range from resolved wireless capability"
      (with-redefs [resolver/node-capability (fn [_] cap)]
        (is (= 42.5 (node-handlers/node-range :tile)))))
    (testing "falls back to default range when capability is absent"
      (with-redefs [resolver/node-capability (fn [_] nil)]
        (is (= 20.0 (node-handlers/node-range :tile)))))))

(deftest owner-authorization-test
  (testing "blank or missing stored UUID allows initial edits"
    (with-redefs [uuid/player-uuid (fn [_] nil)]
      (is (true? (node-logic/owner-authorized? {:placer-uuid ""} :player)))
      (is (true? (node-logic/owner-authorized? {} :player)))))

  (testing "matching UUID grants access and mismatch denies"
    (with-redefs [uuid/player-uuid (fn [_] "player-uuid")]
      (is (true? (node-logic/owner-authorized? {:placer-uuid "player-uuid"} :player)))
      (is (false? (node-logic/owner-authorized? {:placer-uuid "other-uuid"} :player)))))

  (testing "stored UUID requires a player UUID"
    (with-redefs [uuid/player-uuid (fn [_] nil)]
      (is (false? (node-logic/owner-authorized? {:placer-uuid "player-uuid"} :player))))))

(deftest node-gui-slot-placement-policy-test
  (with-redefs [slot-schema/slot-type (fn [_ idx]
                                         (case idx
                                           0 :energy
                                           1 :output
                                           :unknown))
                energy/is-energy-item-supported? (fn [item] (= item :energy-item))]
    (testing "input slot accepts energy items only"
      (is (true? (node-gui/can-place-item? nil 0 :energy-item)))
      (is (false? (node-gui/can-place-item? nil 0 :ordinary-item))))

    (testing "output slot accepts energy items only"
      (is (true? (node-gui/can-place-item? nil 1 :energy-item)))
      (is (false? (node-gui/can-place-item? nil 1 :ordinary-item))))))
