(ns cn.li.ac.block.wireless-matrix-owner-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.wireless-matrix.logic :as logic]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world :as world]))

(deftest owner-authorized-check
  (with-redefs [uuid/player-uuid (fn [_] "11111111-1111-1111-1111-111111111111")]
    (testing "UUID match grants access"
      (is (true? (logic/owner-authorized?
                  {:placer-uuid "11111111-1111-1111-1111-111111111111"
                   :placer-name "any"}
                  :player))))

    (testing "UUID mismatch denies access"
      (is (false? (logic/owner-authorized?
                   {:placer-uuid "00000000-0000-0000-0000-000000000000"
                    :placer-name "any"}
                   :player)))))

  (with-redefs [uuid/player-uuid (fn [_] nil)]
    (testing "blank stored UUID means unowned initialization"
      (is (true? (logic/owner-authorized?
                  {:placer-uuid ""}
                  :player))))

    (testing "missing stored UUID means unowned initialization"
      (is (true? (logic/owner-authorized?
                  {:placer-name "alice"}
                  :player))))

    (testing "stored UUID requires a player UUID"
      (is (false? (logic/owner-authorized?
                   {:placer-uuid "11111111-1111-1111-1111-111111111111"
                    :placer-name "alice"}
                   :player))))))

(deftest placer-name-from-state
  (is (= "Dev" (logic/placer-name {:placer-name "Dev"})))
  (is (= "" (logic/placer-name {})))
  (is (= "" (logic/placer-name nil))))

(deftest placer-uuid-from-state
  (is (= "abc-123" (logic/placer-uuid {:placer-uuid "abc-123"})))
  (is (nil? (logic/placer-uuid {})))
  (is (nil? (logic/placer-uuid nil))))

(deftest handle-matrix-place-persists-uuid-and-canonical-name
  (let [saved-state (atom nil)
        handler (logic/handle-matrix-place)]
    (with-redefs [world/get-tile-entity (fn [_ _] :be)
                  platform-be/get-custom-state (fn [_] {:foo 1})
                  platform-be/set-custom-state! (fn [_ state] (reset! saved-state state))
                  uuid/player-uuid (fn [_] "test-uuid-123")
                  entity/player-get-name (fn [_] "alice")]
      (handler :player :world :pos :wireless-matrix)
      (is (= "test-uuid-123" (:placer-uuid @saved-state)))
      (is (= "alice" (:placer-name @saved-state))))))
