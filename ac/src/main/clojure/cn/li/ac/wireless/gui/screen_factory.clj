(ns cn.li.ac.wireless.gui.screen-factory
  "Platform-agnostic screen factory for AC block GUIs.
  
  This namespace contains the content-side logic for creating reactive screen data,
  independent of platform-specific implementation details (Forge/Fabric).
  
  Platform-specific screen_impl.clj files should delegate to this factory
  and only handle platform-specific registration mechanics."
  (:require [cn.li.mcmod.gui.registry :as gui-registry]
            [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Screen Creation - Core Game Logic
;; ============================================================================

(defn- resolve-clj-container
  [gui-type gui-id minecraft-menu]
  (or (container-state/resolve-container-for-menu minecraft-menu)
      (throw (ex-info "No Clojure container registered for screen creation"
                      {:gui-type gui-type
                       :gui-id gui-id
                       :menu-type (some-> minecraft-menu type str)
                       :menu-container-id (try
                                            (container-state/get-menu-container-id minecraft-menu)
                                            (catch Exception _ nil))}))))

(defn create-screen
  "Generic screen creation function (platform-agnostic factory)
  
  Args:
  - gui-type: Keyword (:node, :matrix, etc.)
  - container-or-handler: Minecraft menu/container created by mc1201 menu bridge
  - player-inventory: Player inventory (not currently used but required by platform APIs)
  - title: Text component (not currently used but required by platform APIs)
  
  Returns: reactive screen data map or nil on error"
  [gui-type container-or-handler player-inventory _title]
  (try
    (let [cfg (gui-registry/get-gui-by-type gui-type)
          _ (when-not cfg
              (throw (ex-info "Unknown gui-type" {:gui-type gui-type})))
      gui-id (gui-registry/get-gui-id-for-type gui-type)
      screen-fn (or (when gui-id (gui-registry/get-screen-fn gui-id))
              (get-in cfg [:lifecycle :screen-fn]))
        _ (when-not screen-fn
          (throw (ex-info "GUI has no screen factory function"
                  {:gui-type gui-type :gui-id gui-id})))
        clj-container (resolve-clj-container gui-type gui-id container-or-handler)
        player (entity/inventory-get-player player-inventory)
        screen-data (screen-fn clj-container container-or-handler player)]
      screen-data)

    (catch Throwable e
      (log/stacktrace (str "[SCREEN-FACTORY-CORE] Failed to create " (name gui-type) " screen") e)
      nil)))


;; ============================================================================
;; Design Notes
;; ============================================================================

;; This factory provides a clean separation between:
;;
;; 1. **Content Logic* (this namespace):
;;    - Extracting Clojure containers from platform wrappers
;;    - Delegating to registered reactive GUI creation
;;    - Error handling and logging
;;
;; 2. **Platform-Specific Code* (screen_impl.clj in each platform):
;;    - Registering screen factories with platform APIs
;;    - Handling platform-specific types and interfaces
;;    - Platform initialization lifecycle
;;
;; Benefits:
;; - DRY: No duplicate game logic across platforms
;; - Testability: Core logic testable without platform dependencies
;; - Maintainability: Changes to screen creation logic in one place
;; - Clarity: Clear boundary between game logic and platform integration
