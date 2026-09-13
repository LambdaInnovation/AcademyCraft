(ns cn.li.fabric262.adapter.network
  "Fabric transport wiring for runtime system.

  Reuses existing runtime RPC handlers and Fabric GUI S2C transport.
  Keeps protocol/message IDs aligned with Forge runtime network implementation."
  (:require [cn.li.fabric262.gui.network.server :as gui-network]
            [cn.li.fabric262.adapter.server-context :as _server-context]
            [cn.li.mc262.runtime.network-core :as network-core]))

(def send-sync-to-client!
  network-core/send-sync-to-client!)

(defn init!
  "Initialize runtime network stack: register server handlers and injected send fns."
  []
  (network-core/install-runtime-network-transport! {:label "Fabric"
                                                   :install-server-context! _server-context/install-server-context!
                                                   :send-push-to-client! gui-network/send-push-to-client!}))
