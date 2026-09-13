(ns cn.li.mcbase.integration.event-helpers-core
  "Shared event helper functions for runtime checks and data construction.

  These helpers eliminate duplication of common event processing patterns
  between Forge and Fabric event handlers."
  (:require [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.world.entity.player Player]))

(defn get-player-uuid
  "Extract UUID from a player object as a string."
  [^Player player]
  (str (.getUUID player)))

(defn runtime-activated?
  "Check if a player has runtime mode activated.

  Shared Minecraft code delegates the business state decision to the installed
  runtime hook."
  [player]
  (power-runtime/runtime-activated? (get-player-uuid player)))

(defn build-block-event-data
  "Build a normalized event-data map from block interaction parameters.

  Common fields across Forge/Fabric event models:
  - x, y, z: block position coordinates
  - pos: BlockPos object
  - sneaking: player shift-key state
  - player: player entity
  - hand: InteractionHand
  - item-stack: player's held item
  - world: Level/World object
  - block: Block object

  Additional fields can be merged with (merge result custom-fields)."
  [{:keys [x y z pos sneaking player hand item-stack world block]}]
  {:x x
   :y y
   :z z
   :pos pos
   :sneaking sneaking
   :player player
   :hand hand
   :item-stack item-stack
   :world world
   :block block})

(defn validate-block-event-data
  "Validate that event-data contains required fields for processing.

  Required: :x :y :z :block (all other fields are optional)

  Returns nil if validation fails (for early exit), otherwise returns event-data."
  [event-data]
  (if (and event-data
           (map? event-data)
           (contains? event-data :x)
           (contains? event-data :y)
           (contains? event-data :z)
           (contains? event-data :block))
    event-data
    (do
      (log/warn "Invalid event-data structure:" event-data)
      nil)))

(defn make-platform-adapter
  "Factory function to create a platform-specific event wrapper.

  Args:
  - event-name: String name of the event (for logging)
  - event-unpacker: Function that takes platform event object and returns event-data map
  - event-handler: Function that takes event-data map and returns result
  - runtime-check?: Optional boolean, if true checks runtime-activated? first
  - on-runtime-active: Optional function to call if runtime is activated

  Returns: Function suitable for use as Forge/Fabric event callback"
  [event-name event-unpacker event-handler & {:keys [runtime-check? on-runtime-active]}]
  (fn [platform-event]
    (try
      (let [event-data (event-unpacker platform-event)]
        (when-let [validated-data (validate-block-event-data event-data)]
          (if (and runtime-check? (runtime-activated? (:player validated-data)))
            (when on-runtime-active
              (on-runtime-active platform-event validated-data))
            (event-handler validated-data))))
      (catch Throwable t
        (log/stacktrace (str "Error handling " event-name " event") t)))))
