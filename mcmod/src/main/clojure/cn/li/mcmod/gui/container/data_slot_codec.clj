(ns cn.li.mcmod.gui.container.data-slot-codec
  "Platform-neutral int codecs for Container DataSlot fields.

  DataSlots only transport ints; encode/decode helpers keep server and client atoms
  aligned without custom S2C state packets."
  (:require [clojure.set]))

(def ^:const int-min -2147483648)
(def ^:const int-max 2147483647)

(defn clamp-int
  [n]
  (cond
    (nil? n) 0
    (>= n int-max) int-max
    (<= n int-min) int-min
    :else (int n)))

(defn int-codec
  []
  {:kind :int
   :encode clamp-int
   :decode clamp-int})

(defn boolean-codec
  []
  {:kind :boolean
   :encode (fn [v] (if v 1 0))
   :decode (fn [v] (not (zero? (int v))))})

(defn scaled-double-codec
  [{:keys [scale]
    :or {scale 100}}]
  (let [scale* (double scale)]
    {:kind :scaled-double
     :scale scale*
     :encode (fn [v] (clamp-int (Math/round (* (double (or v 0.0)) scale*))))
     :decode (fn [i] (/ (double (int i)) scale*))}))

(defn enum-codec
  [code->int int->code]
  {:kind :enum
   :encode (fn [v]
             (clamp-int (get code->int v 0)))
   :decode (fn [i] (get int->code (int i) (first (keys int->code))))})

(defn keyword-enum-codec
  [keywords]
  (let [ordered (vec keywords)
        code->int (into {} (map-indexed (fn [i k] [k i]) ordered))
        int->code (into {} (map-indexed (fn [i k] [i k]) ordered))]
    (enum-codec code->int int->code)))

(defn string-status-codec
  [status-strings]
  (let [ordered (vec status-strings)
        normalize (fn [v]
                    (cond
                      (string? v) v
                      (keyword? v) (name v)
                      :else (str v)))
        code->int (into {} (map-indexed (fn [i s] [s i]) ordered))
        int->code (into {} (map-indexed (fn [i s] [i s]) ordered))]
    {:kind :string-status
     :encode (fn [v]
               (clamp-int (get code->int (normalize v) 0)))
     :decode (fn [i] (get int->code (int i) (first ordered)))}))

(defn codec-for-gui-field
  "Infer a DataSlot codec from a GUI schema field map, or nil when not encodable."
  [field]
  (or (:gui-data-slot-codec field)
      (let [coerce (:gui-coerce field)]
        (cond
          (some? (:gui-data-slot-status-codes field))
          (string-status-codec (:gui-data-slot-status-codes field))

          (= coerce int) (int-codec)
          (= coerce boolean) (boolean-codec)
          (= coerce double) (scaled-double-codec {:scale (or (:gui-data-slot-scale field) 100)})
          (= coerce keyword) (when-let [codes (:gui-data-slot-enum field)]
                               (enum-codec codes (clojure.set/map-invert codes)))
          (= coerce str) (when-let [statuses (:gui-data-slot-status-codes field)]
                           (string-status-codec statuses))
          :else nil))))

(defn encodable-gui-field?
  [field]
  (some? (codec-for-gui-field field)))

(defn encode-value
  [codec value]
  (clamp-int ((:encode codec) value)))

(defn decode-value
  [codec int-value]
  ((:decode codec) (int int-value)))
