(ns cn.li.ac.ability.service.radiation-mark-index-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.service.radiation-mark-index :as rad-index]
            [cn.li.ac.test.support.player-state :as ps-fix]))

;; ============================================================================
;; Pure fn: apply-source-marks
;; ============================================================================

(def ^:private mark-a {:source-player-id "atk-a" :target-id "v1" :ticks-left 100 :rate 1.5})
(def ^:private mark-b {:source-player-id "atk-b" :target-id "v1" :ticks-left 50 :rate 2.0})
(def ^:private mark-c {:source-player-id "atk-a" :target-id "v2" :ticks-left 40 :rate 1.5})

(deftest apply-source-marks-adds-fresh-source-test
  (let [idx (rad-index/apply-source-marks {} "atk-a" {"v1" mark-a})]
    (is (= {"v1" mark-a} (get-in idx [:by-source "atk-a"])))
    (is (= {"atk-a" mark-a} (get-in idx [:by-target "v1"])))))

(deftest apply-source-marks-full-replacement-drops-stale-targets-test
  (let [idx (rad-index/apply-source-marks {} "atk-a" {"v1" mark-a "v2" mark-c})
        idx2 (rad-index/apply-source-marks idx "atk-a" {"v2" mark-c})]
    (is (= {"v2" mark-c} (get-in idx2 [:by-source "atk-a"])))
    (is (nil? (get-in idx2 [:by-target "v1"])) "stale target entry must be fully removed, not left as {}")
    (is (= {"atk-a" mark-c} (get-in idx2 [:by-target "v2"])))))

(deftest apply-source-marks-cross-source-same-target-coexist-test
  (let [idx (-> {}
                (rad-index/apply-source-marks "atk-a" {"v1" mark-a})
                (rad-index/apply-source-marks "atk-b" {"v1" mark-b}))]
    (is (= {"atk-a" mark-a "atk-b" mark-b} (get-in idx [:by-target "v1"])))))

(deftest apply-source-marks-clearing-source-removes-empty-maps-entirely-test
  (let [idx (-> {}
                (rad-index/apply-source-marks "atk-a" {"v1" mark-a})
                (rad-index/apply-source-marks "atk-a" {}))]
    (is (nil? (:by-source idx)) "no empty-map residue after clearing the only source")
    (is (nil? (:by-target idx)) "no empty-map residue after clearing the only target")))

(deftest apply-source-marks-clearing-one-of-two-sources-preserves-other-test
  (let [idx (-> {}
                (rad-index/apply-source-marks "atk-a" {"v1" mark-a})
                (rad-index/apply-source-marks "atk-b" {"v1" mark-b})
                (rad-index/apply-source-marks "atk-a" {}))]
    (is (nil? (get-in idx [:by-source "atk-a"])))
    (is (= {"v1" mark-b} (get-in idx [:by-source "atk-b"])))
    (is (= {"atk-b" mark-b} (get-in idx [:by-target "v1"])) "atk-a's reverse entry removed, atk-b's kept")))

;; ============================================================================
;; Stateful API — needs a framework atom (fresh per test, see with-framework)
;; ============================================================================

(defn- with-clean-radiation-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (rad-index/reset-for-test!)
      (try (f)
           (finally (rad-index/reset-for-test!))))))

(use-fixtures :each with-clean-radiation-runtime)

(def ^:private sid ps-fix/test-session-id)

(deftest sync-source-marks-roundtrips-through-queries-test
  (rad-index/sync-source-marks! sid "atk-a" {"v1" mark-a})
  (is (= mark-a (rad-index/strongest-mark-for-target sid "v1")))
  (is (= '("atk-a") (rad-index/sources-for-target sid "v1")))
  (is (= '("atk-a") (rad-index/mark-holders sid))))

(deftest strongest-mark-for-target-picks-max-ticks-left-test
  (testing "deterministic: highest :ticks-left wins regardless of insertion order"
    (rad-index/sync-source-marks! sid "atk-a" {"v1" (assoc mark-a :ticks-left 30)})
    (rad-index/sync-source-marks! sid "atk-b" {"v1" (assoc mark-b :ticks-left 90)})
    (is (= 90 (:ticks-left (rad-index/strongest-mark-for-target sid "v1"))))
    (is (= "atk-b" (:source-player-id (rad-index/strongest-mark-for-target sid "v1"))))))

(deftest snapshot-by-target-reflects-all-targets-test
  (rad-index/sync-source-marks! sid "atk-a" {"v1" mark-a "v2" mark-c})
  (let [snap (rad-index/snapshot-by-target sid)]
    (is (= #{"v1" "v2"} (set (keys snap))))
    (is (= mark-a (get snap "v1")))
    (is (= mark-c (get snap "v2")))))

(deftest clear-session-drops-all-entries-for-session-only-test
  (rad-index/sync-source-marks! sid "atk-a" {"v1" mark-a})
  (rad-index/clear-session! sid)
  (is (nil? (rad-index/strongest-mark-for-target sid "v1")))
  (is (empty? (rad-index/mark-holders sid))))

(deftest sessions-are-isolated-test
  (rad-index/sync-source-marks! sid "atk-a" {"v1" mark-a})
  (rad-index/sync-source-marks! :other-session "atk-a" {"v1" mark-b})
  (is (= mark-a (rad-index/strongest-mark-for-target sid "v1")))
  (is (= mark-b (rad-index/strongest-mark-for-target :other-session "v1"))))

(deftest reset-for-test-clears-every-session-test
  (rad-index/sync-source-marks! sid "atk-a" {"v1" mark-a})
  (rad-index/reset-for-test!)
  (is (empty? (rad-index/mark-holders sid))))
