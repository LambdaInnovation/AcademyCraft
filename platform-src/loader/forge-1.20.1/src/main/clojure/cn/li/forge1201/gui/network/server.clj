(ns cn.li.forge1201.gui.network.server
  "Forge 1.20.1 GUI/RPC server transport via ClojureNetwork SimpleChannel."
  (:require [cn.li.forge1201.gui.network.shared :as shared]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.platform.neutral.network-runtime :as net-server]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server.level ServerPlayer]))

(defn send-response-to-client!
  [^ServerPlayer player request-id payload]
  (shared/invoke-network-static "sendToClient"
    player
    (int request-id)
    (packet-base/encode-payload-bytes (or payload {}))))

(defn- handle-server-request!
  [msg-id request-id payload-bytes player]
  (try
    (let [payload (shared/decode-request-payload payload-bytes)
          respond-fn (fn [req-id response]
                       (send-response-to-client! player (int req-id) response))]
      (shared/with-server-player-owner
        player
        #(net-server/handle-request
           msg-id
           (int request-id)
           payload
           player
           respond-fn)))
    (catch Throwable t
      (log/stacktrace "[GUI-NETWORK] req-handler UNCAUGHT:" t))))

(defn- make-request-handler
  []
  (fn [msg-id request-id payload-bytes player]
    (handle-server-request! msg-id request-id payload-bytes player)))

(defn init-server!
  "Register the C2S request handler. Channel install completes when the
  client response handler is also registered (see shared/try-install-handlers!)."
  []
  (shared/set-request-handler! (make-request-handler))
  (shared/try-install-handlers!)
  nil)
