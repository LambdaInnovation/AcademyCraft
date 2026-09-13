(ns cn.li.ac.wireless.gui.container.dispatcher
  "Platform-agnostic container operation dispatcher

  This namespace provides unified interfaces for container operations,
  eliminating the need for platform-specific instanceof checks and cond branching.

  Uses metadata-driven dispatch via GUI registry."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.gui.registry :as gui-registry]))

(defn- registration-value [cfg k]
  (get-in cfg [:registration k]))

(defn- lifecycle-value [cfg k]
  (get-in cfg [:lifecycle k]))

(defn- sync-value [cfg k]
  (get-in cfg [:sync k]))

(defn- operation-value [cfg k]
  (get-in cfg [:operations k]))

(defn- slot-value [cfg k]
  (get-in cfg [:slot-operations k]))


;; ============================================================================
;; Container Operation Functions
;; ============================================================================

(defn tick-container!
  "Tick the container (called every frame on server)."
  [container]
  (if-let [f (:server-menu-sync! container)]
    (f container)
    (when-let [cfg (gui-registry/get-config-by-container container)]
      (when-let [f (lifecycle-value cfg :server-menu-sync-fn)]
        (f container)))))

(defn validate-container
  "Check if player can still use this container."
  [container player]
  (if-let [cfg (gui-registry/get-config-by-container container)]
    (if-let [f (operation-value cfg :validate-fn)]
      (boolean (f container player))
      true)
    false))

(defn sync-container!
  "Synchronize container data to client."
  [container]
  (cond
    (fn? (:server-menu-sync! container))
    ((:server-menu-sync! container) container)

    :else
    (when-let [cfg (gui-registry/get-config-by-container container)]
      (when-let [f (or (lifecycle-value cfg :server-menu-sync-fn)
                       (sync-value cfg :server-menu-sync!))]
        (f container)))))

(defn handle-button-click!
  "Handle button click from client."
  [container button-id player]
  (if-let [cfg (gui-registry/get-config-by-container container)]
    (when-let [f (operation-value cfg :button-click-fn)]
      (f container button-id player))
    (log/warn "Unknown container type for button click")))

(defn handle-text-input!
  "Handle text input from client."
  [container field-id text player]
  (if-let [cfg (gui-registry/get-config-by-container container)]
    (when-let [f (operation-value cfg :text-input-fn)]
      (f container field-id text player))
    (log/warn "Unknown container type for text input")))

(defn close-container!
  "Close container and perform cleanup."
  [container]
  (if-let [cfg (gui-registry/get-config-by-container container)]
    (when-let [f (operation-value cfg :close-fn)]
      (f container))
    (log/warn "Unknown container type for close")))

;; ============================================================================
;; Container Type Queries
;; ============================================================================

(defn get-container-type
  "Get the type identifier for a container

  Args:
  - container: NodeContainer or MatrixContainer

  Returns: :node or :matrix"
  [container]
  (if-let [cfg (gui-registry/get-config-by-container container)]
    (registration-value cfg :gui-type)
    (do
      (log/warn "Unknown container type:" (type container))
      :unknown)))

;; ============================================================================
;; Safe Operation Wrappers
;; ============================================================================

(defn safe-validate
  "Safely validate container with error handling

  Args:
  - container: Any container
  - player: Player entity

  Returns: boolean (default false on error). Never returns nil."
  [container player]
  (try
    (boolean (validate-container container player))
    (catch Exception e
      (log/stacktrace "Error validating container" e)
      false)))

(defn server-menu-sync!
  "Lightweight server menu sync: refresh container atoms from tile state only."
  [container]
  (try
    (sync-container! container)
    true
    (catch Exception e
      (log/stacktrace "Error in server menu sync" e)
      false)))

(defn safe-handle-button-click!
  "Safely handle button click with error handling

  Args:
  - container: Any container
  - button-id: int
  - player: Player entity

  Returns: true if successful, false if error"
  [container button-id player]
  (try
    (handle-button-click! container button-id player)
    true
    (catch Exception e
      (log/stacktrace "Error handling button click" e)
      false)))

(defn safe-handle-text-input!
  "Safely handle text input with error handling

  Args:
  - container: Any container
  - field-id: int
  - text: string
  - player: Player entity

  Returns: true if successful, false if error"
  [container field-id text player]
  (try
    (handle-text-input! container field-id text player)
    true
    (catch Exception e
      (log/stacktrace "Error handling text input" e)
      false)))

(defn safe-close!
  "Safely close container with error handling

  Args:
  - container: Any container implementing IContainerOperations

  Returns: true if successful, false if error"
  [container]
  (try
    (close-container! container)
    true
    (catch Exception e
      (log/stacktrace "Error closing container" e)
      false)))

;; ============================================================================
;; Slot Operation Dispatch (for platform menu bridges)
;; ============================================================================

(defn slot-count
  "Get tile slot count for a container. Returns 0 for unknown containers."
  [container]
  (try
    (if-let [cfg (gui-registry/get-config-by-container container)]
      (if-let [f (slot-value cfg :slot-count-fn)]
        (f container)
        0)
      0)
    (catch Exception e
      (log/stacktrace "Error getting slot count" e)
      0)))

(defn slot-get-item
  "Get slot item as ItemStack (or nil/empty when unavailable)."
  [container slot-index]
  (try
    (when-let [cfg (gui-registry/get-config-by-container container)]
      (when-let [f (slot-value cfg :slot-get-fn)]
        (f container slot-index)))
    (catch Exception e
      (log/stacktrace "Error getting slot item" e)
      nil)))

(defn slot-set-item!
  "Set slot item. Nil stack clears the slot."
  [container slot-index item-stack]
  (try
    (when-let [cfg (gui-registry/get-config-by-container container)]
      (when-let [f (slot-value cfg :slot-set-fn)]
        (f container slot-index item-stack)))
    (catch Exception e
      (log/stacktrace "Error setting slot item" e)
      nil)))

(defn slot-can-place?
  "Check whether a stack may be placed in a slot."
  [container slot-index item-stack]
  (try
    (if-let [cfg (gui-registry/get-config-by-container container)]
      (if-let [f (slot-value cfg :slot-can-place-fn)]
        (boolean (f container slot-index item-stack))
        true)
      false)
    (catch Exception e
      (log/stacktrace "Error checking slot placement" e)
      false)))

(defn slot-changed!
  "Notify container that slot content changed."
  [container slot-index]
  (try
    (when-let [cfg (gui-registry/get-config-by-container container)]
      (when-let [f (slot-value cfg :slot-changed-fn)]
        (f container slot-index)))
    (catch Exception e
      (log/stacktrace "Error in slot changed notification" e)
      nil)))

(defn execute-quick-move
  "Dispatch quick-move/shift-click to the GUI's registered quick-move function.

  Returns the handler result when implemented, else nil."
  [container slot-index player-inventory-start]
  (try
    (when-let [cfg (gui-registry/get-config-by-container container)]
      (when-let [f (slot-value cfg :quick-move-fn)]
        (f container slot-index player-inventory-start)))
    (catch Exception e
      (log/stacktrace "Error executing quick move" e)
      nil)))

;; ============================================================================
;; Design Notes
;; ============================================================================;

;; This dispatcher provides:
;;
;; 1. **Metadata-Driven Dispatch*:
;;    - Uses GUI registry to look up handlers by container predicate
;;    - No hardcoded type checks or cond branching needed
;;    - Extensible: new container types register metadata through the GUI registry
;;
;; 2. **Unified Interface*:
;;    - tick-container! - Lifecycle tick
;;    - validate-container - Permission check
;;    - sync-container! - Data synchronization
;;    - handle-button-click! - Button interactions
;;    - handle-text-input! - Text field interactions
;;    - close-container! - Cleanup on close
;;
;; 3. **Slot Operations*:
;;    - slot-count, slot-get-item, slot-set-item!
;;    - slot-can-place?, slot-changed!
;;
;; 4. **Type Queries*:
;;    - get-container-type: returns :gui-type from metadata
;;
;; 5. **Error Handling*:
;;    - public platform callbacks catch exceptions
;;    - Return sensible defaults on error
;;    - Log errors for debugging
;;
;; Benefits:
;; - Zero dispatcher changes when adding new GUIs
;; - Single source of truth in GUI registry declarations
;; - Clean separation of concerns
;; - Easy to add new container types
;;
;; Usage in platform bridge:
;;   (defn -tick [this]
;;     (dispatcher/server-menu-sync! (get-clojure-container this)))
;;
;;   (defn -stillValid [this player]
;;     (dispatcher/safe-validate (get-clojure-container this) player))
