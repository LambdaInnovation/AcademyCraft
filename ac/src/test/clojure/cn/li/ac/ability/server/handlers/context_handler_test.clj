(ns cn.li.ac.ability.server.handlers.context-handler-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.server.handlers.context-handler :as context-handler]
            [cn.li.ac.ability.server.handlers.input-handler :as input-handler]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-state :as ctx-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.test.support.handlers :as handler-support]))

(use-fixtures :each handler-support/handler-fixture)

(deftest context-lifecycle-handlers-require-context-owner-test
  (with-redefs [uuid/player-uuid identity]
    (let [events (atom [])
          ctx-id "ctx-owned"]
      (handler-support/register-owned-server-context! "p1" ctx-id)
      (ctx/with-context-owner (handler-support/server-owner "p1")
        (ctx/ctx-on! ctx-id :test/channel #(swap! events conj %)))

      (handler-support/with-server-player-owner "p2"
        #(context-handler/handle-keepalive-context {:ctx-id ctx-id} "p2"))
      (is (= 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id))))

      (handler-support/with-server-player-owner "p2"
        #(context-handler/handle-channel-context {:ctx-id ctx-id
                                                  :channel :test/channel
                                                  :payload {:n 1}}
                                                 "p2"))
      (is (empty? @events))

      (handler-support/with-server-player-owner "p2"
        #(context-handler/handle-terminate-context {:ctx-id ctx-id} "p2"))
      (is (= ctx/STATUS-ALIVE (:status (handler-support/get-owned-context "p1" ctx-id))))

      (handler-support/with-server-player-owner "p1"
        #(context-handler/handle-keepalive-context {:ctx-id ctx-id} "p1"))
      (is (< 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id))))

      (handler-support/with-server-player-owner "p1"
        #(context-handler/handle-channel-context {:ctx-id ctx-id
                                                  :channel :test/channel
                                                  :payload {:n 2}}
                                                 "p1"))
      (is (= [{:n 2}] @events))

      (handler-support/with-server-player-owner "p1"
        #(context-handler/handle-terminate-context {:ctx-id ctx-id} "p1"))
      (is (= ctx/STATUS-TERMINATED (:status (handler-support/get-owned-context "p1" ctx-id)))))))

(deftest input-handlers-require-context-owner-and-refresh-keepalive-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-input"]
      (handler-support/register-owned-server-context! "p1" ctx-id)

      (handler-support/with-server-player-owner "p2"
        #(input-handler/handle-key-tick-skill {:ctx-id ctx-id} "p2"))
      (is (= 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id))))

      (handler-support/with-server-player-owner "p1"
        #(input-handler/handle-key-tick-skill {:ctx-id ctx-id} "p1"))
      (is (< 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id)))))))

(deftest slot-key-tick-handler-refreshes-keepalive-without-dispatching-runtime-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-keepalive-only"
          dispatch-calls (atom [])]
      (handler-support/register-owned-server-context! "p1" ctx-id)
      (with-redefs [ctx-rt/handle-key-tick! (fn [& args]
                                              (swap! dispatch-calls conj args)
                                              true)]
        (handler-support/with-server-player-owner "p1"
          #(input-handler/handle-key-tick-skill {:ctx-id ctx-id} "p1"))
        (is (empty? @dispatch-calls))
        (is (< 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id))))))))

(deftest keepalive-and-channel-ignore-non-alive-context-test
  (with-redefs [uuid/player-uuid identity]
    (let [events (atom [])
          ctx-id "ctx-not-alive"]
      (handler-support/register-owned-server-context!
       "p1" ctx-id :status ctx/STATUS-TERMINATED)
      (ctx/with-context-owner (handler-support/server-owner "p1")
        (ctx/ctx-on! ctx-id :test/channel #(swap! events conj %)))

      (handler-support/with-server-player-owner "p1"
        #(context-handler/handle-keepalive-context {:ctx-id ctx-id} "p1"))
      (handler-support/with-server-player-owner "p1"
        #(context-handler/handle-channel-context {:ctx-id ctx-id
                                                  :channel :test/channel
                                                  :payload {:n 1}}
                                                 "p1"))
      (is (= 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id))))
      (is (empty? @events)))))

(deftest key-down-does-not-let-other-player-reuse-existing-context-id-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-key-down"]
      (handler-support/register-owned-server-context! "p1" ctx-id :input-state :idle)
      (handler-support/with-server-player-owner "p2"
        #(input-handler/handle-key-down-skill {:ctx-id ctx-id :skill-id :other-skill} "p2"))
      (is (= "p1" (:player-uuid (handler-support/get-owned-context "p1" ctx-id))))
      (is (= :test-skill (:skill-id (handler-support/get-owned-context "p1" ctx-id))))
      (is (= :idle (:input-state (handler-support/get-owned-context "p1" ctx-id))))
      (is (= 1 (:last-keepalive-ms (handler-support/get-owned-context "p1" ctx-id)))))))

