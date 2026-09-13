(ns cn.li.ac.ability.service.command-runtime-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.reducer :as reducer]
            [cn.li.ac.ability.effects.interpreter :as interpreter]
            [cn.li.ac.test.support.player-state :as player-state-support]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(def ^:private baseline-state
  {:ability-data {}
   :resource-data {}
   :cooldown-data {}
   :preset-data {}
   :context-registry {}
   :dirty? false})

(deftest run-command-in-session-injects-session-id-test
  (testing "explicit session id is propagated into reducer command payload"
    (let [captured-command (atom nil)]
      (with-redefs [store/get-or-create-player-state! (fn [_session-id _uuid] baseline-state)
                    store/set-player-state! (fn [_session-id _uuid _state] nil)
                    store/mark-player-dirty! (fn [_session-id _uuid] nil)
                    reducer/apply-command (fn [state command]
                                            (reset! captured-command command)
                                            {:state state :events [] :effects []})
                    interpreter/execute-reducer-result! (fn [_result] nil)]
        (command-rt/run-command-in-session! "session-A" "player-A"
                                            {:command :level-up})
        (is (= "session-A" (:session-id @captured-command)))
        (is (= "player-A" (:player-uuid @captured-command)))
        (is (= :level-up (:command @captured-command)))
        (is (nil? (:command-id @captured-command)))))))

(deftest run-command-in-session-preserves-command-id-test
  (testing "existing command-id is preserved during normalization"
    (let [captured-command (atom nil)]
      (with-redefs [store/get-or-create-player-state! (fn [_session-id _uuid] baseline-state)
                    store/set-player-state! (fn [_session-id _uuid _state] nil)
                    store/mark-player-dirty! (fn [_session-id _uuid] nil)
                    reducer/apply-command (fn [state command]
                                            (reset! captured-command command)
                                            {:state state :events [] :effects []})
                    interpreter/execute-reducer-result! (fn [_result] nil)]
        (command-rt/run-command-in-session! "session-A" "player-A"
                                            {:command :level-up
                                             :command-id "fixed-command-id"})
        (is (= "fixed-command-id" (:command-id @captured-command)))))))

(deftest run-command-in-session-uses-installed-session-resolver-test
  (testing "bound owner session is used when explicit session id is nil"
    (let [captured-command (atom nil)]
      (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id "injected-session"
                                                  :player-uuid "player-A"}} (fn [] (with-redefs [store/get-or-create-player-state! (fn [_session-id _uuid] baseline-state)
                      store/set-player-state! (fn [_session-id _uuid _state] nil)
                      store/mark-player-dirty! (fn [_session-id _uuid] nil)
                      reducer/apply-command (fn [state command]
                                              (reset! captured-command command)
                                              {:state state :events [] :effects []})
                      interpreter/execute-reducer-result! (fn [_result] nil)]
          (command-rt/run-command-in-session! nil "player-A" {:command :level-up})
          (is (= "injected-session" (:session-id @captured-command)))))))))

(deftest run-command-in-session-idempotent-replay-test
  (testing "idempotent mode replays cached result and skips reducer/effects"
    (player-state-support/with-framework
      (fn []
        (command-rt/reset-command-traces-for-test!)
        (let [apply-count (atom 0)
              effect-count (atom 0)
              command {:command :level-up
                       :command-id "idempotent-command-1"}
              opts {:idempotent? true}]
          (with-redefs [store/get-or-create-player-state! (fn [_session-id _uuid] baseline-state)
                        store/set-player-state! (fn [_session-id _uuid _state] nil)
                        store/mark-player-dirty! (fn [_session-id _uuid] nil)
                        reducer/apply-command (fn [state _normalized-command]
                                                (swap! apply-count inc)
                                                {:state state
                                                 :events [{:event/type :ability/test-event}]
                                                 :effects [{:effect/type :persist-state}]})
                        interpreter/execute-reducer-result! (fn [_result]
                                                              (swap! effect-count inc)
                                                              nil)]
            (let [first-result (command-rt/run-command-in-session! "session-A" "player-A" command opts)
                  second-result (command-rt/run-command-in-session! "session-A" "player-A" command opts)]
              (is (= 1 @apply-count))
              (is (= 1 @effect-count))
              (is (nil? (:idempotent-replay? first-result)))
              (is (true? (:idempotent-replay? second-result))))))))))

(deftest run-command-in-session-records-trace-test
  (testing "command trace snapshot contains completed command entry"
    (player-state-support/with-framework
      (fn []
        (command-rt/reset-command-traces-for-test!)
        (with-redefs [store/get-or-create-player-state! (fn [_session-id _uuid] baseline-state)
                      store/set-player-state! (fn [_session-id _uuid _state] nil)
                      store/mark-player-dirty! (fn [_session-id _uuid] nil)
                      reducer/apply-command (fn [state _normalized-command]
                                              {:state state :events [] :effects []})
                      interpreter/execute-reducer-result! (fn [_result] nil)]
          (command-rt/run-command-in-session! "session-Z" "player-Z"
                                              {:command :level-up
                                               :command-id "trace-check-1"})
          (is (contains? (command-rt/command-traces-snapshot)
                         ["session-Z" "player-Z" "trace-check-1"])))))))

(deftest run-command-in-session-tick-command-not-traced-test
  (testing "auto-generated command ids (no explicit :command-id) are never traced"
    (player-state-support/with-framework
      (fn []
        (command-rt/reset-command-traces-for-test!)
        (with-redefs [store/get-or-create-player-state! (fn [_session-id _uuid] baseline-state)
                      store/set-player-state! (fn [_session-id _uuid _state] nil)
                      store/mark-player-dirty! (fn [_session-id _uuid] nil)
                      reducer/apply-command (fn [state _normalized-command]
                                              {:state state :events [] :effects []})
                      interpreter/execute-reducer-result! (fn [_result] nil)]
          (dotimes [_ 5]
            (command-rt/run-command-in-session! "session-Z" "player-Z" {:command :server-tick}))
          (is (empty? (command-rt/command-traces-snapshot))))))))

(deftest command-sequence-isomorphic-across-session-injection-test
  (testing "same command sequence yields isomorphic state/effects under different injected sessions"
    (let [uuid "player-isomorphic"
          commands [{:command :set-level :level 3}
                    {:command :set-skill-exp :skill-id :arc-gen :amount 42}
                    {:command :change-category-with-level :new-category :electromaster :new-level 4}
                    {:command :clear-all-cooldowns}]
          run-sequence (fn [session-id]
                         (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id session-id
                                                                         :player-uuid uuid}} (fn [] (let [executed (atom [])
                                 result (with-redefs [interpreter/execute-reducer-result!
                                                      (fn [reducer-result]
                                                        (swap! executed conj reducer-result)
                                                        nil)]
                                          (command-rt/run-commands-in-session! nil uuid commands))]
                             {:result result
                              :state (store/get-player-state session-id uuid)
                              :executed @executed}))))]
      (store/reset-store!)
      (try
        (let [a (run-sequence "session-A")
              b (run-sequence "session-B")]
          (is (= (:state a) (:state b)))
          (is (= (:result a) (:result b)))
          (is (= (:executed a) (:executed b))))
        (finally
          (store/reset-store!))))))
