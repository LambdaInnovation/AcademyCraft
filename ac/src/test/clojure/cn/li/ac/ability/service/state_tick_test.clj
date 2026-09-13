(ns cn.li.ac.ability.service.state-tick-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.state-tick :as state-tick]
            [cn.li.ac.test.support.player-state :as ps-fix]))

(defn- with-fresh-state-tick-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (try
        (f)
        (finally
          (store/reset-store!))))))

(use-fixtures :each with-fresh-state-tick-runtime)

(deftest idle-player-dispatches-no-server-tick-command-test
  (testing "a fully idle player (CP/overload at rest, no cooldowns, not developing)
            never reaches command-runtime — zero commands, zero events"
    (ps-fix/seed-player-state! "p-idle" (store/fresh-player-state))
    (let [calls (atom 0)
          original command-rt/run-command-in-session!]
      (with-redefs [command-rt/run-command-in-session! (fn [& args]
                                                         (swap! calls inc)
                                                         (apply original args))]
        (let [result (state-tick/server-tick-player-in-session!
                      ps-fix/test-session-id "p-idle" nil)]
          (is (nil? result))
          (is (zero? @calls)))))))

(deftest non-idle-player-still-dispatches-server-tick-command-test
  (testing "a player recovering CP still gets ticked and progresses"
    (ps-fix/seed-player-state! "p-recovering"
                               (update (store/fresh-player-state) :resource-data
                                       #(assoc % :cur-cp (- (:max-cp %) 50.0))))
    (let [before (store/get-player-state ps-fix/test-session-id "p-recovering")
          result (state-tick/server-tick-player-in-session!
                  ps-fix/test-session-id "p-recovering" nil)]
      (is (some? result))
      (is (not= (:resource-data before) (:resource-data (:state result)))))))
