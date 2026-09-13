(ns cn.li.ac.content.ability.reducer-backed-skill-state-test
  "Samples when skill-state paths go through reducer/command-runtime, not test-only ctx-skill stubs."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.effects.state :as state]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.test.support.skill-context :as skill-ctx]))

(defn- clean-fixture [f]
  (test-contexts/clean-contexts-fixture
    #(test-player/clean-player-states-fixture f)))

(use-fixtures :each clean-fixture)

(defn- runtime-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})

(deftest execute-assoc-state-paths-skill-state-via-reducer-test
  (testing "content skill paths should stay lean once ctx-skill only-handoffs stop assigning store paths"
    (let [ctx-id "ctx-reducer-backed"
          player-id "p-reducer"
          c (ctx/new-server-context player-id :mag-movement ctx-id (runtime-owner player-id))]
      (test-player/seed-player-state!
        player-id
        {:context-registry {ctx-id {:id ctx-id :skill-id :mag-movement :status :constructed}}})
      (ctx/register-context! c)
      (skill-ctx/with-context-owner (runtime-owner player-id)
        (fn []
          (state/execute-assoc-state! ctx-id player-id {:k [:charge-ticks] :v 3})
          (is (= 3 (get-in (ctx/get-context (runtime-owner player-id) ctx-id)
                           [:skill-state :charge-ticks])))
          (let [store-val (store/get-player-state test-player/test-session-id player-id)]
            (is (= 3 (get-in store-val [:context-registry ctx-id :skill-state :charge-ticks])))))))))

(deftest command-runtime-context-assoc-skill-state-test
  (testing "executing reducer command updates context-registry skill-state slice"
    (let [ctx-id "ctx-cmd-backed"
          player-id "p-cmd"
          c (ctx/new-server-context player-id :directed-blastwave ctx-id (runtime-owner player-id))]
      (test-player/seed-player-state!
        player-id
        {:context-registry {ctx-id {:id ctx-id :skill-id :directed-blastwave :status :constructed}}})
      (ctx/register-context! c)
      (skill-ctx/with-context-owner (runtime-owner player-id)
        (fn []
          (state/execute-assoc-state! ctx-id player-id {:k [:launched?] :v true})
          (is (true? (get-in (ctx/get-context (runtime-owner player-id) ctx-id)
                             [:skill-state :launched?])))
          (is (true? (get-in (store/get-player-state test-player/test-session-id player-id)
                             [:context-registry ctx-id :skill-state :launched?]))))))))
