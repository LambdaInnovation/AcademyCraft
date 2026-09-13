(ns cn.li.mc1201.runtime.network-core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mc1201.runtime.network-core :as network-core]
            [cn.li.mcmod.content.registry :as content-registry]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.hooks.messages :as messages]))

(def ^:private test-message-ids
  {:ctx-channel "ability:ctx/channel"
   :sync-v2 "ability:sync/runtime-v2"})

(defn- network-fixture
  [f]
  (content-registry/clear-registry!)
  (messages/clear-messages!)
  (messages/register-messages! test-message-ids)
  (try
    (f)
    (finally
      (messages/clear-messages!)
      (content-registry/clear-registry!))))

(use-fixtures :each network-fixture)

(deftest targeted-sender-requires-existing-player-test
  (let [sent (atom [])
        sender (network-core/create-targeted-client-sender
                 (fn [uuid]
                   (when (= uuid "player-a")
                     {:player uuid}))
                 (fn [player msg-id payload]
                   (swap! sent conj [player msg-id payload])))]
    (is (= {:sent 1
            :msg-id "msg/a"
            :target-player-uuid "player-a"}
           (sender "player-a" "msg/a" {:x 1})))
    (is (= [[{:player "player-a"} "msg/a" {:x 1}]] @sent))
    (try
      (sender "missing-player" "msg/b" {:x 2})
      (is false "missing target should throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :target-player-not-found (:reason (ex-data e))))
        (is (= "missing-player" (:target-player-uuid (ex-data e))))))))

(deftest sync-sender-reports-counts-and-missing-targets-test
  (let [sent (atom [])
        sender (network-core/create-sync-sender
                 (fn [uuid]
                   (when (= uuid "player-a")
                     {:player uuid}))
                 (fn [player msg-id payload]
                   (swap! sent conj [player msg-id payload])))]
    (let [payload {:version 2 :opcode 2 :revision 7 :dirty-mask 3
                   :ability-data {:a 1} :resource-data {:r 2}}]
      (is (= {:sent 1
              :target-player-uuid "player-a"
              :msg-id (:sync-v2 test-message-ids)}
             (sender "player-a" payload)))
      (is (= [[{:player "player-a"} (:sync-v2 test-message-ids) payload]]
             @sent)))
    (try
      (sender "missing-player" {:ability-data {:a 1}})
      (is false "missing target should throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :target-player-not-found (:reason (ex-data e))))))))

(deftest except-local-sender-returns-structured-results-test
  (let [sent (atom [])
        sender (network-core/create-except-local-context-sender
                 (fn [_source-player-uuid _radius]
                   ["source-player" "near-1" "near-2"])
                 (fn [target-uuid msg-id payload]
                   (swap! sent conj [target-uuid msg-id payload])))]
    (with-redefs [runtime-hooks/get-context-player-uuid (fn [_ctx-id] "source-player")]
      (is (= {:sent 2
              :ctx-id "ctx-1"
              :channel :fx
              :source-player-uuid "source-player"
              :target-player-uuids ["near-1" "near-2"]}
             (sender "ctx-1" :fx {:v 7} {}))))
    (is (= [["near-1" (:ctx-channel test-message-ids) {:ctx-id "ctx-1" :channel :fx :payload {:v 7}}]
            ["near-2" (:ctx-channel test-message-ids) {:ctx-id "ctx-1" :channel :fx :payload {:v 7}}]]
           @sent))
    (with-redefs [runtime-hooks/get-context-player-uuid (fn [_ctx-id] nil)]
      (is (= {:sent 0
              :reason :missing-context-player-uuid
              :ctx-id "ctx-404"
              :channel :fx}
             (sender "ctx-404" :fx {:v 9} {}))))))

(deftest init-runtime-network-registers-owner-aware-routes-test
  (let [registered-routes (atom nil)
        registered-send-fns (atom nil)
        sent (atom [])]
    (with-redefs [runtime-hooks/register-network-handlers! (fn [] nil)
                  runtime-hooks/register-context-route-fns! (fn [routes] (reset! registered-routes routes))
                  runtime-hooks/register-context-send-fns! (fn [send-fns] (reset! registered-send-fns send-fns))
                  runtime-hooks/get-context-player-uuid (fn [_ctx-id] nil)]
      (network-core/init-runtime-network!
        {:send-to-server-fn (fn [msg-id payload]
                              (swap! sent conj [:server msg-id payload]))
         :send-to-client-fn (fn [player-uuid msg-id payload]
                              (swap! sent conj [:client player-uuid msg-id payload])
                              {:sent 1 :target-player-uuid player-uuid})}))
    (is (map? @registered-routes))
    (is (map? @registered-send-fns))
    (is (= {:sent 1
            :ctx-id "ctx-server"
            :channel :sync
            :msg-id (:ctx-channel test-message-ids)}
           ((:to-server @registered-routes) "ctx-server" :sync {:v 1} {})))
    (is (= {:sent 0
            :reason :missing-context-player-uuid
            :ctx-id "ctx-client"
            :channel :sync}
           ((:to-client @registered-routes) "ctx-client" :sync {:v 2} {})))
    (is (= {:ctx-id "ctx-client"
            :channel :sync
            :sent 1
            :target-player-uuid "player-b"}
           ((:to-client @registered-routes) "ctx-client" :sync {:v 3} {:player-uuid "player-b"})))
    (is (= [[:server (:ctx-channel test-message-ids) {:ctx-id "ctx-server" :channel :sync :payload {:v 1}}]
            [:client "player-b" (:ctx-channel test-message-ids) {:ctx-id "ctx-client" :channel :sync :payload {:v 3}}]]
         @sent))))
