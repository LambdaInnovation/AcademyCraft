(ns cn.li.mcmod.block.blockstate-properties
  "Shared blockstate property registry logic for platform adapters."
  (:require [cn.li.mcmod.util.log :as log]))

(defn create-property-registry []
  (atom {}))

(defn create-property
  [property-key property-config create-integer-fn create-boolean-fn create-facing-fn]
  (let [prop-type (:type property-config)
        prop-name (or (:name property-config) (name property-key))]
    (case prop-type
      :integer (let [min-val (:min property-config 0)
                     max-val (:max property-config 15)]
                 (create-integer-fn prop-name min-val max-val))
      :boolean (create-boolean-fn prop-name)
      :horizontal-facing (when create-facing-fn
                           (create-facing-fn prop-name))
      :direction (when create-facing-fn
                   (create-facing-fn prop-name))
      (do (log/warn "Unknown property type:" prop-type)
          nil))))

(defn register-block-properties!
  ([registry block-id block-state-properties create-integer-fn create-boolean-fn]
   (register-block-properties! registry block-id block-state-properties create-integer-fn create-boolean-fn nil))
  ([registry block-id block-state-properties create-integer-fn create-boolean-fn create-facing-fn]
   (when block-state-properties
     (doseq [[prop-key prop-config] block-state-properties]
       (let [property (create-property prop-key prop-config create-integer-fn create-boolean-fn create-facing-fn)]
         (when property
           (swap! registry assoc [block-id prop-key] property)
           (log/debug "Registered property" (name prop-key) "for block" block-id)))))))

(defn get-property
  [registry block-id property-key]
  (get @registry [block-id property-key]))

(defn get-all-properties
  [registry block-id]
  (let [props (filter (fn [[[bid _] _]] (= bid block-id)) @registry)]
    (mapv second props)))
