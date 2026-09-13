(ns cn.li.ac.persistence.nbt-collections
  "Native-NBT collection helpers for AC-owned persistence schemas."
  (:require [cn.li.mcmod.platform.structured-data :as sd]))

(defn write-keyword-set! [compound key values]
  (let [result (sd/create-list)]
    (doseq [value values]
      (let [entry (sd/create-structured)]
        (sd/set-string! entry "v" (if-let [ns-part (namespace value)]
                                          (str ns-part "/" (name value))
                                          (name value)))
        (sd/append! result entry)))
    (sd/set-entry! compound key result)))

(defn read-keyword-set [compound key]
  (let [values (sd/get-list compound key)
        size (long (or (some-> values sd/list-size) 0))]
    (loop [index 0 result (transient #{})]
      (if (< index size)
        (let [entry (sd/list-structured values index)]
          (recur (unchecked-inc-int index)
                 (conj! result (keyword (sd/get-string entry "v")))))
        (persistent! result)))))

(defn write-int-set! [compound key values]
  (let [result (sd/create-list)]
    (doseq [value values]
      (let [entry (sd/create-structured)]
        (sd/set-int! entry "v" (int value))
        (sd/append! result entry)))
    (sd/set-entry! compound key result)))

(defn read-int-set [compound key]
  (let [values (sd/get-list compound key)
        size (long (or (some-> values sd/list-size) 0))]
    (loop [index 0 result (transient #{})]
      (if (< index size)
        (let [entry (sd/list-structured values index)]
          (recur (unchecked-inc-int index)
                 (conj! result (int (sd/get-int entry "v")))))
        (persistent! result)))))
