(ns cn.li.mcbase.command.action-impls
  "Shared command action defmethod implementations for Minecraft-owned actions.

  Content-specific action ids and state mutations are registered by content
  modules through cn.li.mcmod.command.actions."
  (:require [cn.li.mcbase.command.feedback :as feedback]
            [cn.li.mcmod.command.actions :as cmd-actions])
  (:import [net.minecraft.commands CommandSourceStack]))

(defonce ^:private executor-atom (atom nil))

(defn install-executor!
  [m]
  (reset! executor-atom m)
  m)

(defn- executor []
  (let [m @executor-atom]
    (when (nil? m)
      (throw (IllegalStateException. "command executor-core not installed")))
    m))

(defmethod cmd-actions/execute-action-impl :send-message
  [action-map context]
  (let [^CommandSourceStack source (:source context)
        send-feedback-fn (fn [msg trans? args err?]
                           (feedback/send-feedback! source msg trans? args err?))]
    ((:execute-send-message-action (executor)) action-map send-feedback-fn)))

(defmethod cmd-actions/execute-action-impl :grant-advancement
  [action-map context]
  (let [^CommandSourceStack source (:source context)
        send-feedback-fn (fn [msg trans? args err?]
                           (feedback/send-feedback! source msg trans? args err?))]
    ((:execute-grant-advancement-action (executor)) action-map send-feedback-fn)))
