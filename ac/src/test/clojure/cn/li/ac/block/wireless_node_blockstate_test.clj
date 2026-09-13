(ns cn.li.ac.block.wireless-node-blockstate-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.wireless-node.blockstate :as node-blockstate]
            [cn.li.mcmod.config :as mcmod-config]))

(deftest node-definitions-use-exclusive-energy-connected-parts-test
  (with-redefs [mcmod-config/mod-id "academy"]
    (let [definition (node-blockstate/get-node-blockstate-definition :wireless-node-basic)
          parts (:parts definition)
          conditions (map :condition parts)]
      (testing "all parts are conditional on both energy and connected"
        (is (every? map? conditions))
        (is (every? #(contains? % :energy) conditions))
        (is (every? #(contains? % :connected) conditions)))
      (testing "generated states cover 5 energy levels x 2 connected flags"
        (is (= 10 (count parts)))
        (is (= #{"true" "false"}
               (set (map :connected conditions))))
        (is (= #{"0" "1" "2" "3" "4"}
               (set (map :energy conditions)))))
      (testing "connected=true parts use dedicated combined models"
        (let [connected-models (->> parts
                                    (filter #(= "true" (get-in % [:condition :connected])))
                                    (map (comp first :models))
                                    set)]
          (is (= #{"academy:block/node_basic_energy_0_connected"
                   "academy:block/node_basic_energy_1_connected"
                   "academy:block/node_basic_energy_2_connected"
                   "academy:block/node_basic_energy_3_connected"
                   "academy:block/node_basic_energy_4_connected"}
                 connected-models)))))))

(deftest node-model-texture-config-supports-combined-connected-variant-test
  (with-redefs [mcmod-config/mod-id "academy"]
    (let [cfg (node-blockstate/get-node-model-texture-config "node_basic_energy_3_connected")]
      (is (= "academy:block/node_basic_side_3" (:side cfg)))
      (is (= "academy:block/node_top_1" (:vert cfg))))))
