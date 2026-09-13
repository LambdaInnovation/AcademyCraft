(ns cn.li.ac.ability.server.cooldown-policy-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]            [cn.li.ac.ability.model.cooldown :as cd]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill :as skill-reg]
            [cn.li.ac.ability.service.context-state :as rt]))

(defn- reset-test-state! [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture f)))

(use-fixtures :each reset-test-state!)

(defn- test-context-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})

(defn- seed-player!
  ([uuid]
   (seed-player! uuid (cd/new-cooldown-data)))
  ([uuid cooldown-data]
  (store/set-player-state! test-player/test-session-id
                            uuid
                            {:ability-data {:skill-exps {:arc-gen 0.0}}
                             :cooldown-data cooldown-data})))

(defn- active-context!
  [uuid ctx-id]
  (ctx/register-context!
    (assoc (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid))
          :input-state :active)))

(deftest default-key-up-applies-main-cooldown-with-max-existing-rule-test
  (let [uuid "cooldown-player-default"
        ctx-id "ctx-cooldown-default"
        existing (cd/set-cooldown (cd/new-cooldown-data) :arc-gen :main 20)]
    (seed-player! uuid existing)
    (active-context! uuid ctx-id)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :ctrl-id :arc-gen
                                         :cooldown {:mode :default}
                                         :cooldown-ticks 7})
                  evt/fire-ability-event! (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-up! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})))
        (is (= 20 (cd/get-remaining (:cooldown-data (store/get-player-state test-player/test-session-id uuid)) :arc-gen :main)))
        (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context ctx-id))))))))

(defn- cooldown-after-key-up!
  "Run a default key-up for `spec` with the player's arc-gen exp at `exp`, and
  return the main cooldown that landed."
  [uuid ctx-id spec exp]
  (store/set-player-state! test-player/test-session-id uuid
                           {:ability-data {:skill-exps {:arc-gen exp}}
                            :cooldown-data (cd/new-cooldown-data)})
  (active-context! uuid ctx-id)
  (with-redefs [skill-reg/get-skill (fn [_] spec)
                evt/fire-ability-event! (fn [_] nil)]
    (ctx/with-context-owner (test-context-owner uuid)
      (rt/handle-key-up! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})
      (cd/get-remaining (:cooldown-data (store/get-player-state test-player/test-session-id uuid))
                        :arc-gen :main))))

(deftest key-up-honours-cooldown-policy-ticks-test
  ;; This path used to read :cooldown-ticks alone, so a skill declaring its
  ;; duration under :cooldown-policy got a one-tick cooldown while the pattern
  ;; path (skill-effects/apply-cooldown!) gave it the declared 200.
  (is (= 200 (cooldown-after-key-up! "cooldown-player-policy" "ctx-cooldown-policy"
                                     {:id :arc-gen :ctrl-id :arc-gen
                                      :cooldown {:mode :default}
                                      :cooldown-policy {:ticks 200}}
                                     0.0))))

(deftest key-up-resolves-exp-scaled-cooldown-functions-test
  ;; Most skills declare :cooldown-ticks as a fn of exp; this path used to call
  ;; (int fn) on it. The exp comes from the player's own skill exp, the same
  ;; value the pattern path passes through its event.
  (let [spec {:id :arc-gen :ctrl-id :arc-gen
              :cooldown {:mode :default}
              :cooldown-ticks (fn [_player-id _skill-id exp] (* 120 (- 1.0 exp)))}]
    (is (= 120 (cooldown-after-key-up! "cooldown-player-fn-0" "ctx-cooldown-fn-0" spec 0.0)))
    (is (= 30 (cooldown-after-key-up! "cooldown-player-fn-75" "ctx-cooldown-fn-75" spec 0.75)))))

(deftest manual-key-up-does-not-apply-automatic-main-cooldown-test
  (let [uuid "cooldown-player-manual"
        ctx-id "ctx-cooldown-manual"]
    (seed-player! uuid)
    (active-context! uuid ctx-id)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :ctrl-id :arc-gen
                                         :cooldown {:mode :manual}
                                         :cooldown-ticks 9})
                  evt/fire-ability-event! (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-up! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})))
        (is (= 0 (cd/get-remaining (:cooldown-data (store/get-player-state test-player/test-session-id uuid)) :arc-gen :main)))
        (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context ctx-id))))))))

