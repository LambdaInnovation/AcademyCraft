(ns cn.li.ac.ability.client.fx-spec-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]))

(defn- reset-fixture [f]
  (fx-registry/reset-fx-registry-for-test!)
  (level-effects/reset-level-effect-registry-for-test!)
  (hand-effects/reset-hand-effect-registry-for-test!)
  (try
    (f)
    (finally
      (fx-registry/reset-fx-registry-for-test!)
      (level-effects/reset-level-effect-registry-for-test!)
      (hand-effects/reset-hand-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(deftest default-owner-key-prefers-effect-instance-test
  (is (= [:effect-instance "inst-1"]
         (fx-spec/default-owner-key "ctx-1" {:effect-instance-id "inst-1"})))
  (is (= [:ctx "ctx-1"]
         (fx-spec/default-owner-key "ctx-1" {})))
  (is (= [:source-player "p1"]
         (fx-spec/default-owner-key nil {:source-player-id "p1"}))))

(deftest register-binds-level-hand-and-channel-handlers-test
  (let [registered-topics* (atom #{})
        level* (atom nil)
        hand* (atom nil)]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! level* [effect-id effect-map]))
                  hand-effects/register-hand-effect! (fn [effect-id effect-map]
                                                       (reset! hand* [effect-id effect-map]))
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                       (swap! registered-topics* conj topic))]
      (fx-spec/register!
        {:id :test-effect
         :level {:initial-state {}}
         :hand {:initial-state {}}
         :channels {:start {:topic :test/fx-start :mode :start :targets [:level :hand]}
                    :end {:topic :test/fx-end :mode :end :targets [:hand]}}})
      (is (= :test-effect (first @level*)))
      (is (= :test-effect (first @hand*)))
      (is (= #{:test/fx-start :test/fx-end} @registered-topics*)))))

(deftest register-dispatches-to-level-and-hand-targets-test
  (let [enqueued-level* (atom [])
        enqueued-hand* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  hand-effects/register-hand-effect! (fn [& _] nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued-level* conj (into [effect-id ctx-id channel payload] opts)))
                  hand-effects/enqueue-hand-effect! (fn [effect-id ctx-id channel payload & opts]
                                                      (swap! enqueued-hand* conj (into [effect-id ctx-id channel payload] opts)))]
      (fx-spec/register!
        {:id :test-effect
         :channels {:start {:topic :test/fx-start :mode :start :targets [:level :hand]}
                    :end {:topic :test/fx-end :mode :end :targets [:hand]}}})
      (fx-registry/dispatch-fx-channel! "ctx-a" :test/fx-start {:ticks 3})
      (fx-registry/dispatch-fx-channel! "ctx-a" :test/fx-end {:performed? true})
      (is (= 1 (count @enqueued-level*)))
      (is (= 2 (count @enqueued-hand*)))
      (is (= [:test-effect "ctx-a" :test/fx-start {:mode :start :ticks 3} :owner-key [:ctx "ctx-a"]]
             (first @enqueued-hand*))))))
