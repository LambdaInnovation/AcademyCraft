(ns cn.li.ac.content.ability.meltdowner.damage-helper-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.server.damage.runtime :as rt]
            [cn.li.ac.ability.service.player-runtime-commands :as prt-cmd]
            [cn.li.ac.ability.service.radiation-mark-index :as rad-index]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as dh]
            [cn.li.ac.content.ability.meltdowner.rad-intensify :as rad]))

(defn- with-fresh-marks-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (try
        (f)
        (finally
          (dh/reset-marks-for-test!)
          (store/reset-store!))))))

(use-fixtures :each with-fresh-marks-runtime)

(defn- learned-rad-intensify-data []
  (-> (ad/new-ability-data) (ad/learn-skill :rad-intensify)))

(defn- learn-rad-intensify! [player-id]
  (ps-fix/seed-player-state! player-id {:ability-data (learned-rad-intensify-data)}))

(deftest process-damage-without-mark-is-unchanged-test
  (testing "no rad mark leaves damage unchanged (other handlers may still run)"
    (dh/ensure-damage-handler!)
    (let [victim (str (random-uuid))
          out (rt/process-damage! victim "attacker-1" 42.0 :test-source)]
      (is (= 42.0 (double out))))))

(deftest marked-target-gets-multiplied-damage-test
  (testing "mark-target! installs target mark rate until ticks expire"
    (let [attacker "atk-1"
          victim (str (random-uuid))]
      (dh/ensure-damage-handler!)
      (learn-rad-intensify! attacker)
      (with-redefs [rad/rate (fn [_sid] 1.75)
                    rad/mark-duration-ticks (fn [] 120)]
        (dh/mark-target! attacker victim)
        (is (= 1.75 (:rate (get (dh/marks-snapshot) victim))))
        (is (= 140.0 (double (rt/process-damage! victim attacker 80.0 :magic))))))))

(deftest marked-target-does-not-require-matching-attacker-test
  (testing "target mark amplifies damage from any attacker while active"
    (let [attacker-a "atk-owner"
          attacker-b "atk-other"
          victim (str (random-uuid))]
      (dh/ensure-damage-handler!)
      (learn-rad-intensify! attacker-a)
      (with-redefs [rad/rate (fn [_sid] 1.75)
                    rad/mark-duration-ticks (fn [] 100)]
        (dh/mark-target! attacker-a victim)
        (is (= 140.0 (double (rt/process-damage! victim attacker-b 80.0 :magic))))
        (is (= 140.0 (double (rt/process-damage! victim attacker-a 80.0 :magic))))))))

(deftest marked-target-applies-without-attacker-test
  (testing "nil attacker still amplifies because mark belongs to target"
    (let [attacker "atk-owner"
          victim (str (random-uuid))]
      (dh/ensure-damage-handler!)
      (learn-rad-intensify! attacker)
      (with-redefs [rad/rate (fn [_sid] 1.5)
                    rad/mark-duration-ticks (fn [] 100)]
        (dh/mark-target! attacker victim)
        (is (= 15.0 (double (rt/process-damage! victim nil 10.0 :magic))))))))

(deftest mark-refresh-overwrites-rate-and-ticks-test
  (let [attacker-a "atk-a"
        attacker-b "atk-b"
        victim "victim-1"]
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! attacker-a)
    (learn-rad-intensify! attacker-b)
    (with-redefs [rad/rate (fn [sid]
                             (case sid
                               "atk-a" 1.5
                               "atk-b" 2.0
                               1.0))
                  rad/mark-duration-ticks (fn [] 100)]
      (dh/mark-target! attacker-a victim)
      (is (= 100 (:ticks-left (get (dh/marks-snapshot) victim))))
      (dh/tick-marks!)
      (is (= 99 (:ticks-left (get (dh/marks-snapshot) victim))))
      (dh/mark-target! attacker-b victim)
      (is (= 100 (:ticks-left (get (dh/marks-snapshot) victim))))
      (is (= 2.0 (:rate (get (dh/marks-snapshot) victim)))))))

