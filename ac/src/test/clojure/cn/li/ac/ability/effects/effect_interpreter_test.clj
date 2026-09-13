(ns cn.li.ac.ability.effects.effect-interpreter-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.effects.interpreter :as interpreter]
            [cn.li.ac.ability.effects.network-handler :as net]
            [cn.li.ac.ability.effects.persistence-handler :as persist]
            [cn.li.ac.ability.effects.platform-handler :as platform]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest execute-effects-dispatches-to-correct-handlers-test
  (let [calls (atom [])]
    (with-redefs [net/execute-network-send! (fn [session-id effect]
                                              (swap! calls conj [:network session-id effect])
                                              nil)
                  platform/execute-platform-call! (fn [effect]
                                                    (swap! calls conj [:platform effect])
                                                    nil)
                  persist/execute-persist-state! (fn [session-id effect]
                                                   (swap! calls conj [:persist session-id effect])
                                                   nil)]
      (interpreter/execute-effects! "session-A"
        [{:effect/type :network-send :player-uuid "p1"}
         {:effect/type :platform-call :fn-ref :demo/fn :args []}
         {:effect/type :persist-state :player-uuid "p1"}
         {:effect/type :unknown/type}])
      (is (= [[:network "session-A" {:effect/type :network-send :player-uuid "p1"}]
              [:platform {:effect/type :platform-call :fn-ref :demo/fn :args []}]
              [:persist "session-A" {:effect/type :persist-state :player-uuid "p1"}]]
             @calls)))))

(deftest execute-reducer-result-translates-events-before-effects-test
  (let [calls (atom [])]
    (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id "session-A"
                                                   :player-uuid "p1"}} (fn [] (with-redefs [interpreter/execute-effects! (fn [_session-id effects]
                                                   (swap! calls conj [:effects effects])
                                                   nil)]
        (interpreter/execute-reducer-result!
          {:events [{:event/type :ability/test-a}
                    {:event/type :ability/test-b}]
           :effects [{:effect/type :network-send :player-uuid "p1"}]})
        (is (= [[:effects [{:effect/type :fire-event
                            :event {:event/type :ability/test-a}
                            :event-type :ability/test-a
                            :player-uuid nil
                            :ctx-id nil
                            :session-id "session-A"}
                           {:effect/type :fire-event
                            :event {:event/type :ability/test-b}
                            :event-type :ability/test-b
                            :player-uuid nil
                            :ctx-id nil
                            :session-id "session-A"}
                           {:effect/type :network-send :player-uuid "p1"}]]]
               @calls)))))))

(deftest execute-reducer-result-uses-bound-owner-session-test
  (let [captured-effects (atom nil)]
    (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id "effect-session"
                                                   :player-uuid "player-a"}} (fn [] (with-redefs [interpreter/execute-effects! (fn [session-id effects]
                                                   (reset! captured-effects [session-id effects])
                                                   nil)]
        (interpreter/execute-reducer-result!
          {:events [{:event/type :ability/test-a :uuid "player-a"}]
           :effects []})
        (is (= "effect-session" (first @captured-effects)))
        (is (= [{:effect/type :fire-event
                 :event {:event/type :ability/test-a :uuid "player-a"}
                 :event-type :ability/test-a
                 :player-uuid "player-a"
                 :ctx-id nil
                 :session-id "effect-session"}]
               (second @captured-effects))))))))
