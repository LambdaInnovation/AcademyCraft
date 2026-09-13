(ns cn.li.mcbase.runtime.accessor-registry-world
  "World-domain accessor registrations.
   
   Provides platform-independent accessors for world-related operations:
   - Block queries and manipulation
   - World state access
   - Raycast operations
  - Content-named world positions"
  (:require [cn.li.mcbase.runtime.accessor-registry-core :as core]
            [cn.li.mcmod.runtime.install :as install]))

(defn- register-world-accessors-impl!
  []
    ;; Block query accessors
    (core/register-accessor! :world :get-block-type
      (fn [_ _] nil)
      "Get block type at position. Returns block identifier keyword.")

    (core/register-accessor! :world :get-block-metadata
      (fn [_ _] nil)
      "Get block metadata/properties at position. Returns map of properties.")

    (core/register-accessor! :world :is-block-air?
      (fn [_ _] nil)
      "Check if position contains air block. Returns boolean.")

    (core/register-accessor! :world :is-replaceable?
      (fn [_ _] nil)
      "Check if block at position is replaceable. Returns boolean.")

    ;; Block manipulation
    (core/register-accessor! :world :set-block!
      (fn [_ _ _ _] nil)
      "Set block at position. Parameters: block-id (keyword), properties (map)")

    (core/register-accessor! :world :replace-block!
      (fn [_ _ _ _] nil)
      "Replace block if it matches old-block specification.")

    (core/register-accessor! :world :break-block!
      (fn [_ _ _] nil)
      "Break block at position. Returns success boolean.")

    ;; World effects
    (core/register-accessor! :world :particle-effect
      (fn [_ _ _ _] nil)
      "Spawn particle effect at position. Parameters: effect-type (keyword), velocity (map)")

    (core/register-accessor! :world :sound-effect
      (fn [_ _ _ _ _] nil)
      "Play sound at position. Parameters: volume (0-1), pitch (float)")

    (core/register-accessor! :world :explosion
      (fn [_ _ _ _ _] nil)
      "Create explosion at position. Parameters: radius (float)")

    ;; Raycast operations
    (core/register-accessor! :world :raycast-block
      (fn [_ _ _ _] nil)
      "Cast ray for block collision. Returns {:block block-pos :hit-pos vector}")

    (core/register-accessor! :world :raycast-entity
      (fn [_ _ _ _ _] nil)
      "Cast ray for entity collision. Returns matching entities.")

    ;; Content-named world positions
    (core/register-accessor! :world :save-location
      (fn [_ _ _] nil)
      "Save a world location with identifier.")

    (core/register-accessor! :world :load-location
      (fn [_ _] nil)
      "Load saved location by identifier. Returns world-pos or nil.")

    (core/register-accessor! :world :list-named-positions
      (fn [_] nil)
      "List all named position identifiers in this world.")

    (core/register-accessor! :world :delete-location
      (fn [_ _] nil)
      "Delete saved location by identifier.")

    nil)

(defn register-world-accessors!
  "Explicitly register world-domain accessors once.
   Process-scoped guard: the backing registry (accessor-registry-core's
   registries*) is a process-level atom that throws on duplicate
   registration, not reset by Framework reinjection. process-once! already
   rolls back the flag and rethrows on failure, so a subsequent call retries."
  []
  (install/process-once! ::world-accessors-registered register-world-accessors-impl!)
  nil)
