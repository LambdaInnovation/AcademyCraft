(ns cn.li.mcmod.block.dsl-properties
  "Block physical and rendering properties definitions.
   Provides record types and material/sound type definitions for block properties.")

;; ============================================================================
;; Block Property Record Types
;; ============================================================================

(defrecord PhysicalProperties
  [material hardness resistance friction slip-factor
   sounds harvest-level harvest-tool requires-tool]
  ;; Physical properties of a block: material, hardness, resistance, friction, sounds, harvest requirements.
  ;;
  ;; Fields:
  ;; - material: Material type keyword (:stone, :wood, :metal, :glass, etc.)
  ;; - hardness: How long it takes to break (higher = longer)
  ;; - resistance: Explosion resistance (higher = more resistant)
  ;; - friction: Slipperiness factor (0.6 = normal, lower = more slippery)
  ;; - slip-factor: Alternative name for friction
  ;; - sounds: Sound type keyword (:stone, :wood, :metal, :glass, etc.)
  ;; - harvest-level: Minimum tool tier required (0 = wood, 1 = stone, 2 = iron, 3 = diamond)
  ;; - harvest-tool: Tool type required (:pickaxe, :axe, :shovel, :hoe, :sword)
  ;; - requires-tool: Whether a tool is required to harvest drops
  )

(defrecord RenderingProperties
  [model-parent textures model-textures has-item-form?
  flat-item-icon? creative-tab light-level render-shape]
  ;; Rendering and visual properties of a block: models, textures, lighting, creative tab.
  ;;
  ;; Fields:
  ;; - model-parent: Parent model path for datagen (e.g., "block/cube_all")
  ;; - textures: Texture map for datagen (e.g., {:all "modid:block/texture"})
  ;; - model-textures: Additional model-specific textures
  ;; - has-item-form?: Whether this block should have a BlockItem/item model
  ;; - flat-item-icon?: Use flat 2D item icon instead of 3D block model
  ;; - creative-tab: Creative tab for block items (:misc, :building, :decorations, etc.)
  ;; - light-level: Light emission level (0-15, 0 = no light, 15 = brightest)
  ;; - render-shape: Optional world render shape hint (:model, :invisible, :entityblock-animated)
  )

(defrecord BlockStateConfig
  [block-state-properties]
  ;; Block state properties configuration.
  ;;
  ;; Fields:
  ;; - block-state-properties: Dynamic block state properties (e.g., {:energy IntegerProperty, :connected BooleanProperty})
  )

(defrecord EventHandlers
  [on-right-click on-break on-place on-multi-block-break]
  ;; Event handlers for block interactions.
  ;;
  ;; Fields:
  ;; - on-right-click: (fn [player world pos block-id & {:keys [sneaking item-stack]}] ...)
  ;; - on-break: (fn [world pos block-id] ...)
  ;; - on-place: (fn [player world pos block-id] ...)
  ;; - on-multi-block-break: Handler for multi-block structure break (fn [event-data] ...)
  )

(defrecord MultiBlockConfig
  [multi-block? multi-block-size multi-block-positions multi-block-origin
   multi-block-rotation-center multi-block-master? multiblock-mode
   controller-block-id part-block-id pivot-xz-override
   tesr-use-raw-rotation-center? tesr-y-deg-override]
  ;; Multi-block structure configuration used by the DSL core.
  )

;; ============================================================================
;; Material and Audio Definitions
;; ============================================================================

;; Material types (version-agnostic)
(def materials
  {:stone :stone
   :wood :wood
   :metal :metal
   :glass :glass
   :dirt :dirt
   :sand :sand
   :grass :grass
   :leaves :leaves
   :water :water
   :lava :lava
   :air :air})

;; Sound types
(def sound-types
  {:stone :stone
   :wood :wood
   :metal :metal
   :glass :glass
   :grass :grass
   :sand :sand
   :gravel :gravel})

;; Tool types
(def tool-types
  {:pickaxe :pickaxe
   :axe :axe
   :shovel :shovel
   :hoe :hoe
   :sword :sword})

;; ============================================================================
;; Default Values
;; ============================================================================

(def default-hardness 1.5)
(def default-resistance 6.0)
(def default-light-level 0)
(def default-friction 0.6)
(def default-creative-tab :misc)
