(ns cn.li.mc1201.runtime.network-core
  "Thin re-export of cn.li.mcbase.runtime.network-core."
  (:require [cn.li.mcbase.runtime.network-core :as shared]))

(def sync-message-payloads shared/sync-message-payloads)
(def create-targeted-client-sender shared/create-targeted-client-sender)
(def create-sync-sender shared/create-sync-sender)
(def default-send-to-server! shared/default-send-to-server!)
(def default-find-player-by-uuid shared/default-find-player-by-uuid)
(def default-find-nearby-player-uuids shared/default-find-nearby-player-uuids)
(def create-except-local-context-sender shared/create-except-local-context-sender)
(def send-sync-to-client! shared/send-sync-to-client!)
(def send-to-client! shared/send-to-client!)
(def install-runtime-network-transport! shared/install-runtime-network-transport!)
(def init-runtime-network! shared/init-runtime-network!)
