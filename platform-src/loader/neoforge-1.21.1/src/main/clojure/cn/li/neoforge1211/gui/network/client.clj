(ns cn.li.neoforge1211.gui.network.client
  "NeoForge 1.21.1 GUI/RPC client transport via ClojureNetwork payloads.

  Keeps client Minecraft types out of this namespace: session/owner fns are
  injected via shared/install-client-owner-functions! from client init."
  (:require [cn.li.neoforge1211.gui.network.shared :as shared]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.client-network :as net-client]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.target :as target]))

(defn send-to-server!
  [msg-id request-id payload]
  (shared/invoke-network-static "sendToServer"
    msg-id
    (int request-id)
    (packet-base/encode-payload-bytes payload)))

(defn- handle-client-response!
  [request-id response-bytes]
  (try
    (let [payload (shared/decode-response-payload request-id response-bytes)]
      (shared/with-client-response-owner payload
        #(packet-base/dispatch-client-response!
           (runtime-hooks/player-state-owner)
           request-id
           payload)))
    (catch Throwable t
      (log/stacktrace (str "[GUI-NETWORK] resp-handler UNCAUGHT request-id=" request-id ":" ) t))))

(defn- make-response-handler
  []
  (fn [request-id response-bytes]
    (handle-client-response! request-id response-bytes)))

(defn- register-request-transport!
  []
  (net-client/register-request-transport!
    (target/current-target-key!)
    (fn [msg-id payload request-id]
      (send-to-server! msg-id request-id payload))))

(defn init-client!
  "Register S2C response handler and client request transport.
  Handler install completes once the server request handler is also registered."
  []
  (register-request-transport!)
  (shared/set-response-handler! (make-response-handler))
  (shared/try-install-handlers!)
  nil)
