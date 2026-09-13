(ns cn.li.forge1201.adapter.network
  "Forge transport wiring for runtime system.

  Reuses the existing mcmod RPC channel used by GUI network:
  - server-side handlers are registered by content runtime network handlers
  - client-side requests use mcmod.network.client/send-to-server

  Here we provide helper send-fns for context manager and sync service."
  (:require [cn.li.mc1201.runtime.network-core :as network-core]
            [cn.li.mcbase.runtime.network-payload :as network-payload]
            [cn.li.forge1201.adapter.server-context :as _server-context])
  (:import [cn.li.forge1201.network ClojureNetwork]
           [net.minecraft.server.level ServerPlayer]))

(defn send-push-to-client!
  [^ServerPlayer player msg-id payload]
  (ClojureNetwork/sendToClient player -1 (network-payload/serialize-message msg-id payload)))

(def send-sync-to-client!
  network-core/send-sync-to-client!)

(defn init!
  "Initialize runtime network stack: register server handlers and injected send fns."
  []
  (network-core/install-runtime-network-transport! {:label "Forge"
                                                   :install-server-context! _server-context/install-server-context!
                                                   :send-push-to-client! send-push-to-client!}))
