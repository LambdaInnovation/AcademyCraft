(ns cn.li.mcbase.command.brigadier-tree
  "Shared Brigadier command tree builders, platform-neutral.

  Contains execute-command, build-executor, build-argument-node,
  build-arguments-chain, build-subcommand-node, and build-command-node.
  These functions only use Brigadier and Minecraft command APIs which
  are present on both Forge and Fabric."
  (:require [cn.li.platform.neutral.command-runtime :as cmd-ctx]
            [cn.li.platform.neutral.command-runtime :as cmd-actions]
            [cn.li.mcbase.command.brigadier-util :as brig-util]
            [cn.li.mcbase.command.feedback :as feedback]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver McAccess]
           [com.mojang.brigadier.builder ArgumentBuilder]
           [com.mojang.brigadier.builder LiteralArgumentBuilder RequiredArgumentBuilder]
           [com.mojang.brigadier.context CommandContext]
           [com.mojang.brigadier.suggestion SuggestionProvider SuggestionsBuilder]
           [net.minecraft.commands CommandSourceStack SharedSuggestionProvider]
           [net.minecraft.server.level ServerPlayer]))

(defn- player-uuid
  [^ServerPlayer player]
  (when player
    (str (.getUUID player))))

(defn- source-player
  ^ServerPlayer [^CommandSourceStack source]
  (try (.getPlayerOrException source)
       (catch Exception _ nil)))

(defn- command-source-owner
  "Server player-state owner for a command dispatch/suggestion boundary."
  [^CommandSourceStack source ^ServerPlayer player]
  (cond-> {:logical-side :server
           :server-session-id [:server (System/identityHashCode (.getServer source))]}
    player (assoc :player-uuid (str (.getUUID player)))))

;; ============================================================================
;; Command Execution
;; ============================================================================

