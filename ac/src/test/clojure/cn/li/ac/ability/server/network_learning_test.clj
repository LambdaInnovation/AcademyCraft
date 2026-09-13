(ns cn.li.ac.ability.server.network-learning-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.server.network :as network]
            [cn.li.ac.ability.rules.learning-rules :as learning-rules]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.ac.ability.util.uuid :as uuid]))

(use-fixtures :each test-player/clean-player-states-fixture)

(deftest handle-learn-skill-request-delegates-to-command-runtime-test
  (let [calls* (atom [])
        player :stub-player]
    (store/set-player-state! test-player/test-session-id "p1" {:ability-data (assoc (adata/new-ability-data) :level 3)})
    ;; Payload carries no pos-x/y/z (portable path), so the station/tile branch
    ;; is skipped — the player handle is only read by uuid + player-get-level.
    (with-redefs [uuid/player-uuid (constantly "p1")
                  entity/player-get-level (constantly nil)
                  skill/get-skill (fn [skill-id]
                                    {:id skill-id :level 1 :developer-type :normal :prerequisites []})
                  learning-rules/check-all-conditions (fn [skill-spec ability-data player-level developer-type]
                                                        (swap! calls* conj [:conditions (:id skill-spec) player-level developer-type ability-data])
                                                        {:pass? true :failures []})
                  command-rt/run-command-in-session! (fn [session-id uuid {:keys [skill-id]}]
                                            (swap! calls* conj [:learn session-id uuid skill-id])
                                            {:state {:ability-data (adata/learn-skill (get-in (store/get-player-state session-id uuid) [:ability-data]) skill-id)}
                                             :events [{:event/type :ability/skill-learn
                                                       :uuid uuid
                                                       :skill-id skill-id}]
                                             :effects []})]
                                          (#'network/handle-learn-skill-request {:skill-id :arc-gen} player)
      (is (= [[:conditions :arc-gen 3 :normal (get-in (store/get-player-state test-player/test-session-id "p1") [:ability-data])]
              [:learn test-player/test-session-id "p1" :arc-gen]]
             @calls*)))))
