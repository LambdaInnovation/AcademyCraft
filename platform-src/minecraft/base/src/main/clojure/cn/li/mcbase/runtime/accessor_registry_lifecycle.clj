(ns cn.li.mcbase.runtime.accessor-registry-lifecycle
  "Lifecycle-domain accessor registrations.
   
   Provides platform-independent accessors for lifecycle operations:
   - Server startup/shutdown
   - World load/unload
   - Player login/logout
   - Mod lifecycle events"
  (:require [cn.li.mcbase.runtime.accessor-registry-core :as core]
            [cn.li.mcmod.runtime.install :as install]))

;; ============================================================================
;; Lifecycle Accessor Registrations
;; ============================================================================

(defn- register-lifecycle-accessors-impl!
  "Register all lifecycle-domain accessors.
   
   This function should be called during mod initialization to populate
   the lifecycle accessor registry with all available lifecycle operations."
  []
  ;; Server lifecycle
  (core/register-accessor! :lifecycle :on-server-start
    (fn [_] nil)
    "Called when server starts. Parameters: server (platform-specific)")
  
  (core/register-accessor! :lifecycle :on-server-stop
    (fn [_] nil)
    "Called when server stops. Parameters: server (platform-specific)")
  
  (core/register-accessor! :lifecycle :on-server-tick
    (fn [_ _] nil)
    "Called on each server tick. Parameters: server, tick-count (long)")
  
  (core/register-accessor! :lifecycle :get-server-ticks
    (fn [_] nil)
    "Get total server ticks elapsed. Returns long.")
  
  ;; World lifecycle
  (core/register-accessor! :lifecycle :on-world-load
    (fn [_] nil)
    "Called when world loads. Parameters: world (platform-specific)")
  
  (core/register-accessor! :lifecycle :on-world-unload
    (fn [_] nil)
    "Called when world unloads. Parameters: world (platform-specific)")
  
  (core/register-accessor! :lifecycle :on-world-tick
    (fn [_ _] nil)
    "Called on each world tick. Parameters: world, tick-count (long)")
  
  (core/register-accessor! :lifecycle :is-world-loaded?
    (fn [_] nil)
    "Check if world is loaded. Returns boolean.")
  
  (core/register-accessor! :lifecycle :get-world-name
    (fn [_] nil)
    "Get world identifier/name. Returns string.")
  
  ;; Player lifecycle
  (core/register-accessor! :lifecycle :on-player-login
    (fn [_] nil)
    "Called when player logs in. Parameters: player (entity)")
  
  (core/register-accessor! :lifecycle :on-player-logout
    (fn [_] nil)
    "Called when player logs out. Parameters: player (entity)")
  
  (core/register-accessor! :lifecycle :on-player-dimension-change
    (fn [_ _ _] nil)
    "Called when player changes dimensions. Parameters: player, from-dim (keyword), to-dim (keyword)")
  
  (core/register-accessor! :lifecycle :on-player-death
    (fn [_ _] nil)
    "Called when player dies. Parameters: player, damage-source (keyword)")
  
  (core/register-accessor! :lifecycle :on-player-respawn
    (fn [_] nil)
    "Called when player respawns. Parameters: player (entity)")
  
  ;; Block lifecycle
  (core/register-accessor! :lifecycle :on-block-placed
    (fn [_ _ _ _] nil)
    "Called when block is placed. Parameters: world, pos, block-id (keyword), player")
  
  (core/register-accessor! :lifecycle :on-block-broken
    (fn [_ _ _ _] nil)
    "Called when block is broken. Parameters: world, pos, block-id (keyword), player")
  
  ;; Entity lifecycle
  (core/register-accessor! :lifecycle :on-entity-spawn
    (fn [_] nil)
    "Called when entity spawns. Parameters: entity")
  
  (core/register-accessor! :lifecycle :on-entity-despawn
    (fn [_] nil)
    "Called when entity despawns. Parameters: entity")
  
  (core/register-accessor! :lifecycle :on-entity-death
    (fn [_ _] nil)
    "Called when entity dies. Parameters: entity, damage-source (keyword)")
  
  ;; Mod initialization
  (core/register-accessor! :lifecycle :on-mod-load
    (fn [] nil)
    "Called when mod loads (setup phase)")
  
  (core/register-accessor! :lifecycle :on-mod-setup
    (fn [] nil)
    "Called during common setup phase")
  
  (core/register-accessor! :lifecycle :on-client-setup
    (fn [] nil)
    "Called during client setup phase (client-only)")
  
  nil)

(defn register-lifecycle-accessors!
  "Explicitly register lifecycle-domain accessors once.
   Process-scoped guard: the backing registry (accessor-registry-core's
   registries*) is a process-level atom that throws on duplicate
   registration, not reset by Framework reinjection. process-once! already
   rolls back the flag and rethrows on failure, so a subsequent call retries."
  []
  (install/process-once! ::lifecycle-accessors-registered register-lifecycle-accessors-impl!)
  nil)
