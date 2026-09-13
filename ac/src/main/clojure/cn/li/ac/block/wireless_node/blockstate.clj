(ns cn.li.ac.block.wireless-node.blockstate
  "BlockState definitions for wireless node blocks (datagen metadata).

  Generates BlockStateDefinition records for all node types (basic/standard/advanced)
  with multipart models for energy levels and connection states.

  This file contains node-specific blockstate logic extracted from blockstate_definition.clj
  to colocate it with the node block implementation."
  (:require [clojure.string :as str]
            [cn.li.ac.block.wireless-node.schema :as node-schema]
            [cn.li.mcmod.config :as mcmod-config]))

;; ============================================================================
;; BlockState Definition Record (copied from blockstate_definition.clj)
;; ============================================================================

(defrecord BlockStateDefinition
  [;; block的minecraft registry name (不含命名空间)
   registry-name
   ;; BlockState属性定义
   properties
   ;; multipart格式的部分列表
   parts])

;; ============================================================================
;; Node BlockState Generation
;; ============================================================================

(def ^:private node-tiers
  "Static tier list for datagen/blockstate metadata.

  Keep this namespace pure: do not read runtime wireless config here.
  Runtime capacities/energy behavior belong to logic/config namespaces;
  blockstate/model shape metadata only needs tier identifiers."
  [:basic :standard :advanced])

(defn- node-types* []
  node-tiers)

(defn- blockstate-property-fields* []
  node-schema/blockstate-property-fields)

(defn- mod-id []
  mcmod-config/mod-id)

(defn- extract-blockstate-props
  "Extract BlockState properties from schema into a map.
   Returns: {property-keyword {:name :type :min :max :default}}"
  []
  (into {}
    (for [field (blockstate-property-fields*)
          :when (:block-state field)
          :let [bs (:block-state field)
                prop-name (:prop bs)]]
      [(keyword prop-name)
       {:name prop-name
        :type (:type bs)
        :min (:min bs)
        :max (:max bs)
        :default (:default bs)}])))

(defn- node-state-part
  "One mutually-exclusive multipart entry for (energy, connected)."
  [registry-name level connected?]
  {:condition {:energy (str level)
               :connected (str connected?)}
   :models [(str (mod-id)
                 ":block/"
                 registry-name
                 "_energy_"
                 level
                 (when connected? "_connected"))]})

