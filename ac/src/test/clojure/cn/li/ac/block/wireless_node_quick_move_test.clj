(ns cn.li.ac.block.wireless-node-quick-move-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.block.wireless-node.gui-reactive :as node-gui]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.wireless.gui.container.move :as move-common]
            [cn.li.ac.wireless.gui.container.common :as common]))

(use-fixtures :each (fn [f]
                      (support-fw/with-fresh-framework
                        (fn []
                          (node-logic/ensure-node-slot-schema!)
                          (f)))))

(defn- make-container
  [slots]
  {:tile-entity {:inventory (atom slots)}})

(deftest quick-move-container-to-player-test
  (testing "shift-click from node slot removes item from container"
    (let [container (make-container {0 :energy-item 1 nil 100 nil})]
      (with-redefs [common/get-slot-item (fn [c idx] (get @(get-in c [:tile-entity :inventory]) idx))
                    common/set-slot-item! (fn [c idx item]
                                            (swap! (get-in c [:tile-entity :inventory]) assoc idx item))
                    energy/is-energy-item-supported? (constantly true)]
        (is (= :energy-item (#'node-gui/quickly-move container 0 100)))
        (is (nil? (get @(get-in container [:tile-entity :inventory]) 0)))))))

(deftest quick-move-player-to-node-test
  (testing "unsupported items stay in player inventory"
    (let [container (make-container {0 nil 1 nil 100 :other-item})]
      (with-redefs [common/get-slot-item (fn [c idx] (get @(get-in c [:tile-entity :inventory]) idx))
                    common/set-slot-item! (fn [c idx item]
                                            (swap! (get-in c [:tile-entity :inventory]) assoc idx item))
                    energy/is-energy-item-supported? (fn [item] (= item :energy-item))]
        (is (= :other-item (#'node-gui/quickly-move container 100 100)))
        (is (= :other-item (get @(get-in container [:tile-entity :inventory]) 100)))
        (is (nil? (get @(get-in container [:tile-entity :inventory]) 0)))))))

(deftest quick-move-stack-delegation-test
  (testing "node quick-move delegates to shared move engine with generated config"
    (let [called (atom nil)
          container (make-container {0 nil 1 nil 100 :energy-item})]
      (with-redefs [move-common/quick-move-with-rules
                    (fn [c slot-index player-inventory-start cfg]
                      (reset! called {:container c
                                      :slot-index slot-index
                                      :player-inventory-start player-inventory-start
                                      :cfg cfg})
                      :moved)]
        (is (= :moved (#'node-gui/quickly-move container 100 100)))
        (is (= 100 (:slot-index @called)))
        (is (= 100 (:player-inventory-start @called)))
        (is (fn? (get-in @called [:cfg :inventory-pred])))
        (is (seq (get-in @called [:cfg :rules])))))))
