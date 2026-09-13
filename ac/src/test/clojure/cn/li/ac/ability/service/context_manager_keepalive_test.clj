(ns cn.li.ac.ability.service.context-manager-keepalive-test
  "Guards the keepalive reaper's source of truth.

  `:last-keepalive-ms` is refreshed by the :touch-context-keepalive command
  into the runtime-store projection and never onto the raw transport map, so
  the raw value stays frozen at context-creation time. Judging expiry from the
  raw map therefore killed every server context keepalive-timeout-ms after it
  was created, no matter how alive the client was — capping every hold/charge
  skill at 1.5s."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as cm]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]))

(def ^:private player-id "keepalive-player")

(defn- server-owner []
  {:logical-side :server
   :server-session-id test-player/test-session-id
   :player-uuid player-id})

(defn- reset-fixture [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture f)))

(use-fixtures :each reset-fixture)

(defn- register-aged-server-ctx!
  "Register an alive server context whose transport-level keepalive is already
  older than the timeout, so its deadline bucket is due immediately."
  [ctx-id created-ms]
  (ctx/with-context-owner (server-owner)
    (ctx/register-context!
     (assoc (ctx/new-server-context player-id :thunder-clap ctx-id (server-owner))
            :last-keepalive-ms created-ms))))

(defn- touch-keepalive! [ctx-id at-ms]
  (command-rt/run-command-in-session!
   test-player/test-session-id player-id
   {:command :touch-context-keepalive :ctx-id ctx-id :timestamp-ms at-ms}))

(defn- status-of [ctx-id]
  (ctx/with-context-owner (server-owner)
    (:status (ctx/get-context (server-owner) ctx-id))))

(deftest refreshed-keepalive-keeps-a-long-held-context-alive-test
  (let [now (System/currentTimeMillis)]
    ;; Created well beyond the timeout ago, but the client has been talking:
    ;; exactly a 2s+ charge skill mid-hold.
    (register-aged-server-ctx! "ctx-held" (- now (* 4 (cm/keepalive-timeout-ms))))
    (touch-keepalive! "ctx-held" now)
    (cm/tick-context-manager!)
    (is (= ctx/STATUS-ALIVE (status-of "ctx-held"))
        "the reaper must read the store-projected keepalive, not the frozen transport one")))

(deftest stale-keepalive-still-reaps-the-context-test
  (let [now (System/currentTimeMillis)
        stale (- now (* 4 (cm/keepalive-timeout-ms)))]
    (register-aged-server-ctx! "ctx-abandoned" stale)
    ;; Client stopped talking: the store keepalive is just as old.
    (touch-keepalive! "ctx-abandoned" stale)
    (cm/tick-context-manager!)
    (is (= ctx/STATUS-TERMINATED (status-of "ctx-abandoned"))
        "an abandoned context must still time out")))

(deftest untouched-keepalive-falls-back-to-the-transport-value-test
  ;; :register-context does not seed :last-keepalive-ms in the store, so the
  ;; projection leaves the transport value in place until the first touch.
  (let [now (System/currentTimeMillis)]
    (register-aged-server-ctx! "ctx-never-touched" (- now (* 4 (cm/keepalive-timeout-ms))))
    (cm/tick-context-manager!)
    (is (= ctx/STATUS-TERMINATED (status-of "ctx-never-touched")))))
