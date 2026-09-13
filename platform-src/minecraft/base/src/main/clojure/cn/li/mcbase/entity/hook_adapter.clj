(ns cn.li.mcbase.entity.hook-adapter
  "Adapter factory functions converting Minecraft entities to plain map adapters.

   This module bridges between platform-specific Minecraft code and the abstract
   entity operation maps, allowing hooks to work with platform-agnostic interfaces.

   Each platform (Forge, Fabric) provides concrete platform-context maps whose keys
   are operation names (e.g. :get-pos) and values are functions that take the
   Minecraft entity as the first argument.

   Usage:
     (let [adapter (adapt-entity mc-entity ctx)]
       ((:get-pos adapter) (:minecraft-entity adapter)))")

;; ============================================================================
;; Factory Functions
;; ============================================================================

(defn create-entity-adapter
  "Create an entity adapter map from a Minecraft entity and platform context.

   The platform-context should contain IEntity operation functions keyed by
   name (e.g. :get-pos, :set-pos!). Each function takes the Minecraft entity
   as its first argument.

   Returns a plain map merging platform-context with :minecraft-entity."
  [minecraft-entity platform-context]
  (merge platform-context {:minecraft-entity minecraft-entity}))

(defn create-living-entity-adapter
  "Create a living entity adapter map from a Minecraft living entity and
   platform context.

   The platform-context should contain both IEntity and ILivingEntity operation
   functions (e.g. :get-pos, :get-health). Each function takes the Minecraft
   entity as its first argument.

   Returns a plain map merging platform-context with :minecraft-entity."
  [minecraft-entity platform-context]
  (merge platform-context {:minecraft-entity minecraft-entity}))

(defn create-player-adapter
  "Create a player adapter map from a Minecraft player entity and
   platform context.

   The platform-context should contain IEntity, ILivingEntity, and IPlayer
   operation functions. Each function takes the Minecraft entity as its
   first argument.

   Returns a plain map merging platform-context with :minecraft-player,
   and overriding :is-player? to always return true."
  [minecraft-player platform-context]
  (merge platform-context
         {:minecraft-player minecraft-player
          :is-player? (constantly true)}))

;; ============================================================================
;; Public API — stable entry points
;; ============================================================================

(defn adapt-entity
  "Convert Minecraft entity to an entity adapter map.
   Deprecated: prefer create-entity-adapter directly."
  [minecraft-entity platform-context]
  (create-entity-adapter minecraft-entity platform-context))

(defn adapt-living-entity
  "Convert Minecraft living entity to a living entity adapter map.
   Deprecated: prefer create-living-entity-adapter directly."
  [minecraft-entity platform-context]
  (create-living-entity-adapter minecraft-entity platform-context))

(defn adapt-player
  "Convert Minecraft ServerPlayer to a player adapter map.
   Deprecated: prefer create-player-adapter directly."
  [minecraft-player platform-context]
  (create-player-adapter minecraft-player platform-context))
