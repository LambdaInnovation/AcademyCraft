(ns cn.li.mcmod.block.dsl-core
  "Block DSL core - block specification, registration, and main macros.
   Provides the primary interface for defining blocks declaratively."
  (:require [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.block.dsl-properties :as props]
            [cn.li.mcmod.block.dsl-validators :as validators]
            [cn.li.mcmod.framework :as fw]))

;; ============================================================================
;; Block Specification Record
;; ============================================================================

(defrecord BlockSpec
  [id registry-name properties
  physical rendering block-state events multi-block loot]
  ;; Complete block specification with nested configuration groups.
  ;;
  ;; Core identity fields:
  ;; - id: Unique string identifier for this block
  ;; - registry-name: Registry name for platform registration
  ;; - properties: Additional custom properties map
  ;;
  ;; Nested configuration groups:
  ;; - physical: PhysicalProperties record
  ;; - rendering: RenderingProperties record
  ;; - block-state: BlockStateConfig record
  ;; - events: EventHandlers record
  ;; - multi-block: MultiBlockConfig record
  )

;; ============================================================================
;; Block Registry
;; ============================================================================

;; Block Registry — stored in Framework [:registry :blocks]

(defn- block-registry-state []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom [:registry :blocks])
    {}))

(defn get-block-registry
  "Return the active block registry map."
  []
  (block-registry-state))

;; ============================================================================
;; Block Creation
;; ============================================================================

(defn create-block-spec
  "Create a block specification from options.
  Only nested syntax is supported."
  [block-id options]
  (let [physical-opts (:physical options)
        rendering-opts (:rendering options)
        block-state-opts (:block-state options)
        events-opts (:events options)
        multi-block-opts (:multi-block options)

        shorthand-positions (:positions multi-block-opts)
        processed-shorthand-positions (when shorthand-positions
                                        (let [positions (mapv (fn [pos]
                                                                (if (vector? pos)
                                                                  {:x (nth pos 0 0)
                                                                   :y (nth pos 1 0)
                                                                   :z (nth pos 2 0)}
                                                                  pos))
                                                              shorthand-positions)]
                                          (if (some #(and (= (:x %) 0)
                                                          (= (:y %) 0)
                                                          (= (:z %) 0))
                                                    positions)
                                            positions
                                            (into [{:x 0 :y 0 :z 0}] positions))))

        physical (props/map->PhysicalProperties
                   {:material (or (:material physical-opts) :stone)
                    :hardness (or (:hardness physical-opts) props/default-hardness)
                    :resistance (or (:resistance physical-opts) props/default-resistance)
                    :friction (or (:friction physical-opts) props/default-friction)
                    :slip-factor (or (:slip-factor physical-opts) props/default-friction)
                    :sounds (or (:sounds physical-opts) :stone)
                    :harvest-level (or (:harvest-level physical-opts) 0)
                    :harvest-tool (or (:harvest-tool physical-opts) :pickaxe)
                    :requires-tool (boolean (:requires-tool physical-opts))})

        rendering (props/map->RenderingProperties
                    {:model-parent (:model-parent rendering-opts)
                     :textures (:textures rendering-opts)
                     :model-textures (:model-textures rendering-opts)
                     :has-item-form? (if (contains? rendering-opts :has-item-form?)
                                       (:has-item-form? rendering-opts)
                                       true)
                     :flat-item-icon? (boolean (:flat-item-icon? rendering-opts))
                     :creative-tab (or (:creative-tab rendering-opts) props/default-creative-tab)
                     :light-level (or (:light-level rendering-opts) props/default-light-level)
                     :render-shape (:render-shape rendering-opts)})

        block-state (props/map->BlockStateConfig
                      {:block-state-properties (:block-state-properties block-state-opts)})

        events (props/map->EventHandlers
                 {:on-right-click (:on-right-click events-opts)
                  :on-break (:on-break events-opts)
                  :on-place (:on-place events-opts)
                  :on-multi-block-break (:on-multi-block-break events-opts)})

        multi-block? (or (:enabled? multi-block-opts)
                         (:multi-block? multi-block-opts)
                         (some? (:size multi-block-opts))
                         (seq processed-shorthand-positions))
        multi-block-size (:size multi-block-opts)
        multi-block-positions processed-shorthand-positions
        multi-block-origin (or (:origin multi-block-opts)
                               {:x 0 :y 0 :z 0})
        multi-block-rotation-center (:rotation-center multi-block-opts)
        pivot-xz-override (:pivot-xz-override multi-block-opts)
        tesr-use-raw-rotation-center?
        (boolean (:tesr-use-raw-rotation-center? multi-block-opts))
        tesr-y-deg-override (:tesr-y-deg-override multi-block-opts)

        multi-block (props/map->MultiBlockConfig
                      {:multi-block? multi-block?
                       :multi-block-size multi-block-size
                       :multi-block-positions multi-block-positions
                       :multi-block-origin multi-block-origin
                       :multi-block-rotation-center multi-block-rotation-center
                       :multi-block-master? (boolean (:master? multi-block-opts))
                       :multiblock-mode (:mode multi-block-opts)
                       :controller-block-id (:controller-block-id multi-block-opts)
                       :part-block-id (:part-block-id multi-block-opts)
                       :pivot-xz-override pivot-xz-override
                       :tesr-use-raw-rotation-center? tesr-use-raw-rotation-center?
                       :tesr-y-deg-override tesr-y-deg-override})]

    (map->BlockSpec
      {:id block-id
       :registry-name (:registry-name options)
       :properties (or (:properties options) {})
       :physical physical
       :rendering rendering
       :block-state block-state
       :events events
       :multi-block multi-block
       :loot (or (:loot options) {:type :self})})))

