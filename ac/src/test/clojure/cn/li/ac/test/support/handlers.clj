(ns cn.li.ac.test.support.handlers
  "Fixtures for ability server handler tests (context + player-state store)."
  (:require [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.owner :as owner-support]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn server-owner
  ([player-uuid]
   (owner-support/server-owner owner-support/default-server-session-id player-uuid)))

(defn with-server-player-owner
  [player-uuid f]
  ;; player-state-owner is a reader FN now — set the owner through the
  ;; ThreadLocal HOF instead of (broken) binding on a non-dynamic var.
  (runtime-hooks/with-player-state-owner-fn
    {:server-session-id owner-support/default-server-session-id
     :player-uuid (str player-uuid)}
    f))

(defn handler-fixture
  [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture f)))

(defn- store-context-entry
  [player-uuid ctx-id {:keys [skill-id status input-state last-keepalive-ms]
                       :or {skill-id :test-skill
                            status :alive
                            input-state :active
                            last-keepalive-ms 1}}]
  {:id ctx-id
   :player-uuid (str player-uuid)
   :skill-id skill-id
   :status status
   :input-state input-state
   :last-keepalive-ms last-keepalive-ms})

(defn register-owned-server-context!
  "Register matching dispatcher transport + runtime-store context entries."
  [player-uuid ctx-id & {:keys [skill-id status input-state last-keepalive-ms]
                         :or {skill-id :test-skill
                              status :alive
                              input-state :idle
                              last-keepalive-ms 1}}]
  (let [owner (server-owner player-uuid)
        store-entry (store-context-entry player-uuid ctx-id
                                         {:skill-id skill-id
                                          :status status
                                          :input-state input-state
                                          :last-keepalive-ms last-keepalive-ms})]
    (ctx/register-context!
     (assoc (ctx/new-server-context player-uuid skill-id ctx-id owner)
            :status status
            :input-state input-state
            :last-keepalive-ms last-keepalive-ms))
    (store/set-player-state! owner-support/default-server-session-id
                              player-uuid
                              {:context-registry {ctx-id store-entry}})
    store-entry))

(defn get-owned-context
  [player-uuid ctx-id]
  (ctx/get-context (server-owner player-uuid) ctx-id))

(defn store-context
  [player-uuid ctx-id]
  (get-in (store/get-player-state owner-support/default-server-session-id
                                    player-uuid)
          [:context-registry ctx-id]))
