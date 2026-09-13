(ns cn.li.mcbase.entity.hook-abstraction
  "Platform-agnostic entity operation function maps.

   Defines the expected function keys for entity adapter maps, allowing:
   - Unit testing of entity hooks with mock implementations
   - Easy porting to new Minecraft versions
   - Clear separation of business logic from platform details

   Each entity type documents its required function keys as a set.
   Adapter maps are plain Clojure maps where each key is an operation
   (e.g. :get-pos) and each value is a function that takes the
   Minecraft entity as its first argument.

   Usage example:
     (let [adapter (create-entity-adapter mc-entity ctx)]
       ((:get-pos adapter) (:minecraft-entity adapter)))")

;; ============================================================================
;; Key Sets — documented function names for each entity type
;; ============================================================================

(def entity-keys
  "Set of IEntity operation keys.
   Adapter maps containing these keys can answer position, velocity,
   rotation, world, and entity-type queries."
  #{:get-pos :get-eye-pos :get-feet-pos
    :set-pos! :move-relative!
    :get-velocity :set-velocity! :apply-velocity-impulse!
    :get-rotation :set-rotation!
    :get-level
    :is-in-water? :is-on-ground? :is-wet?
    :get-entity-type :is-player? :is-living?
    :get-name :get-uuid :get-tag :set-tag!})

(def living-entity-keys
  "Set of ILivingEntity operation keys (in addition to entity-keys).
   Adapter maps containing these keys can answer health, effect,
   and attribute queries."
  #{:get-health :set-health! :get-max-health :hurt!
    :add-effect! :remove-effect! :has-effect? :get-effects
    :can-breathe-underwater? :is-sprinting? :set-sprinting!})

(def player-keys
  "Set of IPlayer operation keys (in addition to entity-keys + living-entity-keys).
   Adapter maps containing these keys can answer player-specific
   queries like gamemode, experience, inventory, and spawn info."
  #{:get-player-name :get-player-uuid
    :get-gamemode :set-gamemode! :is-creative? :is-survival?
    :can-fly? :is-flying? :set-flying!
    :get-experience :get-experience-level :add-experience!
    :get-inventory
    :get-spawn-dimension :get-spawn-pos})

;; ============================================================================
;; Utility Functions
;; ============================================================================

(defn satisfies-entity-protocol?
  "Check if an object is a map-based entity adapter (has :get-pos key)."
  [obj]
  (and (map? obj) (contains? obj :get-pos)))

(defn satisfies-living-protocol?
  "Check if an object is a map-based living entity adapter (has :get-health key)."
  [obj]
  (and (map? obj) (contains? obj :get-health)))

(defn satisfies-player-protocol?
  "Check if an object is a map-based player adapter (has :get-player-name key)."
  [obj]
  (and (map? obj) (contains? obj :get-player-name)))

(defn safe-cast-living
  "Cast to living entity adapter if possible, otherwise returns nil."
  [entity]
  (when (satisfies-living-protocol? entity)
    entity))

(defn safe-cast-player
  "Cast to player adapter if possible, otherwise returns nil."
  [entity]
  (when (satisfies-player-protocol? entity)
    entity))
