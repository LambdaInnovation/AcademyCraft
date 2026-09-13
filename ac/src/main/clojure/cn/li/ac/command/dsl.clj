(ns cn.li.ac.command.dsl
  "Command DSL - Declarative command definition using Clojure macros"
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Command Registry — Framework [:registry :commands :dsl]

(def ^:private cmd-path [:registry :commands :dsl])

(defn- command-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom cmd-path {:commands {} :frozen? false})
    {:commands {} :frozen? false}))

(defn- update-command-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in cmd-path
           (fn [current] (apply f (or current {:commands {} :frozen? false}) args))))
  nil)

(defn- assert-command-registry-open! []
  (when (:frozen? (command-registry-state-snapshot))
    (throw (ex-info "Command registry is frozen" {}))))

(defn command-registry-snapshot []
  (command-registry-state-snapshot))

(defn reset-command-registry-for-test!
  ([] (reset-command-registry-for-test! {}))
  ([{:keys [commands frozen?] :or {commands {} frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in cmd-path {:commands commands :frozen? frozen?}))
   nil))

(defn freeze-command-registry!
  []
  (update-command-registry-state! assoc :frozen? true)
  nil)

;; ============================================================================
;; Record Structures for Command Specifications
;; ============================================================================

(defrecord ArgumentSpec
  [name type optional? default-value suggestions-fn validator-fn description]
  ;; Argument specification for command parameters
  ;;
  ;; Fields:
  ;; - name: Argument name (keyword or string)
  ;; - type: Argument type (:player, :string, :integer, :float, :boolean, :enum)
  ;; - optional?: Whether this argument is optional
  ;; - default-value: Default value when optional argument is not provided
  ;; - suggestions-fn: Function to provide tab completion suggestions (fn [context] -> [suggestions])
  ;; - validator-fn: Function to validate argument value (fn [value context] -> boolean or error-message)
  ;; - description: Human-readable description of the argument
  )

(defrecord SubcommandSpec
  [name arguments executor-fn description permission-level subcommands]
  ;; Subcommand specification for nested command trees
  ;;
  ;; Fields:
  ;; - name: Subcommand name (string)
  ;; - arguments: Vector of ArgumentSpec records
  ;; - executor-fn: Function to execute when this subcommand is invoked
  ;; - description: Human-readable description
  ;; - permission-level: Permission level override (inherits from parent if nil)
  ;; - subcommands: Map of nested subcommands (for multi-level trees)
  )

(defrecord CommandSpec
  [id permission-level arguments executor-fn description subcommands]
  ;; Complete command specification
  ;;
  ;; Fields:
  ;; - id: Unique command identifier (string)
  ;; - permission-level: Required vanilla permission level (0=all, 2=OP,
  ;;   4=top OP level, as upstream AcademyCraft's ACCommand inherits the
  ;;   vanilla default of 4). The server console satisfies every level.
  ;; - arguments: Vector of ArgumentSpec records (for simple commands)
  ;; - executor-fn: Function to execute (for simple commands)
  ;; - description: Human-readable description
  ;; - subcommands: Map of SubcommandSpec records (for command trees)
  )

;; ============================================================================
;; Argument Type Definitions
;; ============================================================================

(def argument-types
  "Supported argument types for commands"
  #{:player      ; Player entity
    :string      ; String argument
    :integer     ; Integer number
    :float       ; Floating point number
    :boolean     ; Boolean (true/false)
    :enum        ; Enumerated value from a set
    :word        ; Single word (no spaces)
    :greedy-string}) ; Greedy string (consumes all remaining input)

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn create-argument-spec
  "Create an ArgumentSpec from options map"
  [arg-name options]
  (let [arg-type (:type options :string)]
    (when-not (contains? argument-types arg-type)
      (throw (ex-info "Invalid argument type"
                      {:arg-name arg-name
                       :type arg-type
                       :valid-types argument-types})))
    (map->ArgumentSpec
      {:name arg-name
       :type arg-type
       :optional? (boolean (:optional? options))
       :default-value (:default options)
       :suggestions-fn (:suggestions options)
       :validator-fn (:validator options)
       :description (:description options)})))

(defn create-subcommand-spec
  "Create a SubcommandSpec from options map"
  [subcommand-name options]
  (let [arguments (if (vector? (:arguments options))
                    (mapv (fn [arg]
                            (if (instance? ArgumentSpec arg)
                              arg
                              (create-argument-spec
                                (or (:name arg) (str "arg-" (gensym)))
                                arg)))
                          (:arguments options))
                    [])
        nested-subcommands (when-let [subs (:subcommands options)]
                             (into {}
                                   (map (fn [[k v]]
                                          [k (create-subcommand-spec (name k) v)])
                                        subs)))]
    (map->SubcommandSpec
      {:name (name subcommand-name)
       :arguments arguments
       :executor-fn (:executor-fn options)
       :description (:description options)
       :permission-level (:permission-level options)
       :subcommands nested-subcommands})))

