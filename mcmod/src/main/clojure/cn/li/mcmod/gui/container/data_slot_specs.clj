(ns cn.li.mcmod.gui.container.data-slot-specs
  "Registration-time DataSlot field specs and budget validation."
  (:require [cn.li.mcmod.gui.container.data-slot-codec :as codec]))

(def default-max-data-slots 16)

(defn- field-container-key
  [field]
  (or (:gui-container-key field) (:key field)))

(defn- field-sort-key
  [field]
  (or (:data-slot-order field)
      (name (field-container-key field))))

(defn data-slot-field?
  "True when the field participates in vanilla ContainerData sync."
  [field]
  (if (contains? field :gui-data-slot?)
    (:gui-data-slot? field)
    (and (:gui-sync? field) (codec/encodable-gui-field? field))))

(defn menu-sync-field?
  "True when server-menu-sync! may refresh this field from tile state."
  [field]
  (boolean (:gui-sync? field)))

(defn build-field-specs
  "Build ordered DataSlot field specs from GUI schema fields."
  [schema & {:keys [max-slots gui-id]
             :or {max-slots default-max-data-slots}}]
  (let [fields (vec (sort-by field-sort-key
                             (filter data-slot-field? schema)))
        count (count fields)]
    (when (> count max-slots)
      (throw (ex-info "GUI DataSlot field budget exceeded"
                      {:gui-id gui-id
                       :count count
                       :max-slots max-slots
                       :fields (mapv field-container-key fields)})))
    (mapv (fn [field]
            (let [container-key (field-container-key field)
                  field-codec (codec/codec-for-gui-field field)]
              (when-not field-codec
                (throw (ex-info "DataSlot field missing codec"
                                {:gui-id gui-id
                                 :field-key (:key field)
                                 :container-key container-key})))
              {:field-key (:key field)
               :container-key container-key
               :codec field-codec
               :sort-key (field-sort-key field)}))
          fields)))

(defn validate-schema-data-slots!
  [schema & opts]
  (build-field-specs schema opts)
  nil)

(defn spec-container-keys
  [specs]
  (mapv #(get % :container-key) specs))
