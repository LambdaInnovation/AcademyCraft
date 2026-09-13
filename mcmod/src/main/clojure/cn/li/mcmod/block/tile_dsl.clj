(ns cn.li.mcmod.block.tile-dsl
  "Tile/BlockEntity DSL - declarative BlockEntityType metadata and lifecycle hooks.

  Goals:
  - Allow defining a single tile type bound to multiple blocks.
  - Keep core platform-neutral: stores metadata only; platforms query via registry metadata."
  (:require [clojure.string :as str]
            [cn.li.mcmod.block.tile-kind :as tile-kind]
            [cn.li.mcmod.schema.core :as schema]
            [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Tile Registry — stored in Framework [:registry :tiles]
;; Structure: {:by-id {tile-id -> TileSpec} :block->tile-id {block-id -> tile-id}}

(def ^:private tile-path [:registry :tiles])

(defn- tile-state []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom tile-path)
    {:by-id {} :block->tile-id {}}))

(defrecord TileSpec
  [id
   registry-name
   impl
   blocks
   tile-kind
   tick-fn
   read-nbt-fn
   write-nbt-fn
   container
   capability-keys
   ;; Optional custom BlockEntity supplier. Shape:
   ;; (fn [be-type pos state block-id] -> BlockEntity)
   be-supplier])

(defn- normalize-id
  [x]
  (cond
    (nil? x) nil
    (string? x) x
    (keyword? x) (name x)
    (symbol? x) (name x)
    :else (str x)))

(defn- normalize-block-ids
  [blocks]
  (->> blocks
       (map normalize-id)
       (remove str/blank?)
       vec))

(defn- ^:private non-blank-tile-id? [s]
  (not (str/blank? s)))

(def ^:private tile-id-schema
  [:and string? [:fn non-blank-tile-id?]])

(def ^:private tile-impl-schema
  keyword?)

(def ^:private tile-blocks-schema
  [:and
   [:vector string?]
   [:fn seq]])

(def ^:private tile-id-validator (schema/lazy-validator tile-id-schema))
(defn- valid-tile-id? [x]
  (schema/valid? (tile-id-validator) x))
(def ^:private tile-impl-validator (schema/lazy-validator tile-impl-schema))
(defn- valid-tile-impl? [x]
  (schema/valid? (tile-impl-validator) x))
(def ^:private tile-blocks-validator (schema/lazy-validator tile-blocks-schema))
(defn- valid-tile-blocks? [x]
  (schema/valid? (tile-blocks-validator) x))

(defn validate-tile-spec
  [{:keys [id impl blocks] :as spec}]
  (when-not (valid-tile-id? id)
    (throw (ex-info "TileSpec must have non-empty string :id" {:spec spec})))
  (when-not (valid-tile-impl? impl)
    (throw (ex-info "TileSpec :impl must be a keyword" {:id id :impl impl})))
  (when-not (valid-tile-blocks? blocks)
    (throw (ex-info "TileSpec :blocks must be a non-empty vector of block-id strings"
                    {:id id :blocks blocks})))
  true)

(defn create-tile-spec
  "Create a TileSpec from options.

  Required options:
  - :impl   keyword (e.g. :scripted)
  - :blocks vector of block-ids (strings/keywords/symbols accepted)

  Optional:
  - :registry-name string override (snake_case) used by platform registration
  - :tile-kind keyword
  - :tick-fn/:read-nbt-fn/:write-nbt-fn
  - :container map of WorldlyContainer hook fns (compiled at registration)
  - :capability-keys set of capability keywords for this tile
  - :be-supplier custom BlockEntity supplier (advanced)"
  [tile-id options]
  (let [blocks (normalize-block-ids (:blocks options))
        cap-keys (when-let [ck (:capability-keys options)]
                   (set (map #(if (keyword? %) % (keyword %)) ck)))
        spec (map->TileSpec
              {:id tile-id
               :registry-name (:registry-name options)
               :impl (or (:impl options) :scripted)
               :blocks blocks
               :tile-kind (:tile-kind options)
               :tick-fn (:tick-fn options)
               :read-nbt-fn (:read-nbt-fn options)
               :write-nbt-fn (:write-nbt-fn options)
               :container (:container options)
               :capability-keys cap-keys
               :be-supplier (:be-supplier options)})]
    (validate-tile-spec spec)
    spec))

(defn register-tile!
  "Register a TileSpec.

Enforces:
- tile-id unique
- a block-id can only belong to one tile-id"
  [tile-spec]
  (validate-tile-spec tile-spec)
  (let [{:keys [id blocks]} tile-spec
        fw (fw/fw-atom)]
    (when-let [fw-atom fw]
      (swap! fw-atom update-in tile-path
             (fn [{:keys [by-id block->tile-id] :as reg}]
               (when (contains? by-id id)
                 nil)
               (doseq [block-id blocks
                       :let [existing (get block->tile-id block-id)]
                       :when existing]
                 (when-not (= existing id)
                   (when-not *compile-files*
                     (throw (ex-info "Block is already bound to a tile-id"
                                     {:block-id block-id
                                      :existing-tile-id existing
                                      :new-tile-id id})))))
               ;; Merge defaults to ensure :by-id and :block->tile-id are maps
               ;; even when reg is {} (empty map from framework init).
               ;; (or reg defaults) fails because {} is truthy in Clojure.
               (let [reg (merge {:by-id {} :block->tile-id {}} reg)]
                 (-> reg
                     (assoc-in [:by-id id] tile-spec)
                     (update :block->tile-id
                             into
                             (map (fn [b] [b id]) blocks))))))
      (log/debug "Registered tile" id "for blocks" blocks))
    tile-spec))

(defn get-tile
  [tile-id]
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom (conj tile-path :by-id (normalize-id tile-id)))
    nil))

(defn list-tiles
  []
  (keys (:by-id (tile-state))))

(defn get-tile-id-for-block
  [block-id]
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom (conj tile-path :block->tile-id (normalize-id block-id)))
    nil))

(defn snapshot-tiles-by-id
  "Read-only snapshot of {:tile-id TileSpec} for bundle compilation."
  []
  (:by-id (tile-state)))

(defn register-tile-capability-keys!
  "Associate capability keywords with a tile spec (declaration phase only)."
  [tile-id & cap-keys]
  (let [normalized (map #(if (keyword? %) % (keyword %)) cap-keys)]
    (when-let [fw-atom (fw/fw-atom)]
      (swap! fw-atom update-in tile-path
             #(update-in (or % {:by-id {} :block->tile-id {}})
                         [:by-id tile-id :capability-keys]
                         (fn [existing]
                           (into (or existing #{}) normalized)))))
    nil))

(defmacro deftile
  "Define and register a tile spec.

  Example:
  (deftile content-node-tile
    :id \"content-node\"
    :impl :scripted
    :blocks [\"content-node-basic\" \"content-node-standard\"]
    :tile-kind :content-node
    :tick-fn my-tick
    :read-nbt-fn my-read
    :write-nbt-fn my-write)"
  [tile-name & options]
  (let [options-map (apply hash-map options)
        tile-id (or (:id options-map) (name tile-name))
        options-map (dissoc options-map :id)]
    `(def ~tile-name
       (register-tile!
         (create-tile-spec ~tile-id ~options-map)))))

(defmacro deftile-kind
  "Register reusable tile-kind defaults in `cn.li.mcmod.block.tile-kind`."
  [tile-kind & options]
  (let [options-map (apply hash-map options)]
    `(tile-kind/register-tile-kind! ~tile-kind ~options-map)))
