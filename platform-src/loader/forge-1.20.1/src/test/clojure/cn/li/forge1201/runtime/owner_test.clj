(ns cn.li.forge1201.runtime.owner-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.forge1201.runtime.owner :as forge-owner]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest with-player-owner-binds-server-owner-test
  (let [owner {:logical-side :server
               :server-session-id :test-server-session
               :player-uuid "player-1"}
        seen (atom nil)]
    (with-redefs [forge-owner/owner-for-player (fn [player side]
                                                 (is (= :player player))
                                                 (is (= :server side))
                                                 owner)]
      (forge-owner/with-player-owner
        :player
        :server
        #(reset! seen (runtime-hooks/player-state-owner))))
    (is (= owner @seen))))
