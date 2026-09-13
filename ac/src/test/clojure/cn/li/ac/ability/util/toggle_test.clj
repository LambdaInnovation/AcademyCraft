(ns cn.li.ac.ability.util.toggle-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.util.toggle :as tg]))

(defn- framework-contexts-fixture [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (test-contexts/clean-contexts-fixture f))))

(use-fixtures :each framework-contexts-fixture)

(def ^:private test-context-owner
  {:logical-side :server :server-session-id :test-session :player-uuid "p"})

(deftest init-and-query-toggle-test
  (let [m (tg/init-toggle-state :my-skill)]
    (is (true? (:active m)))
    (is (= :my-skill (:skill-id m)))
    (is (= 0 (:start-tick m)))
    (is (= 0 (:total-ticks m))))
  (is (not (tg/is-toggle-active? {} :x)))
  (is (true? (tg/is-toggle-active? {:skill-state {:toggle {:x {:active true}}}} :x)))
  (is (false? (tg/is-toggle-active? {:skill-state {:toggle {:x {:active false}}}} :x)))
  (is (= {:active true} (tg/get-toggle-state {:skill-state {:toggle {:s {:active true}}}} :s))))

(deftest toggle-active-for-player-test
  (let [c1 (ctx/new-server-context "player-a" :skill "ctx-a" test-context-owner)
        c2 (ctx/new-server-context "player-b" :skill "ctx-b" (assoc test-context-owner :player-uuid "player-b"))]
    (ctx/register-context! c1)
    (ctx/register-context! c2)
    ;; Bind the routing owner on the thread — that is what the command runtime
    ;; and ctx/get-context read, not the framework atom.
    (ctx/with-context-owner-fn test-context-owner
      (fn []
        (tg/activate-toggle! "ctx-a" :storm-wing)
        (is (true? (tg/toggle-active-for-player? "player-a" :storm-wing)))
        (is (false? (tg/toggle-active-for-player? "player-b" :storm-wing)))
        (is (false? (tg/toggle-active-for-player? "player-a" :vec-reflection)))))))

(deftest toggle-mutations-via-context-test
  (let [c (ctx/new-server-context "p" :skill "ctx-toggle" test-context-owner)]
    (ctx/register-context! c)
    ;; Bind the routing owner on the thread — that is what the command runtime
    ;; and ctx/get-context read, not the framework atom.
    (ctx/with-context-owner-fn test-context-owner
      (fn []
        (tg/activate-toggle! "ctx-toggle" :s)
        (is (tg/is-toggle-active? (ctx/get-context test-context-owner "ctx-toggle") :s))
        (tg/update-toggle-tick! "ctx-toggle" :s)
        (is (= 1 (:total-ticks (tg/get-toggle-state (ctx/get-context test-context-owner "ctx-toggle") :s))))
        (tg/deactivate-toggle! "ctx-toggle" :s)
        (is (false? (:active (tg/get-toggle-state (ctx/get-context test-context-owner "ctx-toggle") :s))))
        (tg/remove-toggle! "ctx-toggle" :s)
        (is (nil? (tg/get-toggle-state (ctx/get-context test-context-owner "ctx-toggle") :s)))))))
