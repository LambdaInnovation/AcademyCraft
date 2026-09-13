(ns cn.li.mcbase.runtime.native-nbt
  "Typed Clojure value codec backed directly by Minecraft NBT tags.

  Reads go through cn.li.mcver.NbtAccess (classic getters on 1.20.1/1.21.1;
  Optional / OrEmpty forms on 26.2)."
  (:import [cn.li.mcver NbtAccess]
           [net.minecraft.nbt CompoundTag ListTag]))

(def ^:private type-nil 0)
(def ^:private type-boolean 1)
(def ^:private type-long 2)
(def ^:private type-double 3)
(def ^:private type-string 4)
(def ^:private type-keyword 5)
(def ^:private type-map 6)
(def ^:private type-vector 7)
(def ^:private type-set 8)

(declare encode-value decode-value)

(defn- encode-sequence
  ^ListTag [values]
  (let [result (ListTag.)]
    (doseq [value values]
      (.add result (encode-value value)))
    result))

(defn- encode-map
  ^ListTag [value]
  (let [result (ListTag.)]
    (doseq [[key entry-value] value]
      (let [entry (CompoundTag.)]
        (.put entry "k" (encode-value key))
        (.put entry "v" (encode-value entry-value))
        (.add result entry)))
    result))

(defn encode-value
  ^CompoundTag [value]
  (let [result (CompoundTag.)]
    (cond
      (nil? value)
      (.putByte result "t" (byte type-nil))

      (boolean? value)
      (do (.putByte result "t" (byte type-boolean))
          (.putBoolean result "b" (boolean value)))

      (integer? value)
      (do (.putByte result "t" (byte type-long))
          (.putLong result "l" (long value)))

      (number? value)
      (do (.putByte result "t" (byte type-double))
          (.putDouble result "d" (double value)))

      (string? value)
      (do (.putByte result "t" (byte type-string))
          (.putString result "s" value))

      (keyword? value)
      (do (.putByte result "t" (byte type-keyword))
          (.putString result "s" (if-let [ns-part (namespace value)]
                                    (str ns-part "/" (name value))
                                    (name value))))

      (map? value)
      (do (.putByte result "t" (byte type-map))
          (.put result "e" (encode-map value)))

      (set? value)
      (do (.putByte result "t" (byte type-set))
          (.put result "e" (encode-sequence value)))

      (sequential? value)
      (do (.putByte result "t" (byte type-vector))
          (.put result "e" (encode-sequence value)))

      :else
      (throw (ex-info "Unsupported native NBT runtime value"
                      {:value-type (some-> value class .getName)})))
    result))

(defn- decode-list
  [^ListTag values]
  (let [size (.size values)]
    (loop [index 0
           result (transient [])]
      (if (< index size)
        (recur (unchecked-inc-int index)
               (conj! result (decode-value (NbtAccess/getCompoundAt values index))))
        (persistent! result)))))

(defn- decode-map
  [^ListTag entries]
  (let [size (.size entries)]
    (loop [index 0
           result (transient {})]
      (if (< index size)
        (let [^CompoundTag entry (NbtAccess/getCompoundAt entries index)]
          (recur (unchecked-inc-int index)
                 (assoc! result
                         (decode-value (NbtAccess/getCompound entry "k"))
                         (decode-value (NbtAccess/getCompound entry "v")))))
        (persistent! result)))))

(defn decode-value
  [^CompoundTag value]
  (case (int (NbtAccess/getByte value "t"))
    0 nil
    1 (NbtAccess/getBoolean value "b")
    2 (NbtAccess/getLong value "l")
    3 (NbtAccess/getDouble value "d")
    4 (NbtAccess/getString value "s")
    5 (keyword (NbtAccess/getString value "s"))
    6 (decode-map (NbtAccess/getList value "e"))
    7 (decode-list (NbtAccess/getList value "e"))
    8 (into #{} (decode-list (NbtAccess/getList value "e")))
    (throw (ex-info "Unsupported native NBT runtime type"
                    {:type-id (int (NbtAccess/getByte value "t"))}))))
