(ns cn.li.mc1211.runtime.sync-core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcbase.runtime.sync-core :as sync-core]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(use-fixtures :each
  (fn [f]
    (sync-core/call-with-sync-scheduler-runtime
      (sync-core/create-sync-scheduler-runtime)
      f)))

(deftest duplicate-end-phase-does-not-send-twice-test
  (let [owner {:server-session-id :test-session}
        sent (atom [])]
    (with-redefs [runtime-hooks/build-sync-payload (fn [uuid _full?] {:uuid uuid})
                  runtime-hooks/mark-player-clean! (fn [_uuid] nil)]
      (sync-core/mark-player-dirty! owner "p1")
      (sync-core/tick-sync! (fn [uuid payload] (swap! sent conj [uuid payload]))
                            (assoc owner :server-tick-id 1))
      (sync-core/tick-sync! (fn [uuid payload] (swap! sent conj [uuid payload]))
                            (assoc owner :server-tick-id 1))
      (is (= [["p1" {:uuid "p1"}]] @sent)))))

(deftest sessions-flush-independently-test
  (let [sent (atom [])]
    (with-redefs [runtime-hooks/build-sync-payload (fn [uuid _full?] {:uuid uuid})
                  runtime-hooks/mark-player-clean! (fn [_uuid] nil)]
      (sync-core/mark-player-dirty! {:server-session-id :a} "pa")
      (sync-core/mark-player-dirty! {:server-session-id :b} "pb")
      (sync-core/tick-sync! (fn [uuid payload] (swap! sent conj [uuid payload]))
                            {:server-session-id :a :server-tick-id 1})
      (is (= [["pa" {:uuid "pa"}]] @sent))
      (is (contains? (get-in (sync-core/scheduler-snapshot) [:b :dirty-players]) "pb")))))

(deftest successful-send-clears-only-sent-player-test
  (let [owner {:server-session-id :test-session}
        sent (atom [])]
    (with-redefs [runtime-hooks/build-sync-payload (fn [uuid _full?]
                                                     (when (= uuid "online")
                                                       {:uuid uuid}))
                  runtime-hooks/mark-player-clean! (fn [_uuid] nil)]
      (sync-core/mark-player-dirty! owner "online")
      (sync-core/mark-player-dirty! owner "offline")
      (sync-core/tick-sync! (fn [uuid payload] (swap! sent conj [uuid payload]))
                            (assoc owner :server-tick-id 1))
      (is (= [["online" {:uuid "online"}]] @sent))
      (is (not (contains? (get-in (sync-core/scheduler-snapshot) [:test-session :dirty-players]) "online")))
      (is (contains? (get-in (sync-core/scheduler-snapshot) [:test-session :dirty-players]) "offline")))))

(deftest clean-players-are-never-periodically-sent-test
  (let [owner {:server-session-id :test-session}
        sent (atom [])]
    (with-redefs [runtime-hooks/build-sync-payload (fn [uuid _full?] {:uuid uuid})
                  runtime-hooks/mark-player-clean! (fn [_uuid] nil)
                  runtime-hooks/list-player-uuids (fn [] ["never-dirty"])]
      (doseq [server-tick-id (range 1 201)]
        (sync-core/tick-sync! (fn [uuid payload] (swap! sent conj [uuid payload]))
                              (assoc owner :server-tick-id server-tick-id)))
      (is (empty? @sent)))))

(deftest scheduler-owner-is-required-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :server-session-id"
                        (sync-core/mark-player-dirty! {} "p1")))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :server-session-id"
                        (sync-core/tick-sync! (fn [_uuid _payload]) {:server-tick-id 1}))))

(deftest clear-session-scheduler-state-removes-only-target-session-test
  (sync-core/mark-player-dirty! {:server-session-id :a} "pa")
  (sync-core/mark-player-dirty! {:server-session-id :b} "pb")
  (sync-core/clear-session-scheduler-state! :a)
  (is (nil? (get (sync-core/scheduler-snapshot) :a)))
  (is (contains? (get-in (sync-core/scheduler-snapshot) [:b :dirty-players]) "pb")))

(deftest scheduler-runtime-isolation-test
  (let [owner {:server-session-id :test-session}
        runtime-b (sync-core/create-sync-scheduler-runtime)]
    (sync-core/mark-player-dirty! owner "outer")
    (is (contains? (get-in (sync-core/scheduler-snapshot) [:test-session :dirty-players]) "outer"))
    (sync-core/call-with-sync-scheduler-runtime
      runtime-b
      (fn []
        (is (empty? (sync-core/scheduler-snapshot)))
        (sync-core/mark-player-dirty! owner "inner")
        (is (contains? (get-in (sync-core/scheduler-snapshot) [:test-session :dirty-players]) "inner"))))
    (is (contains? (get-in (sync-core/scheduler-snapshot) [:test-session :dirty-players]) "outer"))
    (is (not (contains? (get-in (sync-core/scheduler-snapshot) [:test-session :dirty-players]) "inner")))))
