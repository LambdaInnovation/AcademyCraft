(ns cn.li.ac.ability.integration.external-providers-test
  "End-to-end test of the third-party ability ServiceLoader path: the real
  cn.li.ac.ability.integration.fixture.FixtureAbilityProvider is discovered
  via META-INF/services (src/test/resources) exactly as a real third-party
  mod's provider would be."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.integration.external-providers :as external-providers]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.mcmod.hooks.core :as runtime-hooks])
  (:import [cn.li.ac.ability.integration.fixture FixtureAbilityProvider]))

(def ^:private fixture-skill-id :fixture-provider-test-strike)

(defn- reset-fixture [f]
  (test-player/with-framework
    (fn []
      (skill-registry/reset-skill-registry-for-test!)
      (.set FixtureAbilityProvider/PERFORM_COUNT 0)
      (try
        (f)
        (finally
          (skill-registry/reset-skill-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(deftest external-skill-is-discovered-and-registered-test
  (external-providers/load-external-providers!)
  (let [spec (skill-registry/get-skill fixture-skill-id)]
    (is (some? spec) "fixture skill should be registered")
    (is (= :electromaster (:category-id spec)))
    (is (= 2 (:level spec)))
    (is (true? (:controllable? spec)))
    (is (= :instant (:pattern spec)))
    (is (true? (:external? spec)))
    (is (fn? (get-in spec [:actions :perform!])))))

(deftest external-skill-perform-action-runs-through-whitelisted-context-test
  (external-providers/load-external-providers!)
  (let [spec (skill-registry/get-skill fixture-skill-id)
        perform! (get-in spec [:actions :perform!])
        player-uuid (str (java.util.UUID/randomUUID))]
    (store/set-player-state! test-player/test-session-id player-uuid (store/fresh-player-state))
    (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id test-player/test-session-id
                                                   :player-uuid player-uuid}} (fn [] (perform! "ctx-1" player-uuid fixture-skill-id 0.5 true 0 :down nil)))
    (is (= 1 (.get FixtureAbilityProvider/PERFORM_COUNT))
        "the Java ActionHandler ran")
    (let [state (store/get-player-state test-player/test-session-id player-uuid)]
      (is (= {:ticks 20 :max 20}
             (get-in state [:cooldown-data [:fixture-provider-test-strike :main]]))
          "setMainCooldown reached the reducer through the whitelisted context"))))

(deftest external-skill-reregistration-is-idempotent-after-freeze-test
  ;; Re-running discovery (e.g. a second bootstrap attempt) must not fail on
  ;; the *same* skill id/spec, even once frozen — this is what makes
  ;; init-ability-content! safe to call more than once.
  (external-providers/load-external-providers!)
  (skill-registry/freeze-skill-registry!)
  (external-providers/load-external-providers!)
  (is (some? (skill-registry/get-skill fixture-skill-id))))

(deftest new-skill-registration-rejected-after-freeze-test
  (external-providers/load-external-providers!)
  (skill-registry/freeze-skill-registry!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"frozen"
                        (skill-registry/register-skill!
                         {:id :some-other-external-skill
                          :category-id :electromaster
                          :level 1
                          :controllable? false
                          :name-key "fixture.skill.other"
                          :ctrl-id :some-other-external-skill
                          :pattern :passive
                          :external? true}))))
