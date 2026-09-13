(ns cn.li.forge1201.adapter.network-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mc1201.runtime.network-core :as network]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.hooks.messages :as messages]))

(def ^:private test-message-ids
  {:ctx-channel "ability:ctx/channel"})

(defn- message-fixture [f]
  (messages/clear-messages!)
  (messages/register-messages! test-message-ids)
  (try
    (f)
    (finally
      (messages/clear-messages!))))

(use-fixtures :each message-fixture)

(deftest except-local-sender-broadcasts-to-nearby-players-test
  (let [sent (atom [])
        sender (network/create-except-local-context-sender
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
             (sender "ctx-1" :fx {:v 7} {})))
      (is (= [["near-1" (:ctx-channel test-message-ids)
               {:ctx-id "ctx-1" :channel :fx :payload {:v 7}}]
              ["near-2" (:ctx-channel test-message-ids)
               {:ctx-id "ctx-1" :channel :fx :payload {:v 7}}]]
             @sent)))))

(deftest except-local-sender-skips-when-context-owner-missing-test
  (let [sent (atom [])
        sender (network/create-except-local-context-sender
                 (fn [_source-player-uuid _radius] ["near-1"])
                 (fn [target-uuid msg-id payload]
                   (swap! sent conj [target-uuid msg-id payload])))]
    (with-redefs [runtime-hooks/get-context-player-uuid (fn [_ctx-id] nil)]
      (is (= {:sent 0
              :reason :missing-context-player-uuid
              :ctx-id "ctx-404"
              :channel :fx}
             (sender "ctx-404" :fx {:v 9} {})))
      (is (empty? @sent)))))
