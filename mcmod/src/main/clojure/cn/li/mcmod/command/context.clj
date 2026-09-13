(ns cn.li.mcmod.command.context
  "Platform-agnostic command context structure.

  The context provides a unified interface for command handlers to access
  player, world, and argument data without depending on Minecraft APIs.")

;; ============================================================================
;; Command Context Record
;; ============================================================================

(defrecord CommandContext
  [player        ; Platform player object
   world         ; Platform world object
   source        ; Command source (for permission checks, feedback)
   arguments     ; Parsed arguments map {:arg-name value}
   target-player ; Optional target player (for admin commands)
   metadata]     ; Additional metadata map
  ;; Platform-agnostic command execution context
  ;;
  ;; Fields:
  ;; - player: The player executing the command
  ;; - world: The world where the command is executed
  ;; - source: Command source (for sending feedback, checking permissions)
  ;; - arguments: Map of parsed argument values
  ;; - target-player: Optional target player (for commands like /aimp)
  ;; - metadata: Additional context-specific data
  )

;; ============================================================================
;; Context Creation
;; ============================================================================

(defn create-context
  "Create a CommandContext from platform data.

  Args:
    platform-data: Map with keys:
      :player - Platform player object
      :world - Platform world object
      :source - Command source
      :arguments - Parsed arguments map
      :target-player - Optional target player
      :metadata - Optional metadata map

  Returns:
    CommandContext record"
  [{:keys [player world source arguments target-player metadata]}]
  (map->CommandContext
    {:player player
     :world world
     :source source
     :arguments (or arguments {})
     :target-player target-player
     :metadata (or metadata {})}))

;; ============================================================================
;; Context Accessors
;; ============================================================================

(defn get-player
  "Get the player executing the command"
  [ctx]
  (:player ctx))

(defn get-world
  "Get the world where the command is executed"
  [ctx]
  (:world ctx))

(defn get-source
  "Get the command source"
  [ctx]
  (:source ctx))

(defn get-arguments
  "Get all parsed arguments"
  [ctx]
  (:arguments ctx))

(defn get-argument
  "Get a specific argument value by name.

  Args:
    ctx: CommandContext
    arg-name: Keyword or string - Argument name

  Returns:
    Argument value or nil"
  [ctx arg-name]
  (get (:arguments ctx) (keyword arg-name)))

(defn get-target-player
  "Get the target player (for admin commands)"
  [ctx]
  (:target-player ctx))

(defn get-metadata
  "Get metadata value by key"
  [ctx key]
  (get (:metadata ctx) key))

;; ============================================================================
;; Context Modification
;; ============================================================================

(defn with-argument
  "Add or update an argument in the context.

  Args:
    ctx: CommandContext
    arg-name: Keyword or string
    value: Argument value

  Returns:
    Updated CommandContext"
  [ctx arg-name value]
  (assoc-in ctx [:arguments (keyword arg-name)] value))

(defn with-target-player
  "Set the target player in the context.

  Args:
    ctx: CommandContext
    player: Platform player object

  Returns:
    Updated CommandContext"
  [ctx player]
  (assoc ctx :target-player player))

(defn with-metadata
  "Add or update metadata in the context.

  Args:
    ctx: CommandContext
    key: Keyword
    value: Metadata value

  Returns:
    Updated CommandContext"
  [ctx key value]
  (assoc-in ctx [:metadata key] value))
