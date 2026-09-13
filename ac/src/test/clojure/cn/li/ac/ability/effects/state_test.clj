(ns cn.li.ac.ability.effects.state-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.ability.effects.state :as state]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]))

(use-fixtures :each
  (fn [f]
    (test-contexts/clean-contexts-fixture
     #(ps-fix/clean-player-states-fixture f))))

(def ^:private test-context-owner
  {:logical-side :server :server-session-id :test-session :player-uuid "p"})

(deftest assoc-state-op-test
  (let [c (ctx/new-server-context "p" :sk "ctx-st" test-context-owner)]
    (ctx/register-context! c)
    (ctx/with-context-owner test-context-owner
      (state/execute-assoc-state! "ctx-st" "p" {:k :foo :v 42})
      (is (= 42 (get-in (ctx/get-context "ctx-st") [:skill-state :foo]))))))

(deftest charge-tick-op-test
  (let [c (ctx/new-server-context "p" :sk "ctx-ch" test-context-owner)]
    (ctx/register-context! c)
    (ctx/with-context-owner test-context-owner
      (let [out (state/execute-charge-tick! "ctx-ch" "p" {:k :ticks :max 5})]
        (is (= 1 out))
        (is (= 1 (get-in (ctx/get-context "ctx-ch") [:skill-state :ticks])))))))

(deftest overload-floor-op-test
  (store/get-or-create-player-state! ps-fix/test-session-id "of-p")
  (ctx/with-context-owner test-context-owner
    (state/execute-overload-floor! "ctx-of" "of-p" :sk 0.0 {:floor 10.0}))
  (is (<= 10.0 (get-in (store/get-player-state ps-fix/test-session-id "of-p")
                       [:resource-data :cur-overload]))))
