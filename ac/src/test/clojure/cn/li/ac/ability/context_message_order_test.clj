(ns cn.li.ac.ability.context-message-order-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.server.handlers.context-handler :as context-handler]
            [cn.li.ac.ability.server.handlers.input-handler :as input-handler]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-state :as ctx-rt]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.ac.ability.util.uuid :as uuid]))

(use-fixtures :each test-contexts/clean-contexts-fixture)

(def ^:private test-server-session-id :test-server-session)

(defn- server-owner [player-uuid]
  {:logical-side :server
   :server-session-id test-server-session-id
   :player-uuid (str player-uuid)})

(defn- get-owned-context [player-uuid ctx-id]
  (ctx/get-context (server-owner player-uuid) ctx-id))

(defn- seed-owned-alive-context!
  [player-uuid ctx-id]
  (ctx/register-context!
   (assoc (ctx/new-server-context player-uuid :arc-gen ctx-id (server-owner player-uuid))
          :input-state :idle
          :last-keepalive-ms 1)))

(deftest out-of-order-key-tick-and-key-up-before-key-down-are-ignored-test
  (let [ctx-id "ctx-order-1"]
    (seed-owned-alive-context! "p1" ctx-id)
    (ctx/with-context-owner (server-owner "p1")
      (is (nil? (ctx-rt/handle-key-tick! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})))
      (is (nil? (ctx-rt/handle-key-up! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})))
      (let [ctx-map (ctx/get-context ctx-id)]
        (is (= ctx/STATUS-ALIVE (:status ctx-map)))
        (is (= :idle (:input-state ctx-map)))))))

(deftest out-of-order-key-messages-are-counted-at-handler-boundary-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-order-handler"]
      (seed-owned-alive-context! "p1" ctx-id)
      (runtime-hooks/with-client-ctx-fn {:player-owner (server-owner "p1")} (fn [] (is (nil? (input-handler/handle-key-tick-skill {:ctx-id ctx-id :skill-id :arc-gen} "p1")))
        (is (nil? (input-handler/handle-key-up-skill {:ctx-id ctx-id :skill-id :arc-gen} "p1")))))
      (is (= ctx/STATUS-ALIVE (:status (get-owned-context "p1" ctx-id))))
      (is (= :idle (:input-state (get-owned-context "p1" ctx-id)))))))

(deftest duplicate-terminate-messages-are-idempotent-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-order-2"
          sends (atom 0)]
      (seed-owned-alive-context! "p1" ctx-id)
      (runtime-hooks/with-client-ctx-fn {:player-owner (server-owner "p1")} (fn [] (with-redefs [ctx-mgr/send-terminated-context! (fn [_] (swap! sends inc))]
          (context-handler/handle-terminate-context {:ctx-id ctx-id} "p1")
          (context-handler/handle-terminate-context {:ctx-id ctx-id} "p1"))))
      (is (= 1 @sends))
      (is (= ctx/STATUS-TERMINATED (:status (get-owned-context "p1" ctx-id)))))))

(deftest keepalive-arriving-after-terminate-is-rejected-test
  (with-redefs [uuid/player-uuid identity]
    (let [ctx-id "ctx-order-3"]
      (seed-owned-alive-context! "p1" ctx-id)
      (runtime-hooks/with-client-ctx-fn {:player-owner (server-owner "p1")} (fn [] (context-handler/handle-terminate-context {:ctx-id ctx-id} "p1")
        (is (nil? (context-handler/handle-keepalive-context {:ctx-id ctx-id} "p1")))))
      (let [ctx-map (get-owned-context "p1" ctx-id)]
        (is (= ctx/STATUS-TERMINATED (:status ctx-map)))
        (is (= 1 (:last-keepalive-ms ctx-map)))))))
