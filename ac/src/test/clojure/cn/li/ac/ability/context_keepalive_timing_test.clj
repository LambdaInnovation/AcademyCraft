(ns cn.li.ac.ability.context-keepalive-timing-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.messages :as catalog]))

(use-fixtures :each test-contexts/clean-contexts-fixture)

(def ^:private test-context-owner
  {:logical-side :server :server-session-id :test-session :player-uuid "p-keepalive"})

(defn- with-system-property
  [k v f]
  (let [old-v (System/getProperty k)]
    (try
      (System/setProperty k v)
      (f)
      (finally
        (if (some? old-v)
          (System/setProperty k old-v)
          (System/clearProperty k))))))

(defn- register-alive-context!
  [ctx-id last-keepalive-ms]
  (ctx/register-context!
    (assoc (ctx/new-server-context "p-keepalive" :arc-gen ctx-id test-context-owner)
          :last-keepalive-ms last-keepalive-ms)))

(deftest keepalive-timeout-threshold-behavior-test
  (with-system-property "ac.ctx.keepalive-timeout-ms" "1500"
    (fn []
      (let [now (System/currentTimeMillis)
            sends (atom [])
            in-window-id "ctx-in-window"
            expired-id "ctx-expired"]
        (ctx-mgr/register-send-fns! {:to-client (fn [_uuid msg-id payload]
                                                  (when (= catalog/MSG-CTX-TERMINATE msg-id)
                                                    (swap! sends conj (:ctx-id payload))))
                                      :to-server nil})
        (register-alive-context! in-window-id (- now 1400))
        (register-alive-context! expired-id (- now 1600))

        (ctx-mgr/tick-context-manager!)

        (ctx/with-context-owner test-context-owner
          (is (= ctx/STATUS-ALIVE (:status (ctx/get-context in-window-id))))
          (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context expired-id)))))
        (is (= [expired-id] @sends))))))