(deftest mark-duration-inherits-attacker-mark-not-victim-mark-test
  (let [attacker "atk-inherited"
        victim "victim-existing"
        old-owner "atk-old"]
    (learn-rad-intensify! attacker)
    (dh/reset-marks-for-test!
      {attacker {:source-player-id old-owner
                 :target-id attacker
                 :ticks-left 80
                 :rate 1.4}
       victim {:source-player-id old-owner
               :target-id victim
               :ticks-left 120
               :rate 1.4}})
    (with-redefs [rad/rate (fn [_] 1.8)
                  rad/mark-duration-ticks (fn [] 60)]
      (dh/mark-target! attacker victim)
      (is (= {:source-player-id attacker
              :target-id victim
              :ticks-left 80
              :rate 1.8}
             (dissoc (get (dh/marks-snapshot) victim) :updated-at-tick))))))

(deftest expired-mark-cleanup-by-ticks-test
  (let [victim-a "victim-a"
        victim-b "victim-b"]
    (dh/ensure-damage-handler!)
    (dh/reset-marks-for-test!
      {victim-a {:source-player-id "atk-a"
                 :target-id victim-a
                 :ticks-left 0
                 :rate 3.0}
       victim-b {:source-player-id "atk-b"
                 :target-id victim-b
                 :ticks-left 2
                 :rate 2.0}})
    (is (= 10.0 (double (rt/process-damage! victim-a "atk-a" 10.0 :magic))))
    (is (nil? (get (dh/marks-snapshot) victim-a)))
    (is (= 20.0 (double (rt/process-damage! victim-b "atk-z" 10.0 :magic))))
    (dh/tick-marks!)
    (is (= 1 (:ticks-left (get (dh/marks-snapshot) victim-b))))
    (dh/tick-marks!)
    (is (nil? (get (dh/marks-snapshot) victim-b)))))

(deftest marks-snapshot-reset-and-clear-test
  (let [target-a "victim-1"
        target-b "victim-2"]
    (dh/reset-marks-for-test!
      {target-a {:source-player-id "atk-a" :target-id target-a :ticks-left 5 :rate 2.0}
       target-b {:source-player-id "atk-a" :target-id target-b :ticks-left 5 :rate 4.0}})
    (is (= 2.0 (:rate (get (dh/marks-snapshot) target-a))))
    (dh/clear-target-mark! target-a)
    (is (nil? (get (dh/marks-snapshot) target-a)))
    (is (contains? (dh/marks-snapshot) target-b))
    (dh/clear-source-marks! "atk-a")
    (is (empty? (dh/marks-snapshot)))))

(deftest damage-helper-marks-are-per-attacker-state-test
  (let [target-a "victim-a"
        target-b "victim-b"]
    (dh/reset-marks-for-test!
      {target-a {:source-player-id "atk-a"
                 :target-id target-a
                 :ticks-left 2
                 :rate 2.0}
       target-b {:source-player-id "atk-b"
                 :target-id target-b
                 :ticks-left 4
                 :rate 3.0}})
    (dh/tick-marks!)
    (is (= 1 (:ticks-left (get (dh/marks-snapshot) target-a))))
    (is (= 3 (:ticks-left (get (dh/marks-snapshot) target-b))))))

;; ============================================================================
;; Radiation-mark index: O(N) elimination regression coverage
;; ============================================================================

;; NOTE: run-for-player!'s 2-arity delegates to its own 3-arity via the Var
;; (not a direct self-call), so a with-redefs stub must route straight to
;; `original`'s 3-arity — routing through original's 2-arity would re-enter
;; the (redefined) Var and double-count every call.

(deftest tick-marks-zero-marks-emits-zero-commands-test
  (testing "server with no active marks emits zero :tick-radiation-marks commands"
    (let [calls (atom 0)
          original prt-cmd/run-for-player!]
      (with-redefs [prt-cmd/run-for-player!
                    (fn
                      ([uuid cmd] (swap! calls inc) (original uuid cmd {}))
                      ([uuid cmd opts] (swap! calls inc) (original uuid cmd opts)))]
        (dh/tick-marks!))
      (is (zero? @calls)))))

