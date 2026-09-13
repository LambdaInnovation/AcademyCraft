(ns cn.li.ac.wireless.gui.container.schema-runtime
  "Schema-driven container/runtime helpers for AC GUI implementations.

  This namespace replaces cross-module usage of mcmod.gui.schema-builders,
  keeping GUI schema runtime logic inside AC where concrete container behavior
  is defined."
  (:require [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.mcmod.gui.container.data-slot-codec :as data-slot-codec]
            [cn.li.mcmod.gui.container.data-slot-specs :as data-slot-specs]))

(defn- resolve-state
  [tile]
  (or (common/get-tile-state tile) {}))

(defn- present-value
  [state k]
  (let [v (get state k ::missing)]
    (when (not= ::missing v)
      v)))

(defn- normalize-gui-state
  [schema state]
  (reduce
   (fn [state* field]
     (let [k (:key field)]
       (if (nil? (present-value state* k))
         (if (contains? field :default)
           (assoc state* k (:default field))
           (dissoc state* k))
         state*)))
   (or state {})
   schema))

(defn- raw-gui-value
  [field state]
  (let [state* (normalize-gui-state [field] state)
        k (:key field)]
    (cond
      (some? (present-value state* k)) (get state* k)
      (:gui-init field) ((:gui-init field) state*)
      (contains? field :default) (:default field)
      :else nil)))

(defn- coerce-gui-value
  [field value]
  (let [coerce-fn (or (:gui-coerce field) identity)]
    (if (and (nil? value) (:gui-coerce field))
      (throw (ex-info "GUI schema field requires non-nil value before coercion"
                      {:field-key (:key field)
                       :gui-container-key (:gui-container-key field (:key field))}))
      (coerce-fn value))))

(defn build-gui-atoms
  [schema state]
  (let [state* (normalize-gui-state schema state)]
    (into {}
          (for [field schema
                :when (or (:gui-sync? field) (:gui-only? field))
                :let [k (:gui-container-key field (:key field))]]
            [k (atom (coerce-gui-value field (raw-gui-value field state*)))]))))

(defn build-sync-to-client-fn
  [schema]
  (fn [container]
    (let [state (normalize-gui-state schema (resolve-state (:tile-entity container)))]
      (doseq [field schema
              :when (:gui-sync? field)
              :let [container-key (:gui-container-key field (:key field))
                    value (raw-gui-value field state)]]
        (when-let [atom-ref (get container container-key)]
          (let [new-val (coerce-gui-value field value)]
            (when (not= @atom-ref new-val)
              (reset! atom-ref new-val))))))))

(defn build-on-close-fn
  [schema]
  (fn [container]
    (doseq [field schema
            :when (contains? field :gui-close-reset)
            :let [k (:gui-container-key field (:key field))
                  reset-val (:gui-close-reset field)]]
      (when-let [atom-ref (get container k)]
        (reset! atom-ref reset-val)))))

(defn build-data-slot-field-specs
  "Build ordered DataSlot specs for encodable :gui-data-slot? / :gui-sync? fields."
  [schema & {:keys [gui-id max-slots include-tab?]
             :or {max-slots data-slot-specs/default-max-data-slots
                  include-tab? false}}]
  (let [field-key (fn [f] (or (:gui-container-key f) (:key f)))
        has-tab-in-schema? (some #(= :tab-index (field-key %)) schema)
        reserve-tab? (and include-tab? (not has-tab-in-schema?))
        base-specs (data-slot-specs/build-field-specs schema
                                                    :gui-id gui-id
                                                    :max-slots (- max-slots (if reserve-tab? 1 0)))
        tab-spec (when reserve-tab?
                   {:field-key :tab-index
                    :container-key :tab-index
                    :codec (data-slot-codec/int-codec)
                    :sort-key "000-tab-index"})]
    (if tab-spec
      (into [tab-spec] base-specs)
      base-specs)))

(defn build-server-menu-sync!
  "Lightweight server-only refresh: tile state -> container atoms. No network I/O."
  [schema]
  (let [sync-from-tile* (build-sync-to-client-fn schema)]
    (fn server-menu-sync! [container]
      (sync-from-tile* container))))

(defn attach-data-slot-specs!
  [container schema & opts]
  (assoc container
         :data-slot-field-specs (apply build-data-slot-field-specs schema opts)))
