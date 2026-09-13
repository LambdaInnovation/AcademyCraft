(ns cn.li.mcmod.command.actions
  "Command action execution protocol.

  Actions are platform-agnostic descriptions of what should happen when
  a command executes. This namespace owns the generic registry/execution seam;
  content-specific action ids and executors are registered by content modules."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]))
;; ============================================================================
;; Action Protocol — Map-based
;; ============================================================================

;; Expected map keys for IActionExecutor:
;; - :execute-action (fn [action-map context] -> result-map)

;; Wrapper function

(defn execute-action
  "Execute an action and return result.

  Args:
    executor: Map with :execute-action key
    action-map: Map describing the action to execute
    context: CommandContext

  Returns:
    Result map with :success? boolean and optional :message"
  [executor action-map context]
  ((:execute-action executor) action-map context))

;; ============================================================================
;; Action Validation
;; ============================================================================

(def ^:private base-action-types
  #{:send-message
    :grant-advancement})

;; Action executors stored in Framework [:registry :handlers :action-executors]

(def ^:private executors-path [:registry :handlers :action-executors])

(defn- action-executors-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom executors-path {})
    {}))

(defn valid-action-types
  "Return the currently registered command action ids."
  []
  (into base-action-types (keys (action-executors-snapshot))))

(defn register-action-type!
  "Register an action id as valid without installing an executor.

  Platform namespaces may still provide an `execute-action-impl` multimethod for
  the id. Content namespaces should prefer `register-action-executor!` so their
  business logic remains content-owned."
  [action-type]
  (when-not (keyword? action-type)
    (throw (ex-info "Action type must be a keyword" {:action-type action-type})))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in (conj executors-path action-type) #(or % nil)))
  nil)

(defn register-action-types!
  "Register multiple action ids as valid."
  [action-types]
  (doseq [action-type action-types]
    (register-action-type! action-type))
  nil)

(defn register-action-executor!
  "Register a content-owned executor for an action id.

  The executor receives `[action-map context]` and returns a command result map."
  [action-type executor-fn]
  (when-not (keyword? action-type)
    (throw (ex-info "Action type must be a keyword" {:action-type action-type})))
  (when-not (fn? executor-fn)
    (throw (ex-info "Action executor must be a function" {:action-type action-type
                                                           :executor executor-fn})))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in (conj executors-path action-type) executor-fn))
  nil)

(defn register-action-executors!
  "Register a map of action id -> executor function."
  [executor-map]
  (doseq [[action-type executor-fn] executor-map]
    (register-action-executor! action-type executor-fn))
  nil)

(defn get-action-executor
  "Return the registered content executor for `action-type`, if present."
  [action-type]
  (get (action-executors-snapshot) action-type))

(defn validate-action
  "Validate an action map.

  Args:
    action-map: Map with :action key

  Returns:
    Boolean - true if valid

  Throws:
    ex-info if invalid"
  [action-map]
  (when-not (map? action-map)
    (throw (ex-info "Action must be a map" {:action action-map})))

  (let [action-type (:action action-map)]
    (when-not action-type
      (throw (ex-info "Action map must have :action key" {:action action-map})))

    (when-not (contains? (valid-action-types) action-type)
      (throw (ex-info "Invalid action type"
                      {:action-type action-type
                       :valid-types (valid-action-types)}))))
  true)

;; ============================================================================
;; Action Execution (Multimethod)
;; ============================================================================

(defmulti execute-action-impl
  "Execute an action based on its type.

  This is a multimethod that dispatches on the :action key.
  Platform implementations should provide methods for each action type.

  Args:
    action-map: Map with :action key and action-specific data
    context: CommandContext

  Returns:
    Result map with :success? boolean and optional :message"
  (fn [action-map _context] (:action action-map)))

;; Default implementation logs a warning
(defmethod execute-action-impl :default
  [action-map _context]
  (log/warn "No implementation for action type:" (:action action-map))
  {:success? false
   :message (str "Unimplemented action: " (:action action-map))})

;; ============================================================================
;; Action Execution Wrapper
;; ============================================================================

(defn execute
  "Execute an action with validation.

  Args:
    action-map: Map describing the action
    context: CommandContext

  Returns:
    Result map with :success? boolean and optional :message"
  [action-map context]
  (try
    (validate-action action-map)
    (if-let [executor-fn (get-action-executor (:action action-map))]
      (executor-fn action-map context)
      (execute-action-impl action-map context))
    (catch Exception e
      (log/stacktrace "Error executing action" e)
      {:success? false
       :message (str "Error: " (ex-message e))})))

;; ============================================================================
;; Batch Action Execution
;; ============================================================================

(defn execute-batch
  "Execute multiple actions in sequence.

  Args:
    action-maps: Sequence of action maps
    context: CommandContext

  Returns:
    Vector of result maps"
  [action-maps context]
  (mapv #(execute % context) action-maps))
