(ns cn.li.ac.terminal.client.apps.skill-tree-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.terminal.client.apps.skill-tree :as skill-tree]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(deftest open-forwards-learn-context-test
  (testing "explicit learn-context is forwarded to the platform bridge"
    (let [calls (atom [])]
      (with-redefs [uuid/player-uuid (fn [_] "player-uuid-1")
                    entity/player-get-name (fn [_] "Tester")
                    client-bridge/open-screen!
                    (fn [screen-id payload]
                      (swap! calls conj {:screen-id screen-id
                                         :payload payload}))]
        (skill-tree/open! :player-1 {:developer-type :portable})
        (skill-tree/open! :player-2)
        (is (= [{:screen-id :ac/skill-tree
                 :payload {:player-uuid "player-uuid-1"
                           :learn-context {:developer-type :portable}}}
                {:screen-id :ac/skill-tree
                 :payload {:player-uuid "player-uuid-1"
                           :learn-context nil}}]
               @calls))))))
