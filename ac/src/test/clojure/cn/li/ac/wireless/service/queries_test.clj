(ns cn.li.ac.wireless.service.queries-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.wireless.config :as wireless-config]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.spatial-lookup :as spatial]
            [cn.li.ac.wireless.service.queries :as query]
            [cn.li.ac.wireless.data.world-registry :as world-registry]))

(deftest find-available-nodes-at-filters-range-and-capacity-test
  (let [world :world
        world-data :world-data
        ;; positions: dist-sq from origin = z^2
        available-pos [0 0 3]   ; dist 9  — in range, has room
        full-pos [0 0 2]        ; dist 4  — in range, at capacity
        far-pos [0 0 11]        ; dist 121 — beyond search range
        available-node (stubs/mutable-node {:range 10.0})
        full-node (stubs/mutable-node {:range 10.0})
        caps {available-pos available-node full-pos full-node}
        full-conn {:kind :full-conn}]
    (with-redefs [wireless-config/node-search-range (fn [] 10)
                  wireless-config/max-results (fn [] 10)
                  world-registry/get-world-data (fn [w]
                                                  (is (= world w))
                                                  world-data)
                  spatial/get-nearby-chunks (fn [x y z r]
                                              (is (= [0 0 0 10] [x y z r]))
                                              [:chunk])
                  spatial/get-positions-in-chunks (fn [wd chunks]
                                                    (is (= world-data wd))
                                                    (is (= [:chunk] chunks))
                                                    [available-pos full-pos far-pos])
                  resolver/resolve-node-cap (fn [_world node-vb]
                                              (get caps (vb/pos-of node-vb)))
                  lookup/get-node-connection (fn [_wd node-vb]
                                               (when (= full-pos (vb/pos-of node-vb))
                                                 full-conn))
                  node-conn/get-receivers (fn [conn]
                                            (is (= full-conn conn))
                                            [:r])
                  node-conn/get-generators (fn [conn]
                                             (is (= full-conn conn))
                                             [])
                  node-conn/get-capacity (fn [conn _world]
                                           (is (= full-conn conn))
                                           1)]
      (testing "available nodes are within search range, within node range, and not at capacity"
        (is (= [available-node]
               (vec (query/find-available-nodes-at world 0 0 0))))))))

(deftest find-available-nodes-at-respects-max-results-test
  (let [world :world
        node-a (stubs/mutable-node {:range 10.0})
        node-b (stubs/mutable-node {:range 10.0})]
    (with-redefs [wireless-config/node-search-range (fn [] 10)
                  wireless-config/max-results (fn [] 1)
                  world-registry/get-world-data (constantly :world-data)
                  spatial/get-nearby-chunks (constantly [:chunk])
                  spatial/get-positions-in-chunks (constantly [[0 0 1] [0 1 0]])
                  resolver/resolve-node-cap (fn [_ node-vb]
                                              (get {[0 0 1] node-a [0 1 0] node-b}
                                                   (vb/pos-of node-vb)))
                  lookup/get-node-connection (constantly nil)]
      (is (= 1 (count (query/find-available-nodes-at world 0 0 0)))))))
