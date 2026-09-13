(ns cn.li.ac.block.wireless-node-gui-sync-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.wireless-node.gui-reactive :as node-gui]
            [cn.li.ac.block.wireless-node.logic :as node-logic]))

(deftest create-container-normalizes-node-default-state-test
  (let [container (node-gui/create-container node-logic/node-default-state :player)]
    (is (= :node (:container-type container)))
    (is (= :basic @(:node-type container)))
    (is (= 15000 @(:max-energy container)))
    (is (= 0 @(:tab-index container)))))
