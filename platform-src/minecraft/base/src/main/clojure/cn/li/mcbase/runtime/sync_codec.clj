(ns cn.li.mcbase.runtime.sync-codec
  "Fixed runtime-v2 frame. The five domain payloads are length-delimited in
  dirty-mask order; generic GUI envelopes and message-id strings are absent."
  (:require [cn.li.platform.neutral.network-runtime :as domain-codec])
  (:import [io.netty.buffer Unpooled]
           [java.nio ByteBuffer]
           [net.minecraft.network FriendlyByteBuf]))

(def ^:const magic 0x41435232) ; ACR2
(def ^:const protocol-version 2)
(def ^:const full-opcode 1)
(def ^:const delta-opcode 2)
(def ^:const allowed-mask 0x1f)
(def ^:private max-domain-bytes (* 4 1024 1024))

(def ^:private domains
  [[0x01 :ability-data]
   [0x02 :resource-data]
   [0x04 :cooldown-data]
   [0x08 :preset-data]
   [0x10 :develop-data]])

(defn- validate-header! [opcode mask]
  (when-not (or (= opcode full-opcode) (= opcode delta-opcode))
    (throw (ex-info "Invalid runtime sync opcode" {:opcode opcode})))
  (when-not (= mask (bit-and mask allowed-mask))
    (throw (ex-info "Invalid runtime sync dirty mask" {:dirty-mask mask}))))

(defn write-payload!
  [^FriendlyByteBuf buf {:keys [version opcode uuid revision dirty-mask] :as payload}]
  (let [version (int version)
        opcode (int opcode)
        mask (int dirty-mask)]
    (when-not (= version protocol-version)
      (throw (ex-info "Invalid runtime sync version" {:version version})))
    (validate-header! opcode mask)
    (.writeInt buf magic)
    (.writeByte buf version)
    (.writeByte buf opcode)
    (.writeUtf buf (str uuid) 64)
    (.writeVarLong buf (long revision))
    (.writeByte buf mask)
    (doseq [[bit domain] domains]
      (when-not (zero? (bit-and mask bit))
        (let [value (get payload domain)]
          (when-not (some? value)
            (throw (ex-info "Runtime sync domain missing for dirty bit"
                            {:domain domain :dirty-mask mask})))
          (.writeByteArray buf ^bytes (domain-codec/encode value)))))
    buf))

(defn read-payload!
  [^FriendlyByteBuf buf]
  (when-not (= magic (.readInt buf))
    (throw (ex-info "Invalid runtime sync magic" {})))
  (let [version (int (.readUnsignedByte buf))
        opcode (int (.readUnsignedByte buf))
        uuid (.readUtf buf 64)
        revision (.readVarLong buf)
        mask (int (.readUnsignedByte buf))]
    (when-not (= version protocol-version)
      (throw (ex-info "Unsupported runtime sync version" {:version version})))
    (validate-header! opcode mask)
    (reduce (fn [payload [bit domain]]
              (if (zero? (bit-and mask bit))
                payload
                (assoc payload domain
                       (domain-codec/decode (.readByteArray buf max-domain-bytes)))))
            {:version version
             :opcode opcode
             :uuid uuid
             :revision revision
             :dirty-mask mask}
            domains)))

(defn encode-bytes
  ^bytes [payload]
  (let [^FriendlyByteBuf buf (FriendlyByteBuf. (Unpooled/buffer))]
    (try
      (write-payload! buf payload)
      (let [result (byte-array (.readableBytes buf))]
        (.readBytes buf result)
        result)
      (finally (.release buf)))))

(defn decode-bytes [^bytes payload]
  (let [^FriendlyByteBuf buf (FriendlyByteBuf. (Unpooled/wrappedBuffer payload))]
    (try (read-payload! buf)
         (finally (.release buf)))))

(defn runtime-sync-bytes?
  [^bytes payload]
  (and payload
       (>= (alength payload) 4)
       (= magic (.getInt (ByteBuffer/wrap payload)))))