(defn execute-command
  "Execute a command handler and process the result.

  Args:
    executor-fn: Command handler function
    ^CommandContext brigadier-ctx: Brigadier command context
    arg-specs: Vector of ArgumentSpec records
    target-player: Optional target player (for admin commands)

  Returns:
    Integer - Brigadier success code (1 for success, 0 for failure)"
  [executor-fn ^CommandContext brigadier-ctx arg-specs target-player]
  (try
    (let [^CommandSourceStack source (.getSource brigadier-ctx)
          player (source-player source)
          world (when player (.level player))
          arguments (brig-util/extract-all-arguments brigadier-ctx arg-specs)
          ;; Command execution is a server-side dispatch boundary (hooks.core
          ;; -- #2): rebuild the player-state owner here -- the Brigadier
          ;; dispatcher runs outside the tick-loop owner binding, so action
          ;; executors would otherwise see no bound session-id.
          owner (command-source-owner source player)
          ctx (cmd-ctx/create-context
                {:player player
                 :world world
                 :source source
                 :arguments arguments
                 :target-player target-player
                 :metadata {:player-uuid-fn player-uuid
                    :player-state-owner owner
                    :send-feedback-fn (fn [message translate? args error?]
                        (feedback/send-feedback! source message translate? args error?))}})]

      (runtime-hooks/with-client-ctx-fn {:player-owner owner}
        (fn []
          (let [action-result (executor-fn ctx)]
            ;; Execute the action
            (when action-result
              (cmd-actions/execute action-result ctx)))))

      ;; Return success
      1)
    (catch Exception e
      (log/stacktrace "Command execution failed:" e)
      0)))

;; ============================================================================
;; Command Node Building
;; ============================================================================

(defn build-executor
  "Build a Brigadier command executor.

  Args:
    executor-fn: Command handler function
    arg-specs: Vector of ArgumentSpec records
    target-player-arg: Optional name of target player argument

  Returns:
    Brigadier Command instance"
  [executor-fn arg-specs target-player-arg]
  (reify com.mojang.brigadier.Command
    (^int run [_ ^CommandContext ctx]
      (let [target-player (when target-player-arg
                            (try
                              (brig-util/entity-arg-get-player ctx target-player-arg)
                              (catch Exception _ nil)))]
        (int (execute-command executor-fn ctx arg-specs target-player))))))

(defn- suggestion-provider
  "Wrap a DSL :suggestions fn as a Brigadier SuggestionProvider.

  suggestions-fn: (fn [{:keys [player-uuid]}] -> seqable of suggestion values).
  It is invoked with a normalized, platform-free context (player-uuid nil from
  console) under a bound server player-state owner -- the suggestion packet
  path, like command execution, runs outside the tick-loop owner binding, so
  content fns can read player state through the usual hooks. Values are read
  dynamically at completion time; SharedSuggestionProvider/suggest applies the
  prefix filter for the partially-typed input."
  [suggestions-fn]
  (reify SuggestionProvider
    (getSuggestions [_ ctx suggestions-builder]
      (let [^CommandSourceStack source (.getSource ^CommandContext ctx)
            player (source-player source)
            owner (command-source-owner source player)
            values (try
                     (runtime-hooks/with-client-ctx-fn {:player-owner owner}
                       (fn []
                         (into [] (map str)
                               (suggestions-fn {:player-uuid (player-uuid player)}))))
                     (catch Exception e
                       (log/warn "Suggestion provider failed:" (ex-message e))
                       []))]
        (SharedSuggestionProvider/suggest
          ^Iterable values
          ^SuggestionsBuilder suggestions-builder)))))

(defn build-argument-node
  "Build a Brigadier argument node.

  Args:
    arg-spec: ArgumentSpec record
    executor-fn: Optional executor function (for last argument)
    all-arg-specs: All argument specs (for extraction)
    target-player-arg: Optional target player argument name

  Returns:
    RequiredArgumentBuilder"
  [arg-spec executor-fn all-arg-specs target-player-arg]
  (let [arg-name (name (:name arg-spec))
        arg-type (brig-util/map-argument-type arg-spec)
        builder (RequiredArgumentBuilder/argument arg-name arg-type)]
    (when-let [suggestions-fn (:suggestions-fn arg-spec)]
      (.suggests builder (suggestion-provider suggestions-fn)))
    (when executor-fn
      (.executes builder (build-executor executor-fn all-arg-specs target-player-arg)))
    builder))

(defn all-optional?
  "True when every one of these argument specs may be omitted."
  [arg-specs]
  (every? :optional? arg-specs))

(defn- complete-at?
  "True when supplying argument `index` already forms a runnable command --
   that is, every argument after it is optional."
  [arg-specs index]
  (all-optional? (subvec arg-specs (inc index))))

(defn build-arguments-chain
  "Build a chain of argument nodes.

  Every prefix that is followed only by optional arguments gets its own
  .executes, which is what makes :optional? mean anything: Brigadier has no
  notion of optional arguments, a command runs at whichever node the input
  stops on. Attaching the executor solely to the last node -- as this did --
  silently made every declared-optional argument mandatory.

  Args:
    arg-specs: Vector of ArgumentSpec records
    executor-fn: Executor function
    target-player-arg: Optional target player argument name

  Returns:
    First argument node with chained arguments"
  [arg-specs executor-fn target-player-arg]
  (when (seq arg-specs)
    (let [arg-specs (vec arg-specs)]
      (loop [index (dec (count arg-specs))
             ^ArgumentBuilder current-node nil]
        (if (neg? index)
          current-node
          (let [^RequiredArgumentBuilder node
                (build-argument-node
                  (nth arg-specs index)
                  (when (complete-at? arg-specs index) executor-fn)
                  arg-specs
                  target-player-arg)]
            (when current-node
              (.then node current-node))
            (recur (dec index) node)))))))

(defn build-subcommand-node
  "Build a Brigadier node for a subcommand.

  Args:
    subcommand-name: String - Subcommand name
    subcommand-spec: SubcommandSpec record
    parent-args: Vector of parent command arguments
    target-player-arg: Optional target player argument name

  Returns:
    LiteralArgumentBuilder"
  [subcommand-name subcommand-spec parent-args target-player-arg]
  (let [^LiteralArgumentBuilder literal (LiteralArgumentBuilder/literal subcommand-name)
        executor-fn (:executor-fn subcommand-spec)
        sub-args (:arguments subcommand-spec)
        all-args (vec (concat parent-args sub-args))]
    (if (seq sub-args)
      (let [^RequiredArgumentBuilder arg-chain (build-arguments-chain sub-args executor-fn target-player-arg)]
        (.then literal arg-chain)
        ;; The bare subcommand is itself runnable when nothing is required,
        ;; e.g. `/aim level` printing the current level.
        (when (and executor-fn (all-optional? sub-args))
          (.executes literal (build-executor executor-fn all-args target-player-arg))))
      (when executor-fn
        (.executes literal (build-executor executor-fn all-args target-player-arg))))
    literal))

(defn build-command-node
  "Build a Brigadier command node from DSL spec.

  Args:
    command-spec: CommandSpec record

  Returns:
    LiteralArgumentBuilder"
  [command-spec]
  (let [command-id (:id command-spec)
        ^LiteralArgumentBuilder literal (LiteralArgumentBuilder/literal command-id)
        permission-level (:permission-level command-spec)]

    ;; Set permission requirement
    (.requires literal
      (reify java.util.function.Predicate
        (test [_ source]
          (McAccess/hasCommandPermission ^CommandSourceStack source (int permission-level)))))

    ;; Handle command tree (with subcommands)
    (if-let [subcommands (:subcommands command-spec)]
      (let [parent-args (:arguments command-spec)
            target-player-arg (when (seq parent-args)
                                (let [first-arg (first parent-args)]
                                  (when (= :player (:type first-arg))
                                    (name (:name first-arg)))))]
        ;; Build parent arguments if any
        (if (seq parent-args)
          (let [arg-chain-builder (fn [^ArgumentBuilder parent-node]
                                    (doseq [[sub-name sub-spec] subcommands]
                                      (let [^LiteralArgumentBuilder sub-node (build-subcommand-node
                                                                            (name sub-name)
                                                                            sub-spec
                                                                            parent-args
                                                                            target-player-arg)]
                                        (.then parent-node sub-node)))
                                    parent-node)
                ^RequiredArgumentBuilder first-arg-node (build-argument-node
                                                         (first parent-args)
                                                         nil
                                                         parent-args
                                                         target-player-arg)]
            (arg-chain-builder first-arg-node)
            (.then literal first-arg-node))
          ;; No parent arguments, attach subcommands directly
          (doseq [[sub-name sub-spec] subcommands]
            (let [^LiteralArgumentBuilder sub-node (build-subcommand-node
                                                    (name sub-name)
                                                    sub-spec
                                                    []
                                                    nil)]
              (.then literal sub-node)))))

      ;; Simple command (no subcommands)
      (let [executor-fn (:executor-fn command-spec)
            arg-specs (:arguments command-spec)]
        (if (seq arg-specs)
          (let [^RequiredArgumentBuilder arg-chain (build-arguments-chain arg-specs executor-fn nil)]
            (.then literal arg-chain)
            (when (and executor-fn (all-optional? arg-specs))
              (.executes literal (build-executor executor-fn arg-specs nil))))
          (when executor-fn
            (.executes literal (build-executor executor-fn [] nil))))))

    literal))
