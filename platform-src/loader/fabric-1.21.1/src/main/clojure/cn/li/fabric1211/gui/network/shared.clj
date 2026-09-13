(ns cn.li.fabric1211.gui.network.shared
  "Shared Fabric GUI network channel and envelope helpers."
  (:require [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.sync-codec :as sync-codec]
            [cn.li.platform.neutral.config :as mod-config]
            [cn.li.mcmod.util.log :as log])
  (:import [io.netty.buffer Unpooled]
           [net.minecraft.network FriendlyByteBuf]))

(defn configured-mod-id!
  "Read the facade only during listener installation.  The Java bridge then
   caches typed payload IDs, so packet paths neither consult the facade nor
   construct ResourceLocations."
  []
  (let [mod-id (str mod-config/mod-id)]
    (when (or (empty? mod-id) (= "nil" mod-id))
      (throw (IllegalStateException.
               "Fabric network channels require installed config provider")))
    mod-id))

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

(defn encode-map-bytes [payload]
  (packet-base/encode-payload-bytes payload))

(defn decode-map-bytes [^bytes payload]
  (packet-base/decode-payload-bytes payload
    #(log/stacktrace "Failed to deserialize Fabric typed payload:" %)))

(defn- buffer->bytes [^FriendlyByteBuf buf]
  (let [length (.readableBytes buf)
        bytes (byte-array length)]
    (.readBytes buf bytes)
    bytes))

(defn encode-runtime-bytes [payload]
  (buffer->bytes (doto (FriendlyByteBuf. (Unpooled/buffer))
                   (sync-codec/write-payload! payload))))

(defn decode-runtime-bytes [^bytes payload]
  (sync-codec/read-payload! (FriendlyByteBuf. (Unpooled/wrappedBuffer payload))))