(defn create-command-spec
  "Create a CommandSpec from options"
  [command-id options]
  (when-not (string? command-id)
    (throw (ex-info "Command ID must be a string" {:id command-id})))

  (let [permission-level (:permission-level options 0)
        arguments (if (vector? (:arguments options))
                    (mapv (fn [arg]
                            (if (instance? ArgumentSpec arg)
                              arg
                              (create-argument-spec
                                (or (:name arg) (str "arg-" (gensym)))
                                arg)))
                          (:arguments options))
                    [])
        subcommands (when-let [subs (:subcommands options)]
                      (into {}
                            (map (fn [[k v]]
                                   [k (create-subcommand-spec (name k) v)])
                                 subs)))]

    ;; Validate: either executor-fn or subcommands, not both
    (when (and (:executor-fn options) subcommands)
      (throw (ex-info "Command cannot have both executor-fn and subcommands"
                      {:id command-id})))

    (when-not (or (:executor-fn options) subcommands)
      (throw (ex-info "Command must have either executor-fn or subcommands"
                      {:id command-id})))

    (map->CommandSpec
      {:id command-id
       :permission-level permission-level
       :arguments arguments
       :executor-fn (:executor-fn options)
       :description (:description options)
       :subcommands subcommands})))

;; ============================================================================
;; Registry Functions
;; ============================================================================

(defn register-command!
  "Register a command in the command registry"
  [command-spec]
  (when-not (instance? CommandSpec command-spec)
    (throw (ex-info "Must register a CommandSpec" {:spec command-spec})))

  (if-let [existing (get (:commands (command-registry-state-snapshot)) (:id command-spec))]
    existing
    (do
      (assert-command-registry-open!)
      (log/debug "Registering command:" (:id command-spec))
      (update-command-registry-state! assoc-in [:commands (:id command-spec)] command-spec)
      command-spec)))

(defn get-command
  "Get a command from the registry by ID"
  [command-id]
  (get (:commands (command-registry-state-snapshot)) command-id))

(defn list-commands
  "List all registered command IDs"
  []
  (keys (:commands (command-registry-state-snapshot))))

(defn unregister-command!
  "Remove a command from the registry"
  [command-id]
  (assert-command-registry-open!)
  (update-command-registry-state! update :commands dissoc command-id))

(defn clear-registry!
  "Clear all commands from the registry (for testing)"
  []
  (reset-command-registry-for-test!))

;; ============================================================================
;; DSL Macros
;; ============================================================================

(defmacro defcommand
  "Define a simple command with arguments and an executor function.

  Example:
  (defcommand acach
    :permission-level 2
    :arguments [{:name \"advancement\" :type :string}
                {:name \"player\" :type :player :optional? true}]
    :executor-fn handle-grant-advancement
    :description \"Grant advancement to player\")"
  [command-name & options]
  (let [command-id (name command-name)
        options-map (apply hash-map options)]
    `(def ~command-name
       (register-command!
         (create-command-spec ~command-id ~options-map)))))

(defmacro defcommand-tree
  "Define a command tree with subcommands.

  Example:
  (defcommand-tree aim
    :permission-level 0
    :description \"Ability management commands\"
    :subcommands
    {:cat {:arguments [{:name \"category\" :type :string}]
           :executor-fn handle-aim-cat
           :description \"Switch ability category\"}
     :learn {:arguments [{:name \"skill\" :type :string}]
             :executor-fn handle-aim-learn
             :description \"Learn a skill\"}})"
  [command-name & options]
  (let [command-id (name command-name)
        options-map (apply hash-map options)]
    `(def ~command-name
       (register-command!
         (create-command-spec ~command-id ~options-map)))))

;; ============================================================================
;; Utility Functions for Command Execution
;; ============================================================================

(defn find-subcommand
  "Find a subcommand by path in a command tree.

  Args:
    command-spec: CommandSpec record
    path: Vector of subcommand names (e.g., [\"aim\" \"cat\"])

  Returns:
    SubcommandSpec or nil if not found"
  [command-spec path]
  (when (seq path)
    (loop [current-subs (:subcommands command-spec)
           remaining-path path]
      (if (empty? remaining-path)
        nil
        (let [subcommand-name (first remaining-path)
              subcommand (get current-subs (keyword subcommand-name))]
          (if-not subcommand
            nil
            (if (= 1 (count remaining-path))
              subcommand
              (recur (:subcommands subcommand) (rest remaining-path)))))))))

(defn get-executor
  "Get the executor function for a command or subcommand.

  Args:
    command-spec: CommandSpec record
    subcommand-path: Optional vector of subcommand names

  Returns:
    Executor function or nil"
  [command-spec & [subcommand-path]]
  (if (empty? subcommand-path)
    (:executor-fn command-spec)
    (when-let [subcommand (find-subcommand command-spec subcommand-path)]
      (:executor-fn subcommand))))

(defn get-arguments
  "Get the arguments for a command or subcommand.

  Args:
    command-spec: CommandSpec record
    subcommand-path: Optional vector of subcommand names

  Returns:
    Vector of ArgumentSpec records"
  [command-spec & [subcommand-path]]
  (if (empty? subcommand-path)
    (:arguments command-spec)
    (when-let [subcommand (find-subcommand command-spec subcommand-path)]
      (:arguments subcommand))))

(defn get-permission-level
  "Get the permission level for a command or subcommand.

  Args:
    command-spec: CommandSpec record
    subcommand-path: Optional vector of subcommand names

  Returns:
    Permission level (integer)"
  [command-spec & [subcommand-path]]
  (if (empty? subcommand-path)
    (:permission-level command-spec)
    (when-let [subcommand (find-subcommand command-spec subcommand-path)]
      (or (:permission-level subcommand)
          (:permission-level command-spec)))))
