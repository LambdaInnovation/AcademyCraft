(ns cn.li.mcbase.command.executor-core
  "Command action executor helpers for Minecraft-owned generic actions."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcbase.command.action-impls :as action-impls])
  (:import [cn.li.mcver AdvancementAccess]
           [net.minecraft.server.level ServerPlayer]))

(defn execute-send-message-action
  [action-map send-feedback-fn]
  (let [message (:message action-map)
        translate? (:translate? action-map true)
        args (:args action-map [])
        error? (:error? action-map false)]
    (send-feedback-fn message translate? args error?)
    {:success? true}))

(defn grant-advancement!
  [^ServerPlayer player advancement-id send-feedback-fn]
  (try
    (if-not (AdvancementAccess/grantAllRemaining player advancement-id)
      (do
        (send-feedback-fn "command.mcmod.grant_advancement.not_found" true [advancement-id] true)
        {:success? false :message "Advancement not found"})
      (do
        (send-feedback-fn "command.mcmod.grant_advancement.success" true
                          [advancement-id (AdvancementAccess/playerName player)] false)
        {:success? true}))
    (catch Exception e
      (log/stacktrace "Failed to grant advancement:" e)
      (send-feedback-fn (str "Error: " (ex-message e)) false [] true)
      {:success? false :message (ex-message e)})))

(defn execute-grant-advancement-action
  [action-map send-feedback-fn]
  (let [advancement-id (:advancement-id action-map)
        player (:player action-map)]
    (grant-advancement! player advancement-id send-feedback-fn)))

(action-impls/install-executor!
  {:execute-send-message-action execute-send-message-action
   :execute-grant-advancement-action execute-grant-advancement-action})