(deftest tick-marks-only-notifies-mark-holders-test
  (testing "only players who hold a mark receive the tick command"
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! "atk-a")
    (with-redefs [rad/rate (fn [_] 1.5)
                  rad/mark-duration-ticks (fn [] 100)]
      (dh/mark-target! "atk-a" "victim-a"))
    (let [calls (atom [])
          original prt-cmd/run-for-player!]
      (with-redefs [prt-cmd/run-for-player!
                    (fn
                      ([uuid cmd]
                       (swap! calls conj [uuid (:command cmd)])
                       (original uuid cmd {}))
                      ([uuid cmd opts]
                       (swap! calls conj [uuid (:command cmd)])
                       (original uuid cmd opts)))]
        (dh/tick-marks!))
      (is (= [["atk-a" :tick-radiation-marks]] @calls)))))

(deftest tick-marks-drains-index-and-authoritative-state-together-test
  (testing "the derived index and the authoritative per-player state expire in lockstep"
    (dh/reset-marks-for-test!
      {"victim-a" {:source-player-id "atk-a" :target-id "victim-a" :ticks-left 2 :rate 1.5}})
    (is (= 2 (:ticks-left (prt-cmd/radiation-marks-for-target "victim-a"))))
    (is (= 2 (:ticks-left (get-in (store/get-player-state ps-fix/test-session-id "atk-a")
                                  [:runtime :meltdowner :radiation-marks "victim-a"]))))
    (dh/tick-marks!)
    (is (= 1 (:ticks-left (prt-cmd/radiation-marks-for-target "victim-a"))))
    (dh/tick-marks!)
    (is (nil? (prt-cmd/radiation-marks-for-target "victim-a")))
    (is (empty? (get-in (store/get-player-state ps-fix/test-session-id "atk-a")
                        [:runtime :meltdowner :radiation-marks])))))

(deftest tick-marks-reaps-ghost-index-entries-without-resurrecting-state-test
  (testing "an index entry whose backing player state is already gone is dropped,
            never resurrected via get-or-create"
    (rad-index/sync-source-marks! ps-fix/test-session-id "ghost-atk"
                                  {"victim-g" {:source-player-id "ghost-atk"
                                              :target-id "victim-g"
                                              :ticks-left 50
                                              :rate 1.5}})
    (is (nil? (store/get-player-state ps-fix/test-session-id "ghost-atk")))
    (dh/tick-marks!)
    (is (nil? (store/get-player-state ps-fix/test-session-id "ghost-atk")))
    (is (nil? (prt-cmd/radiation-marks-for-target "victim-g")))))

(deftest clear-mark-only-targets-actual-mark-sources-test
  (testing "clear-mark! no longer broadcasts to every online player"
    (dh/reset-marks-for-test!
      {"victim-a" {:source-player-id "atk-a" :target-id "victim-a" :ticks-left 50 :rate 1.5}})
    (let [calls (atom [])
          original prt-cmd/run-for-player!]
      (with-redefs [prt-cmd/run-for-player!
                    (fn
                      ([uuid cmd] (swap! calls conj uuid) (original uuid cmd {}))
                      ([uuid cmd opts] (swap! calls conj uuid) (original uuid cmd opts)))]
        (dh/clear-mark! "victim-a"))
      (is (= ["atk-a"] @calls))
      (is (nil? (prt-cmd/radiation-marks-for-target "victim-a"))))))

(deftest hydrate-player-state-runtime-data-syncs-radiation-index-test
  (testing "runtime-data hydration (e.g. NBT load) resyncs the derived index"
    (let [runtime-data {:meltdowner {:radiation-marks
                                     {"victim-h" {:source-player-id "atk-h"
                                                  :target-id "victim-h"
                                                  :ticks-left 30
                                                  :rate 1.2}}}}]
      (prt-cmd/run-for-player! "atk-h" {:command :hydrate-player-state
                                        :runtime-data runtime-data})
      (is (= 30 (:ticks-left (prt-cmd/radiation-marks-for-target "victim-h")))))))
