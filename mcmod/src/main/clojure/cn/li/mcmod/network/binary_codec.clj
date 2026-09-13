(ns cn.li.mcmod.network.binary-codec
  "Tag-framed binary wire codec for network payloads (Clojure data <-> byte[]).

  Restricted to generic GUI RPC messages. Runtime sync v2 and native NBT use
  dedicated fixed-schema codecs. Strings use an explicit int length prefix rather than
  DataOutputStream/FriendlyByteBuf's writeUTF (64KB) / writeUtf (32767-char)
  ceilings, since ability payloads can exceed both."
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream DataOutputStream DataInputStream]
           [java.nio.charset StandardCharsets]
           [cn.li.mcmod.math V3]))

(def ^:private tag-nil (int 0))
(def ^:private tag-false (int 1))
(def ^:private tag-true (int 2))
(def ^:private tag-long (int 3))
(def ^:private tag-double (int 4))
(def ^:private tag-string (int 5))
(def ^:private tag-keyword (int 6))
(def ^:private tag-map (int 7))
(def ^:private tag-vector (int 8))
(def ^:private tag-set (int 9))
(def ^:private tag-v3 (int 10))

(defn- write-str!
  [^DataOutputStream out ^String s]
  (let [^bytes bs (.getBytes s StandardCharsets/UTF_8)]
    (.writeInt out (alength bs))
    (.write out bs)))

(defn- read-str
  ^String [^DataInputStream in]
  (let [n (.readInt in)
        bs (byte-array n)]
    (.readFully in bs)
    (String. bs StandardCharsets/UTF_8)))

(defn- write-val!
  [^DataOutputStream out v]
  (cond
    (nil? v)     (.writeByte out tag-nil)
    (false? v)   (.writeByte out tag-false)
    (true? v)    (.writeByte out tag-true)
    (instance? V3 v)
    (let [^V3 p v]
      (.writeByte out tag-v3)
      (.writeDouble out (.-x p))
      (.writeDouble out (.-y p))
      (.writeDouble out (.-z p)))
    (integer? v) (do (.writeByte out tag-long) (.writeLong out (long v)))
    (float? v)   (do (.writeByte out tag-double) (.writeDouble out (double v)))
    (string? v)  (do (.writeByte out tag-string) (write-str! out v))
    (keyword? v) (do (.writeByte out tag-keyword) (write-str! out (subs (str v) 1)))
    (map? v)     (do (.writeByte out tag-map)
                      (.writeInt out (count v))
                      (reduce-kv (fn [_ k mv] (write-val! out k) (write-val! out mv) nil) nil v))
    (set? v)     (do (.writeByte out tag-set)
                      (.writeInt out (count v))
                      (doseq [x v] (write-val! out x)))
    (sequential? v)
    (do (.writeByte out tag-vector)
        (.writeInt out (count v))
        (doseq [x v] (write-val! out x)))
    :else (throw (ex-info "binary-codec: unsupported value type"
                           {:type (some-> v class .getName) :value v}))))

(defn- read-val
  [^DataInputStream in]
  (let [tag (int (.readByte in))]
    (case tag
      0 nil
      1 false
      2 true
      3 (.readLong in)
      4 (.readDouble in)
      5 (read-str in)
      6 (keyword (read-str in))
      10 (V3. (.readDouble in) (.readDouble in) (.readDouble in))
      7 (let [n (.readInt in)]
          (loop [i 0 m (transient {})]
            (if (< i n)
              (recur (inc i) (assoc! m (read-val in) (read-val in)))
              (persistent! m))))
      8 (let [n (.readInt in)]
          (loop [i 0 acc (transient [])]
            (if (< i n)
              (recur (inc i) (conj! acc (read-val in)))
              (persistent! acc))))
      9 (let [n (.readInt in)]
          (loop [i 0 acc (transient #{})]
            (if (< i n)
              (recur (inc i) (conj! acc (read-val in)))
              (persistent! acc))))
      (throw (ex-info "binary-codec: unknown wire tag" {:tag tag})))))

(defn encode
  "Encode a Clojure value (nil/bool/int/float/string/keyword/map/vector/set/V3,
  arbitrarily nested) to a byte array. Throws on unsupported value types."
  ^bytes [v]
  (let [bos (ByteArrayOutputStream. 256)
        out (DataOutputStream. bos)]
    (write-val! out v)
    (.toByteArray bos)))

(defn decode
  "Decode a byte array produced by `encode` back into the original Clojure value."
  [^bytes bs]
  (read-val (DataInputStream. (ByteArrayInputStream. bs))))
