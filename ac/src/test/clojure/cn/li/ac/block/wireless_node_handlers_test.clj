(ns cn.li.ac.block.wireless-node-handlers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.wireless-node.handlers :as handlers]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.ac.test.support.gui-payload :as gui-payload]
            [cn.li.ac.test.support.network :as network-support]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.ac.wireless.feedback :as feedback]))

(def ^:private payload (gui-payload/machine-payload 1))

(deftest owner-authorization-guards-node-mutations-test
  (testing "non-owner requests are rejected"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  platform-be/get-custom-state (fn [_] {:placer-uuid "owner-uuid"
                                                         :placer-name "owner"})
                  uuid/player-uuid (fn [_] "not-owner-uuid")]
      (let [result (handlers/handle-change-name (assoc payload :node-name "new") :player)]
        (is (false? (:success result)))
        (is (seq (:messages result))))))

  (testing "owner requests run mutation handler"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  platform-be/get-custom-state (fn [_] {:placer-uuid "owner-uuid"
                                                         :placer-name "owner"})
                  uuid/player-uuid (fn [_] "owner-uuid")]
      (let [result (handlers/handle-change-name (assoc payload :node-name "new-name") :player)]
        (is (:success result))
        (is (seq (:messages result)))))))

(deftest change-password-owner-guard-test
  (testing "blank owner allows mutation"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  platform-be/get-custom-state (fn [_] {:placer-uuid ""})
                  uuid/player-uuid (fn [_] "anyone-uuid")]
      (let [result (handlers/handle-change-password (assoc payload :password "x") :player)]
        (is (:success result))
        (is (seq (:messages result))))))

  (testing "missing tile is rejected"
    (with-redefs [machine-handlers/open-container-tile (constantly nil)
                  uuid/player-uuid (fn [_] "owner-uuid")]
      (let [result (handlers/handle-change-password (assoc payload :password "x") :player)]
        (is (false? (:success result)))
        (is (seq (:messages result)))))))
