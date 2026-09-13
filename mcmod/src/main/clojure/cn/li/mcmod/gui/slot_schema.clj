(ns cn.li.mcmod.gui.slot-schema
  "Slot schema DSL and query API.

  A slot schema is the single source of truth for a GUI's tile slots.
  Slot indexes, ranges, and layout metadata are derived from declaration order."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]))

;; Slot Schema — Framework [:registry :slots :schemas]

(def ^:private schema-path [:registry :slots :schemas])

(defn- slot-schema-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom schema-path {:slot-schema-registry {}})
    {:slot-schema-registry {}}))

(defn set-slot-schema-registry!
  "Replace all slot schemas. Intended for tests."
  [registry]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in (conj schema-path :slot-schema-registry) (or registry {})))
  nil)

(defn- ensure-keyword
  [value label]
  (when-not (keyword? value)
    (throw (ex-info (str label " must be a keyword") {label value})))
  value)

(defn- ensure-int
  [value label]
  (when-not (integer? value)
    (throw (ex-info (str label " must be an integer") {label value})))
  value)

(defn- normalize-slot
  [idx slot]
  (let [{:keys [id type x y can-place]} slot]
    {:id (ensure-keyword id ":slot/id")
     :type (ensure-keyword type ":slot/type")
     :x (ensure-int x ":slot/x")
     :y (ensure-int y ":slot/y")
     :index idx
     ;; Optional (fn [ItemStack] -> boolean) restricting what may be placed.
     ;; DynamicSlot.mayPlace overrides the container's canPlaceItem, so the
     ;; GUI's :slot-can-place-fn is never consulted on click — without this
     ;; predicate any item can enter a typed input slot (e.g. a crystal into
     ;; the liquid-unit slot, where the machine logic then consumes it).
     :can-place can-place}))

(defn- derived-ranges
  [tile-slot-count]
  {:tile [0 (dec tile-slot-count)]
   :player-main [tile-slot-count (+ tile-slot-count 26)]
   :player-hotbar [(+ tile-slot-count 27) (+ tile-slot-count 35)]})

(defn- validate-slots!
  [schema-id slots]
  (let [ids (map #(get % :id) slots)
        duplicate-ids (->> ids frequencies (filter (fn [[_id n]] (> n 1))) (map first) seq)]
    (when duplicate-ids
      (throw (ex-info "Slot schema has duplicate slot ids"
                      {:schema-id schema-id
                       :duplicate-ids duplicate-ids}))))
  (let [coords (map #(vector (get % :x) (get % :y)) slots)
        duplicate-coords (->> coords frequencies (filter (fn [[_xy n]] (> n 1))) (map first) seq)]
    (when duplicate-coords
      (throw (ex-info "Slot schema has duplicate slot coordinates"
                      {:schema-id schema-id
                       :duplicate-coordinates duplicate-coords})))))

(defn register-slot-schema!
  "Register a slot schema.

  Config keys:
  - :schema-id keyword
  - :slots [{:id :core :type :core :x 47 :y 24} ...]

  Returns normalized schema with derived fields."
  [{:keys [schema-id slots]}]
  (let [schema-id (ensure-keyword schema-id ":schema-id")
        slots (mapv normalize-slot (range) slots)]
    (validate-slots! schema-id slots)
    (let [tile-slot-count (count slots)
          slot-layout {:slots (mapv (fn [{:keys [type index x y can-place]}]
                                      {:type type :index index :x x :y y
                                       :can-place can-place})
                                    slots)
                       :ranges (derived-ranges tile-slot-count)}
          schema {:schema-id schema-id
                  :slots slots
                  :slot-layout slot-layout
                  :tile-slot-count tile-slot-count
                  :slot-id->index (into {} (map #(vector (get % :id) (get % :index)) slots))
                  :slot-index->slot (into {} (map #(vector (get % :index) %) slots))
                  :slot-type->indexes (into {}
                                            (for [[slot-type grouped] (group-by #(get % :type) slots)]
                                              [slot-type (mapv #(get % :index) grouped)]))}]
      (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in (conj schema-path :slot-schema-registry schema-id) schema))
      (log/debug "Registered slot schema:" schema-id)
      schema)))

(defmacro defslot-schema
  "Define and register a slot schema.

  Example:
  (defslot-schema example-processor
    :schema-id :example-processor
    :slots [{:id :plate-a :type :plate :x 0 :y 0}])"
  [schema-name & options]
  (let [options-map (apply hash-map options)
        schema-id (or (:schema-id options-map) (keyword (name schema-name)))]
    `(def ~schema-name
       (register-slot-schema! ~(assoc options-map :schema-id schema-id)))))

(defn get-slot-schema
  [schema-id]
  (get-in (slot-schema-state-snapshot) [:slot-schema-registry schema-id]))

(defn get-slot-layout
  [schema-id]
  (some-> (get-slot-schema schema-id) :slot-layout))

(defn tile-slot-count
  [schema-id]
  (or (some-> (get-slot-schema schema-id) :tile-slot-count) 0))

(defn slot-index
  [schema-id slot-id]
  (get-in (get-slot-schema schema-id) [:slot-id->index slot-id]))

(defn slot-id
  [schema-id slot-index]
  (get-in (get-slot-schema schema-id) [:slot-index->slot slot-index :id]))

(defn slot-type
  [schema-id slot-index]
  (get-in (get-slot-schema schema-id) [:slot-index->slot slot-index :type]))

(defn slot-type?
  [schema-id slot-index expected-type]
  (= (slot-type schema-id slot-index) expected-type))

(defn slot-indexes-by-type
  [schema-id slot-type]
  (get-in (get-slot-schema schema-id) [:slot-type->indexes slot-type] []))

(defn all-slot-indexes
  [schema-id]
  (->> (get-slot-schema schema-id)
       :slots
       (mapv #(get % :index))))

(defn slot-indexes
  [schema-id slot-ids]
  (->> slot-ids
       (mapv (fn [slot-id']
               (let [idx (slot-index schema-id slot-id')]
                 (when-not (some? idx)
                   (throw (ex-info "Unknown slot id in slot-indexes"
                                   {:schema-id schema-id
                                    :slot-id slot-id'})))
                 idx)))))

(defn get-slot-range
  [schema-id section]
  (get-in (get-slot-layout schema-id) [:ranges section] [0 0]))

(defn build-quick-move-config
  "Compile semantic quick-move rules to index-based config.

  Input config:
  - :inventory-pred (fn [slot-index player-inventory-start])
  - :rules [{:accept? fn :slot-ids [:core]}
            {:accept? fn :slot-type :plate}]"
  [schema-id {:keys [inventory-pred rules]}]
  {:container-slots (set (all-slot-indexes schema-id))
   :inventory-pred inventory-pred
   :rules (mapv (fn [{:keys [accept? slot-ids slot-type]}]
                  {:accept? accept?
                   :slots (cond
                            slot-ids (slot-indexes schema-id slot-ids)
                            slot-type (slot-indexes-by-type schema-id slot-type)
                            :else [])})
                rules)})
