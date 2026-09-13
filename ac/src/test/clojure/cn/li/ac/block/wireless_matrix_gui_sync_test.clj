(ns cn.li.ac.block.wireless-matrix-gui-sync-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.wireless-matrix.gui-reactive :as gui]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.block.wireless-matrix.logic :as matrix-logic]))

(deftest server-menu-sync-refreshes-tile-state-atoms-test
  (testing "server menu sync copies tile state into container atoms"
    (let [container (gui/create-container {:energy 0 :plate-count 2 :core-level 1 :is-working true} :player)]
      (with-redefs [common/get-tile-state (fn [_]
                                            {:plate-count 3
                                             :core-level 2
                                             :is-working false
                                             :bandwidth 40.0
                                             :range 24.0
                                             :capacity 12
                                             :max-capacity 20})]
        (gui/server-menu-sync! container)
        (is (= 3 @(:plate-count container)))
        (is (= 2 @(:core-level container)))
        (is (false? @(:is-working container)))
        (is (= 40 @(:bandwidth container)))))))