;; ============================================================================
;; Registry Management
;; ============================================================================

(defn register-block!
  "Register a block specification. No-op during AOT compilation."
  [block-spec]
  (validators/validate-block-spec block-spec)
  (log/debug "Registering block:" (:id block-spec))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in [:registry :blocks (:id block-spec)] block-spec))
  block-spec)

(defn get-block
  "Get a block specification from the registry by ID."
  [block-id]
  (let [id-str (if (keyword? block-id) (name block-id) block-id)]
    (get-in @(fw/fw-atom) [:registry :blocks id-str])))

(defn list-blocks
  "Get all registered block IDs."
  []
  (if-let [fw-atom (fw/fw-atom)]
    (keys (get-in @fw-atom [:registry :blocks]))
    ()))

;; ============================================================================
;; Main Macro: defblock
;; ============================================================================

(defmacro defblock
  "Define a block with declarative syntax
  
  Example:
  (defblock my-block
    :material :stone
    :hardness 3.0
    :resistance 10.0
    :light-level 15
    :requires-tool true
    :harvest-tool :pickaxe
    :harvest-level 2
    :on-right-click (fn [data] (println \"Clicked!\")))"
  [block-name & options]
  (let [block-id (name block-name)
        options-map (apply hash-map options)]
    `(def ~block-name
       (register-block!
         (create-block-spec ~block-id ~options-map)))))

;; ============================================================================
;; Multi-block Macros
;; ============================================================================

(defmacro defmultiblock
  "Define a controller+part multi-block with shared options.
   
   Example:
   (defmultiblock example-machine
     :multi-block {:positions [[0 0 1] [1 0 1] ...]}
     :common {:physical {:material :stone :hardness 3.0}}
     :controller {:registry-name \"matrix\" :events {:on-place ...}}
     :part {:registry-name \"matrix_part\" :rendering {:has-item-form false}})"
  [base-name & options]
  (let [base-sym (if (and (seq? base-name)
                          (= 'quote (first base-name)))
                   (second base-name)
                   base-name)
        options-map (apply hash-map options)
        multi-block (:multi-block options-map)
        common-opts (or (:common options-map) {})
        raw-controller-opts (or (:controller options-map) {})
        raw-part-opts (or (:part options-map) {})
        controller-name (or (:controller-name options-map) base-sym)
        part-name (or (:part-name options-map)
                      (symbol (str (name base-sym) "-part")))
        controller-id (name controller-name)
        part-id (name part-name)
        ;; Deep merge function for nested maps
        deep-merge (fn deep-merge [& maps]
                     (apply merge-with
                            (fn [x y]
                              (if (and (map? x) (map? y))
                                (deep-merge x y)
                                y))
                            maps))
        merged-controller-opts (deep-merge common-opts
                                           raw-controller-opts
                                           {:multi-block (merge multi-block
                                                                {:mode :controller-parts
                                                                 :controller-block-id controller-id
                                                                 :part-block-id part-id})})
        merged-part-opts (deep-merge common-opts
                                     {:rendering {:has-item-form? false}}
                                     raw-part-opts
                                     {:multi-block {:mode :controller-parts
                                                    :controller-block-id controller-id
                                                    :part-block-id part-id}})]
    `(do
       (defblock ~controller-name ~@(vec (mapcat identity merged-controller-opts)))
       (defblock ~part-name ~@(vec (mapcat identity merged-part-opts))))))

;; ============================================================================
;; Template Support
;; ============================================================================

(def ^:private built-in-templates
  {:content-node {:physical {:material :metal
                              :hardness 3.0
                              :resistance 8.0
                              :requires-tool true
                              :harvest-tool :pickaxe}}
   :energy-gen {:physical {:material :metal
                           :hardness 3.5
                           :resistance 8.0
                           :requires-tool true
                           :harvest-tool :pickaxe}}
   :machine {:physical {:material :metal
                        :hardness 4.0
                        :resistance 10.0
                        :requires-tool true
                        :harvest-tool :pickaxe}}
   :ore {:physical {:material :stone
                    :hardness 3.0
                    :resistance 3.0
                    :requires-tool true
                    :harvest-tool :pickaxe}}})

(defn- deep-merge
  [& maps]
  (apply merge-with
         (fn [x y]
           (if (and (map? x) (map? y))
             (deep-merge x y)
             y))
         maps))

(defn template-options
  "Resolve a block template config into options accepted by `defblock`.

  Config map accepts `:type` plus any regular `defblock` options."
  [{:keys [type] :as config}]
  (let [template (or (get built-in-templates type) {})]
    (-> (deep-merge template (dissoc config :type))
        (dissoc :type))))

(defmacro defblock-template
  "Define a block by merging a built-in template with explicit options."
  [block-name config]
  (let [options-map (template-options config)]
    `(defblock ~block-name ~@(vec (mapcat identity options-map)))))

;; ============================================================================
;; Block Properties Export
;; ============================================================================

(defn get-block-properties
  "Get block properties map for platform-specific creation"
  [block-spec]
  (let [physical (:physical block-spec)
        multi-block (:multi-block block-spec)]
    {:material (:material physical)
     :hardness (:hardness physical)
     :resistance (:resistance physical)
     :light-level (get-in block-spec [:rendering :light-level])
     :requires-tool (:requires-tool physical)
     :sounds (:sounds physical)
     :harvest-level (:harvest-level physical)
     :harvest-tool (:harvest-tool physical)
     :friction (:friction physical)
     :slip-factor (:slip-factor physical)
     :multi-block? (:multi-block? multi-block)
     :multi-block-size (:multi-block-size multi-block)
     :multi-block-positions (:multi-block-positions multi-block)
     :multi-block-origin (:multi-block-origin multi-block)
     :multi-block-rotation-center (:multi-block-rotation-center multi-block)
     :multi-block-master? (:multi-block-master? multi-block)}))
