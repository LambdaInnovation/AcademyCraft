(ns cn.li.mcbase.runtime.network-payload
  "Shared helpers for packaging runtime network payloads across loaders."
  (:require [cn.li.mcbase.runtime.sync-codec :as sync-codec]
            [cn.li.platform.neutral.network-runtime :as codec]))

(def ^:const runtime-sync-message-id "ability:sync/runtime-v2")

(defn wrap-message
  [msg-id payload]
  {:msg-id msg-id
   :payload payload})

(defn serialize-message
  ^bytes [msg-id payload]
  (if (= runtime-sync-message-id msg-id)
    (sync-codec/encode-bytes payload)
    (codec/encode (wrap-message msg-id payload))))
