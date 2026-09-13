(ns cn.li.ac.achievement.dispatcher-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.achievement.dispatcher :as dispatch]
            [cn.li.ac.achievement.registry :as ach-reg]
            [cn.li.ac.achievement.trigger :as ach-trigger]))

(deftest trigger-custom-event-fires-mapped-achievements-test
  (testing "custom event fans out to achievements from registry query"
    (let [seen (atom [])]
      (with-redefs [ach-reg/find-by-trigger (fn [kind payload]
                                              (is (= :custom kind))
                                              (is (= {:event-id "teleporter.critical_attack"} payload))
                                              [:ach-a :ach-b])
                    ach-trigger/trigger-achievement! (fn [uuid ach-id]
                                                       (swap! seen conj [uuid ach-id]))]
        (dispatch/trigger-custom-event! "player-1" "teleporter.critical_attack")
        (is (= [["player-1" :ach-a] ["player-1" :ach-b]] @seen))))))
