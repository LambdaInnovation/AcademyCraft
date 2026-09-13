(ns cn.li.fabric1201.gui.network.shared
  "Shared Fabric GUI network channel and envelope helpers."
  (:require [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.sync-codec :as sync-codec]
            [cn.li.platform.neutral.config :as mod-config]
            [cn.li.mcmod.util.log :as log])
  (:import [io.netty.buffer Unpooled]
           [net.minecraft.network FriendlyByteBuf]
           [net.minecraft.resources ResourceLocation]))

(def ^:private channels-lock (Object.))
(def ^ResourceLocation c2s-channel nil)
(def ^ResourceLocation s2c-channel nil)
(def ^ResourceLocation runtime-sync-s2c-channel nil)

(defn install-channels!
  "Create immutable channel identifiers only after the config provider installs
   `mod-id`. The Vars then hold ResourceLocation values directly, keeping send
   and receive paths free of provider lookup and atom dereferencing."
  []
  (locking channels-lock
    (when-not c2s-channel
      (let [mod-id (str mod-config/mod-id)]
        (when (or (empty? mod-id) (= "nil" mod-id))
          (throw (IllegalStateException.
                   "Fabric network channels require installed config provider")))
        (alter-var-root #'c2s-channel
                        (constantly (ResourceLocation. mod-id "clj_rpc_c2s")))
        (alter-var-root #'s2c-channel
                        (constantly (ResourceLocation. mod-id "clj_rpc_s2c")))
        (alter-var-root #'runtime-sync-s2c-channel
                        (constantly (ResourceLocation. mod-id "runtime_sync_v2")))))
    nil))

(defn make-buf
  [payload]
  (doto (FriendlyByteBuf. (Unpooled/buffer))
    (.writeByteArray ^bytes (packet-base/encode-payload-bytes payload))))

(defn read-buf-map
  [^FriendlyByteBuf buf]
  (packet-base/decode-payload-bytes
    (.readByteArray buf)
    #(log/stacktrace "Failed to deserialize Fabric network payload:" %)))

(defn make-runtime-sync-buf
  [payload]
  (doto (FriendlyByteBuf. (Unpooled/buffer))
    (sync-codec/write-payload! payload)))

(defn read-runtime-sync-payload
  [^FriendlyByteBuf buf]
  (sync-codec/read-payload! buf))
