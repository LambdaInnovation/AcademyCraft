(ns cn.li.ac.ability.state.player-test
  (:require
            [cn.li.ac.ability.service.state-tick :as ps-tick]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.rules.cooldown-rules :as cooldown-rules]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.player-state :as ps-fix]            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(use-fixtures :each
  (fn [f]
    (ps-fix/with-test-player-state-owner
      (fn []
        (store/reset-store!)
        (evt/install-event-subscriber-runtime!
          (evt/create-event-subscriber-runtime))
        (try
          (f)
          (finally
            (store/reset-store!)
            (evt/install-event-subscriber-runtime!
              (evt/create-event-subscriber-runtime))))))))

(deftest player-state-access-requires-explicit-owner-test
  (runtime-hooks/with-client-ctx-fn {:player-owner nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"session-id"
                          (command-rt/run-command-in-session! nil
                                                              "ownerless"
                                                              {:command :change-category
                                                               :category-id :x})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"session-id"
                          (command-rt/run-command-in-session! nil
                                                              "ownerless"
                                                              {:command :change-category
                                                               :category-id :x}))))))

(deftest fresh-state-shape-test
  (let [s (store/fresh-player-state)]
    (is (map? (:ability-data s)))
    (is (map? (:resource-data s)))
    (is (map? (:cooldown-data s)))
    (is (map? (:preset-data s)))
    (is (map? (:develop-data s)))
    (is (= 0 (:max-stim (:develop-data s))))
    (is (map? (:context-registry s)))
    (is (false? (contains? s :dirty-domains)))
    (is (false? (contains? s :terminal-data)))))

(deftest get-or-create-player-state-test
  (let [a (store/get-or-create-player-state! ps-fix/test-session-id "u1")
        b (store/get-or-create-player-state! ps-fix/test-session-id "u1")]
    (is (identical? a b))))

(deftest update-ability-data-marks-dirty-test
  (store/get-or-create-player-state! ps-fix/test-session-id "u2")
  (is (zero? (store/dirty-mask ps-fix/test-session-id "u2")))
  (command-rt/run-command-in-session! ps-fix/test-session-id
                                      "u2"
                                      {:command :change-category
                                       :new-category :vecmanip})
  (is (seq (store/mask->domains (store/dirty-mask ps-fix/test-session-id "u2"))))
  (store/clear-dirty! ps-fix/test-session-id "u2")
  (is (zero? (store/dirty-mask ps-fix/test-session-id "u2"))))

(deftest update-ability-data-uses-bound-owner-session-test
  (store/get-or-create-player-state! :accessor-session "u2")
  (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id :accessor-session}} (fn [] (command-rt/run-command-in-session! nil
                                        "u2"
                                        {:command :change-category
                                         :new-category :vecmanip})
    (is (= :vecmanip
           (get-in (store/get-player-state :accessor-session "u2") [:ability-data :category-id]))))))

(defn- ticking-player-state
  "A player state with one live cooldown — server-tick-player-in-session!
   skips fully idle players (noop optimization), so smoke tests must seed
   live state."
  []
  (assoc (store/fresh-player-state)
         :cooldown-data (:data (cooldown-rules/set-cooldown {} :smoke 5))))

(deftest server-tick-player-smoke-test
  (store/set-player-state! ps-fix/test-session-id "u3" (ticking-player-state))
  (let [r (ps-tick/server-tick-player-in-session! ps-fix/test-session-id "u3" nil)]
    (is (map? r))
    (is (vector? (:events r)))))

(deftest server-tick-player-uses-explicit-session-test
  (store/set-player-state! :tick-session "u3" (ticking-player-state))
  (let [r (ps-tick/server-tick-player-in-session! :tick-session "u3" nil)]
    (is (map? r))
    (is (vector? (:events r)))))

(deftest remove-player-state-test
  (store/set-player-state! ps-fix/test-session-id "u4" (store/fresh-player-state))
  (is (some? (store/get-player-state ps-fix/test-session-id "u4")))
  (store/remove-player-state! ps-fix/test-session-id "u4")
  (is (nil? (store/get-player-state ps-fix/test-session-id "u4"))))

(deftest player-state-isolated-by-session-id-test
  (store/set-player-state! :session-a "same-uuid" (assoc (store/fresh-player-state) :marker :a))
  (store/set-player-state! :session-b "same-uuid" (assoc (store/fresh-player-state) :marker :b))
  (is (= :a (:marker (store/get-player-state :session-a "same-uuid"))))
  (is (= :b (:marker (store/get-player-state :session-b "same-uuid"))))
  (is (= ["same-uuid"] (vec (store/list-players :session-a)))))

(deftest clear-session-player-states-removes-only-target-session-test
  (store/set-player-state! :session-a "same-uuid" (assoc (store/fresh-player-state) :marker :a))
  (store/set-player-state! :session-a "only-a" (assoc (store/fresh-player-state) :marker :only-a))
  (store/set-player-state! :session-b "same-uuid" (assoc (store/fresh-player-state) :marker :b))
  (store/remove-session! :session-a)
  (is (nil? (store/get-player-state :session-a "same-uuid")))
  (is (nil? (store/get-player-state :session-a "only-a")))
  (is (= :b (:marker (store/get-player-state :session-b "same-uuid")))))

(deftest persisted-state-edn-roundtrip-keeps-core-data-test
  (let [state (-> (store/fresh-player-state)
                  (assoc-in [:ability-data :category-id] :electromaster)
                  (update-in [:ability-data :learned-skills] conj :railgun)
                  (assoc-in [:ability-data :skill-exps :railgun] 0.75)
                  (assoc-in [:resource-data :cur-cp] 42.0)
                  (assoc-in [:cooldown-data [:railgun :main]] 20)
                  (assoc-in [:preset-data :slots [0 0]] [:electromaster :railgun])
                  (assoc-in [:develop-data :state] :developing)
                  (assoc :dirty-domains #{:ability-data}))
        persisted (dissoc state :dirty-domains)
        decoded (edn/read-string (pr-str persisted))]
    (is (= persisted decoded))
    (is (false? (contains? decoded :dirty-domains)))
    (is (= #{:ability-data :resource-data :cooldown-data :preset-data :develop-data
             :cheats-data :context-registry}
           (set (keys decoded))))
    (is (false? (get-in decoded [:cheats-data :enabled?]))
        "the /aim cheat switch round-trips like any other persisted domain")
    (is (= [:electromaster :railgun]
           (get-in decoded [:preset-data :slots [0 0]])))))
