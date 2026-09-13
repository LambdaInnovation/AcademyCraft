(ns cn.li.ac.tutorial.network
  "Server-side tutorial network handlers.
  Follows terminal/network.clj pattern."
  (:require [cn.li.ac.tutorial.messages :as tut-msg]
            [cn.li.ac.tutorial.player :as tut-player]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.util.log :as log]))

;; --- Handlers ---

(defn handle-request-sync
  "Client requests tutorial state sync. Returns {:activated-tuts :misaka-id :first-open?}."
  [_payload player]
  (try
    (let [tut-state (tut-player/state player)]
      {:activated-tuts  (vec (:activated-tuts tut-state))
       :misaka-id       (tut-player/get-misaka-id player)
       :first-open?     (:first-open? tut-state)})
    (catch Exception e
      (log/stacktrace "Error handling tutorial request-sync" e)
      {:error (ex-message e)})))

(defn handle-mark-first-open-done
  "Client reports that the first-open animation has played."
  [_payload player]
  (try
    (tut-player/mark-first-open-done! player)
    {:ok true}
    (catch Exception e
      (log/stacktrace "Error handling mark-first-open-done" e)
      {:error (ex-message e)})))

;; --- Registration ---

(defn register-handlers!
  []
  (net-server/register-handler (tut-msg/msg-id :tutorial/request-sync) handle-request-sync
                               {:owner-spec :server :payload-routing :none})
  (net-server/register-handler (tut-msg/msg-id :tutorial/mark-first-open-done) handle-mark-first-open-done
                               {:owner-spec :server :payload-routing :none})
  nil)