(deftest key-down-does-not-auto-establish-missing-context-test
  (with-redefs [uuid/player-uuid identity]
    (let [establish-calls (atom [])
          rt-calls (atom [])]
      (with-redefs [ctx-mgr/establish-context! (fn [player-uuid ctx-id skill-id]
                                                 (swap! establish-calls conj {:player-uuid player-uuid
                                                                              :ctx-id ctx-id
                                                                              :skill-id skill-id})
                                                 nil)
                    ctx-rt/handle-key-down! (fn [ctx-id _payload _terminate-fn]
                                              (swap! rt-calls conj ctx-id)
                                              true)]
        (is (nil? (handler-support/with-server-player-owner "p1"
                    #(input-handler/handle-key-down-skill {:ctx-id "ctx-missing"
                                                           :skill-id :arc-gen}
                                                          "p1"))))
        (is (empty? @establish-calls))
        (is (empty? @rt-calls))))))

(deftest key-input-ignores-owned-but-non-alive-context-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-constructed"
          down-calls (atom 0)]
      (handler-support/register-owned-server-context!
       "p1" ctx-id :skill-id :arc-gen :status ctx/STATUS-CONSTRUCTED)
      (with-redefs [ctx-rt/handle-key-down! (fn [_ctx-id _payload _terminate-fn]
                                              (swap! down-calls inc)
                                              true)]
        (is (nil? (handler-support/with-server-player-owner "p1"
                    #(input-handler/handle-key-down-skill {:ctx-id ctx-id
                                                           :skill-id :arc-gen}
                                                          "p1"))))
        (is (= 0 @down-calls))))))

(deftest malformed-context-and-input-payloads-record-payload-invalid-test
  (with-redefs [uuid/player-uuid identity]
    (is (nil? (handler-support/with-server-player-owner "p1"
                #(context-handler/handle-keepalive-context {:ctx-id nil} "p1"))))
    (is (nil? (handler-support/with-server-player-owner "p1"
                #(context-handler/handle-channel-context {:ctx-id "ctx-x"
                                                          :channel nil
                                                          :payload {}}
                                                         "p1"))))
    (is (nil? (handler-support/with-server-player-owner "p1"
                #(input-handler/handle-key-down-skill {:ctx-id "" :skill-id :arc-gen} "p1"))))))

(deftest begin-link-rejects-malformed-or-owned-mismatch-context-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-begin-owned"
          establish-calls (atom [])]
      (ctx/register-context!
       (assoc (ctx/new-server-context "attacker" :arc-gen ctx-id
                                      (handler-support/server-owner "attacker"))
              :player-uuid "owner"))
      (with-redefs [ctx-mgr/establish-context! (fn [& args]
                                                 (swap! establish-calls conj args))]
        (is (nil? (handler-support/with-server-player-owner "owner"
                    #(context-handler/handle-begin-link-context {:ctx-id nil :skill-id :arc-gen} "owner"))))
        (is (nil? (handler-support/with-server-player-owner "attacker"
                    #(context-handler/handle-begin-link-context {:ctx-id ctx-id :skill-id :arc-gen} "attacker"))))
        (is (empty? @establish-calls))))))

(deftest same-client-context-id-is-isolated-by-server-player-owner-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "cid-1"
          establish-calls (atom [])]
      (with-redefs [ctx-mgr/establish-context! (fn [player-uuid ctx-id skill-id]
                                                 (swap! establish-calls conj [player-uuid ctx-id skill-id])
                                                 (ctx/with-context-owner (handler-support/server-owner player-uuid)
                                                   (handler-support/register-owned-server-context!
                                                    player-uuid ctx-id :skill-id skill-id :input-state :idle)))]
        (handler-support/with-server-player-owner "p1"
          #(context-handler/handle-begin-link-context {:ctx-id ctx-id :skill-id :railgun} "p1"))
        (handler-support/with-server-player-owner "p2"
          #(context-handler/handle-begin-link-context {:ctx-id ctx-id :skill-id :meltdowner} "p2")))
      (is (= [["p1" ctx-id :railgun]
              ["p2" ctx-id :meltdowner]]
             @establish-calls))
      (is (= :railgun (:skill-id (handler-support/get-owned-context "p1" ctx-id))))
      (is (= :meltdowner (:skill-id (handler-support/get-owned-context "p2" ctx-id)))))))