(defn- node-state-parts
  "Eager (energy × connected) parts — avoids nested `for` class-name stacking."
  [registry-name energy-min energy-max]
  (persistent!
   (reduce (fn [out level]
             (reduce (fn [out' connected?]
                       (conj! out' (node-state-part registry-name level connected?)))
                     out
                     [false true]))
           (transient [])
           (range energy-min (inc energy-max)))))

(defn get-all-node-definitions
  "Generate BlockStateDefinition records for all node types.

  Reads node-types from wireless-node/block.clj and BlockState properties
  from schema.clj to maintain single source of truth.

  Returns:
    map of block-key -> BlockStateDefinition"
  []
  (let [;; Extract BlockState properties from schema
        props (extract-blockstate-props)
        energy-min (get-in props [:energy :min])
        energy-max (get-in props [:energy :max])
        connected-type (get-in props [:connected :type])]
    ;; IMPORTANT:
    ;; Keep node parts mutually-exclusive. Using an unconditional base part
    ;; plus conditional full-cube parts causes model stacking/z-fighting,
    ;; which makes runtime appearance look "stuck" to one shape.
    ;;
    ;; We instead generate one model per (energy, connected) tuple.
    (into {}
          (map (fn [node-type]
                 (let [node-type-name (name node-type)
                       block-key (keyword (str "wireless-node-" node-type-name))
                       registry-name (str "node_" node-type-name)
                       state-models (node-state-parts registry-name energy-min energy-max)]
                   [block-key
                    (BlockStateDefinition.
                     registry-name
                     {:energy {:min energy-min :max energy-max}
                      :connected {:type connected-type}}
                     state-models)]))
               (node-types*)))))

(defn get-node-blockstate-definition
  "Get BlockState definition for a specific node block.

  Parameters:
    block-key: keyword like :wireless-node-basic

  Returns:
    BlockStateDefinition or nil"
  [block-key]
  (get (get-all-node-definitions) block-key))

;; ============================================================================
;; Node Model Name Parsing
;; ============================================================================

(defn- node-model-pattern
  "Generate regex pattern for node model names based on node-types.

  Returns:
    regex pattern matching node model names like 'node_basic_base'"
  []
  (let [node-type-str (str "(" (str/join "|" (map name (node-types*))) ")")
      variant-str   "(base|connected|energy_\\d+(?:_connected)?)"]
    (re-pattern (str "node_" node-type-str "_" variant-str))))

(defn- parse-node-model-name
  "Parse node model name into components.

  Parameters:
    model-name: string like 'node_basic_base' or 'node_advanced_energy_3'

  Returns:
    [node-type variant] or nil if not a node model"
  [model-name]
  (when-let [[_ node-type variant] (re-matches (node-model-pattern) model-name)]
    [node-type variant]))

(defn- variant->energy-level
  "Convert variant string to energy level for texture selection.

  Parameters:
    variant: string like 'base', 'connected', or 'energy_3'

  Returns:
    integer energy level (0-4)"
  [variant]
  (let [raw (cond
              (#{"base" "connected"} variant) 0
        (str/starts-with? variant "energy_")
        (let [n (subs variant 7)
          suffix-idx (str/index-of n "_connected")
          digits (if suffix-idx (subs n 0 suffix-idx) n)]
          (Integer/parseInt digits))
              :else 0)
        ;; Extract min/max from schema
        {:keys [min max]} (get (extract-blockstate-props) :energy)
        min-v (or min 0)
        max-v (or max 4)]
    (clojure.core/max min-v (clojure.core/min max-v raw))))

;; ============================================================================
;; Node Model Texture Configuration
;; ============================================================================

(defn get-node-model-texture-config
  "Get texture configuration for node model variants.

  This function contains node-specific texture logic:
  - Energy levels affect side texture
  - Connected state affects top texture

  Parameters:
    model-name: model name without namespace, like 'node_basic_base'

  Returns:
    {:side \"texture-path\" :vert \"texture-path\"} or nil if not a node model"
  [model-name]
  (when-let [[node-type variant] (parse-node-model-name model-name)]
    (let [energy-level (variant->energy-level variant)
          connected?   (or (= variant "connected")
                           (str/ends-with? variant "_connected"))
          top-texture  (if connected?
                        (str (mod-id) ":block/node_top_1")
                        (str (mod-id) ":block/node_top_0"))
          side-texture (str (mod-id) ":block/node_" node-type "_side_" energy-level)]
      {:side side-texture
       :vert top-texture})))

;; ============================================================================
;; Node Block Identification
;; ============================================================================

(defn is-node-block?
  "Check if a block is a wireless node block.

  Parameters:
    registry-name: block registry name

  Returns:
    true if this is a node block (basic/standard/advanced)"
  [registry-name]
  (boolean (re-find #"^node_(basic|standard|advanced)" registry-name)))

(defn get-node-item-model-id
  "Get item model ID for node blocks.

  Node blocks use the _base variant for their item model.

  Parameters:
    mod-id: mod ID string
    registry-name: block registry name

  Returns:
    item model ID string"
  [mod-id registry-name]
  (str mod-id ":block/" registry-name "_base"))

(comment
  ;; Usage examples
  (get-all-node-definitions)
  (get-node-blockstate-definition :wireless-node-basic)
  (get-node-model-texture-config "node_basic_base")
  (get-node-model-texture-config "node_advanced_energy_3")
  (is-node-block? "node_basic")
  (get-node-item-model-id "academycraft" "node_standard"))
