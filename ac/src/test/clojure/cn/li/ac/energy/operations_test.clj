(ns cn.li.ac.energy.operations-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.energy.operations :as op]
            [cn.li.ac.energy.service.item-manager :as item-manager]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.core.capability-lookup :as cap-lookup])
  (:import [cn.li.acapi.wireless IWirelessNode IWirelessReceiver WirelessCapabilityKeys]))

(deftest item-non-battery-path-test
  (with-redefs [item-manager/is-energy-item-supported? (constantly false)]
    (is (false? (op/is-energy-item-supported? :stk)))
    (is (= 0.0 (op/get-item-energy :stk)))
    (is (= 0.0 (op/get-item-max-energy :stk)))
    (is (= 0.0 (op/get-item-bandwidth :stk)))
    (is (nil? (op/set-item-energy! :stk 10)))
    (is (= 5.0 (op/charge-energy-to-item :stk 5.0 false)))
    (is (= 0.0 (op/pull-energy-from-item :stk 5.0 false)))))

(deftest item-battery-delegation-test
  (with-redefs [item-manager/is-energy-item-supported? (constantly true)
                item-manager/get-item-energy (fn [_] 12.0)
                item-manager/get-item-capacity (fn [_] 99.0)
                item-manager/get-item-bandwidth (fn [_] 7.0)
                item-manager/set-item-energy! (fn [_ e] [:set e])
                item-manager/charge-energy-to-item (fn [_ a ig] (- a 3))
                item-manager/pull-energy-from-item (fn [_ a ig] (min a 4.0))]
    (is (true? (op/is-energy-item-supported? :stk)))
    (is (= 12.0 (op/get-item-energy :stk)))
    (is (= 99.0 (op/get-item-max-energy :stk)))
    (is (= 7.0 (op/get-item-bandwidth :stk)))
    (is (= [:set 50.0] (op/set-item-energy! :stk 50.0)))
    (is (= 2.0 (op/charge-energy-to-item :stk 5.0 false)))
    (is (= 4.0 (op/pull-energy-from-item :stk 10.0 false)))))

(deftest node-energy-roundtrip-test
  (let [state (atom 100.0)
        node (reify IWirelessNode
               (getEnergy [_] @state)
               (setEnergy [_ e] (reset! state e))
               (getMaxEnergy [_] 1000.0)
               (getBandwidth [_] 50.0)
               (getCapacity [_] 4)
               (getRange [_] 8.0)
               (getNodeName [_] "n")
               (getPassword [_] "p")
               (getBlockPos [_] nil))]
    ;; Real block entities are the generic ScriptedBlockEntity — is-node-supported?
    ;; resolves the actual IWirelessNode impl via the capability registry
    ;; (cn.li.ac.wireless.core.capability-lookup/tile-capability), not instance?
    ;; on the raw tile. Mock that resolution here.
    (with-redefs [cap-lookup/tile-capability
                  (fn [tile cap-key]
                    (when (and (= tile node) (= cap-key WirelessCapabilityKeys/NODE))
                      node))]
      (is (true? (op/is-node-supported? node)))
      (is (false? (op/is-node-supported? :not-node)))
      (is (= 100.0 (op/get-node-energy node)))
      (op/set-node-energy! node 200.0)
      (is (= 200.0 (op/get-node-energy node)))
      (is (= 0.0 (op/charge-node node 10.0 false)))
      (is (= 210.0 (op/get-node-energy node)))
      (reset! state 80.0)
      (is (= 50.0 (op/pull-from-node node 50.0 false)))
      (is (= 30.0 (op/get-node-energy node)))
      (is (= 100.0 (op/charge-node :bad 100.0 false)))
      (is (= 0.0 (op/pull-from-node :bad 10.0 false))))))

(deftest receiver-path-test
  (let [rec (reify IWirelessReceiver
              (getRequiredEnergy [_] 0.0)
              (injectEnergy [_ a] (* a 0.5))
              (pullEnergy [_ a] (* a 2.0))
              (getReceiverBandwidth [_] 10.0))]
    (with-redefs [cap-lookup/tile-capability
                  (fn [tile cap-key]
                    (when (and (= tile rec) (= cap-key WirelessCapabilityKeys/RECEIVER))
                      rec))]
      (is (true? (op/is-receiver-supported? rec)))
      (is (false? (op/is-receiver-supported? :x)))
      (is (= 2.5 (op/charge-receiver rec 5.0)))
      (is (= 5.0 (op/pull-from-receiver rec 2.5)))
      (is (= 7.0 (op/charge-receiver :x 7.0)))
      (is (= 0.0 (op/pull-from-receiver :x 3.0))))))

(deftest wireless-stub-fallback-test
  (testing "get-wireless-network returns nil on helper exception"
    (with-redefs [wireless-api/get-wireless-net-by-node (fn [_] (throw (ex-info "no" {})))]
      (let [n (op/get-wireless-network :node "secret")]
        (is (nil? n)))))
  (testing "is-node-connected? returns false when helper throws"
    (with-redefs [wireless-api/is-node-linked? (fn [_] (throw (ex-info "no" {})))]
      (is (false? (op/is-node-connected? :n "pw")))
      (is (false? (op/is-node-connected? :n ""))))))
