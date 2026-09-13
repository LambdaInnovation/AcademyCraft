(ns cn.li.mcmod.block.query
  "Block query API for registered block specifications.

  Query helpers accept canonical DSL block ids; code that already has a block
  spec should read the spec map directly."
  (:require [clojure.string :as str]
            [cn.li.mcmod.block.dsl-core :as dsl-core]
            [cn.li.mcmod.block.dsl-multiblock :as mb]
            [cn.li.mcmod.block.tile-dsl :as tdsl]))

(defn- normalize-block-id
  [block-id]
  (cond
    (keyword? block-id) (name block-id)
    (some? block-id) (str block-id)
    :else nil))

;; ============================================================================
;; Block Registry Query API
;; ============================================================================

;; Note: The block-registry is stored in block.dsl; this module provides
;; the query interface that other modules should use. This allows the registry
;; to be extended with different storage mechanisms in the future.

(defn- block-registry-state
  []
  (dsl-core/get-block-registry))

(defn get-block-spec
  "Get a block specification by block-id from the registry
   block-id: string or keyword identifier
   Returns: BlockSpec record or nil if not found"
  [block-id]
  (let [id-str (normalize-block-id block-id)]
    (get (block-registry-state) id-str)))

(defn get-block-registry-name
  "Get the Minecraft registry path for a block-id.
   Uses explicit :registry-name when present; otherwise kebab-case -> snake_case."
  [block-id]
  (when-let [id-str (normalize-block-id block-id)]
    (let [block-spec (get-block-spec id-str)
          explicit-name (:registry-name block-spec)]
      (if (and (string? explicit-name) (not (str/blank? explicit-name)))
        explicit-name
        (str/replace id-str #"-" "_")))))

(defn list-all-blocks
  "Get a sequence of all registered block IDs
   Returns: sequence of block-id strings"
  []
  (keys (block-registry-state)))

(defn identify-block-from-registry-name
  "Resolve DSL block-id from Minecraft registry path.
   First tries snake_case -> kebab-case; then falls back to explicit :registry-name match."
  [registry-name]
  (when registry-name
    (let [candidate-id (str/replace registry-name #"_" "-")]
      (or (when (get-block-spec candidate-id) candidate-id)
          (some (fn [block-id]
                  (when (= registry-name (get-block-registry-name block-id))
                    block-id))
                (list-all-blocks))))))

(defn identify-block-from-full-name
  "Resolve DSL block-id from full block name strings, e.g.
   \"Block{academy:demo_block}\", \"academy:demo_block\", or \"demo_block\"."
  [^String block-name]
  (when block-name
    (let [registry-name (cond
                          (.contains block-name "{")
                          (-> block-name
                              (str/split #"[{}]")
                              second
                              (str/split #":")
                              last)

                          (.contains block-name ":")
                          (last (str/split block-name #":"))

                          :else block-name)]
      (identify-block-from-registry-name registry-name))))

(defn controller-parts-block?
  "Return true when the block uses controller+parts multiblock mode."
  [block-id]
  (= :controller-parts (get-in (get-block-spec block-id)
                               [:multi-block :multiblock-mode])))

;; ============================================================================
;; Block Predicate Queries
;; ============================================================================

(defn is-controller-block?
  "Check if a block is a multi-block controller block
   block-id: string or keyword identifier
   Returns: true if this block is a multi-block controller"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (let [multi-block (:multi-block block-spec)
          block-id (normalize-block-id (:id block-spec))
          controller-id (normalize-block-id (:controller-block-id multi-block))]
      (cond
        (= :controller-parts (:multiblock-mode multi-block))
        (and (some? controller-id)
             (= block-id controller-id))

        (:multi-block? multi-block)
        (boolean (:multi-block-master? multi-block))

        :else false))))

(defn is-part-block?
  "Check if a block is part of a multi-block structure
   block-id: string or keyword identifier
   Returns: true if this block is part of a multi-block"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (let [multi-block (:multi-block block-spec)
          block-id (normalize-block-id (:id block-spec))
          part-id (normalize-block-id (:part-block-id multi-block))]
      (cond
        (= :controller-parts (:multiblock-mode multi-block))
        (and (some? part-id)
             (= block-id part-id))

        (:multi-block? multi-block)
        (not (boolean (:multi-block-master? multi-block)))

        :else false))))

(defn is-multi-block?
  "Check if a block is multi-block enabled
   block-id: string or keyword identifier
   Returns: true if block has multi-block config"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (let [multi-block (:multi-block block-spec)]
      (:multi-block? multi-block))))

(defn has-block-entity?
  "Check if a block is bound to a Tile DSL block entity.
   block-id: string or keyword identifier
   Returns: true if Tile DSL maps the block to a tile-id"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (boolean (tdsl/get-tile-id-for-block (:id block-spec)))))

(defn is-light-emitter?
  "Check if a block emits light
   block-id: string or keyword identifier
   Returns: true if block has light level > 0"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (pos? (get-in block-spec [:rendering :light-level] 0))))

;; ============================================================================
;; Block Property Accessors
;; ============================================================================

(defn get-controller-block-id
  "Get the controller block ID for a multi-block structure
   block-id: string or keyword identifier
   Returns: string block ID or nil"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (get-in block-spec [:multi-block :controller-block-id])))

(defn get-part-block-id
  "Get the part block ID for a multi-block structure
   block-id: string or keyword identifier
   Returns: string block ID or nil"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (get-in block-spec [:multi-block :part-block-id])))

(defn get-tile-kind
  "Get the tile entity kind for a block
   block-id: string or keyword identifier
   Returns: keyword tile kind or nil"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (some-> (tdsl/get-tile-id-for-block (:id block-spec))
            tdsl/get-tile
            :tile-kind)))

(defn get-light-level
  "Get the light emission level for a block
   block-id: string or keyword identifier
   Returns: integer 0-15"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (get-in block-spec [:rendering :light-level] 0)))

(defn get-hardness
  "Get the hardness value for a block
   block-id: string or keyword identifier
   Returns: float hardness value"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (get-in block-spec [:physical :hardness] 1.5)))

(defn get-material
  "Get the material type for a block
   block-id: string or keyword identifier
   Returns: keyword material (:stone, :wood, etc.)"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (get-in block-spec [:physical :material] :stone)))

;; ============================================================================
;; Block Structure Queries
;; ============================================================================

(defn get-structure-offsets
  "Get all relative offsets for a multi-block structure
   block-id: string or keyword identifier
   Returns: vector of {:x :y :z :relative-x :relative-y :relative-z :is-origin?} maps or nil"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (let [multi-block (:multi-block block-spec)]
      (when (:multi-block? multi-block)
        (let [origin (or (:multi-block-origin multi-block) {:x 0 :y 0 :z 0})
              positions (if-let [custom-pos (:multi-block-positions multi-block)]
                          (mb/calculate-multi-block-positions custom-pos origin)
                          (mb/calculate-multi-block-positions (:multi-block-size multi-block) origin))]
          positions)))))

(defn get-block-event-handler
  "Get a block event handler function by block id and event type."
  [block-id event-type]
  (when-let [block-spec (get-block-spec block-id)]
    (get-in block-spec [:events event-type])))

(defn has-block-event-handler?
  "Return true when an explicit block event handler exists."
  [block-id event-type]
  (some? (get-block-event-handler block-id event-type)))

(defn has-block-state-properties?
  "Check if a block has custom block state properties
   block-id: string or keyword identifier
   Returns: true if block has block-state-properties defined"
  [block-id]
  (when-let [block-spec (get-block-spec block-id)]
    (boolean (get-in block-spec [:block-state :block-state-properties]))))
