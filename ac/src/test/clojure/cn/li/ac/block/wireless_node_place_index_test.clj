(ns cn.li.ac.block.wireless-node-place-index-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.wireless.core.spatial-index :as si]
            [cn.li.ac.wireless.data.world :as world]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as ppos]))

(use-fixtures :each support-fw/with-fresh-framework)

(deftest node-place-registers-discoverable-spatial-index-test
  (let [world-id {:server-session-id :test-session
                  :world-id :w-node-index}
        tiles (atom {[0 0 0] (stubs/mutable-node {})})
        saved-state (atom nil)
        handler (node-logic/handle-node-place :basic)]
    (stubs/with-tile-world tiles
      (fn []
        (let [pos (ppos/create-block-pos 0 0 0)]
          (with-redefs [platform-be/get-custom-state (fn [_] nil)
                        platform-be/set-custom-state! (fn [_ state] (reset! saved-state state))
                        node-logic/player-name (fn [_] "tester")
                        uuid/player-uuid (fn [_] "tester-uuid")]
            (handler :player world-id pos :wireless-node-basic)
            (is (= "tester" (:placer-name @saved-state)))
            (is (= "tester-uuid" (:placer-uuid @saved-state)))
            (let [wd (world/get-world-data world-id)]
              (is (some? wd) "placing a node should ensure world-data exists")
              (is (contains? (si/positions-in-index (world-registry/spatial-index wd)
                                                    (si/nearby-chunk-keys 0 0 0 1))
                             [0 0 0])))))))))
