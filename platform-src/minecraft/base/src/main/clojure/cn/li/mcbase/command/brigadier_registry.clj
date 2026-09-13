(ns cn.li.mcbase.command.brigadier-registry
  "Shared Brigadier command registration flow.

  Platform modules should delegate registration to this namespace and
  keep only loader-specific entry signatures."
  (:require [cn.li.platform.neutral.command-runtime :as cmd-meta]
            [cn.li.platform.neutral.command-runtime :as command-hooks]
            [cn.li.mcbase.command.action-impls] ; Ensure action implementations are loaded
            [cn.li.mcbase.command.brigadier-tree :as brig-tree]
            [cn.li.mcmod.util.log :as log])
  (:import [com.mojang.brigadier CommandDispatcher]))

(defn- build-command-node
  [spec]
  (brig-tree/build-command-node spec))

(defn- register-command!
  [^CommandDispatcher dispatcher command-id]
  (try
    (log/debug "Registering command:" command-id)
    (when-let [spec (cmd-meta/get-command-spec command-id)]
      (let [node (build-command-node spec)]
        (.register dispatcher node)
        (log/debug "Successfully registered command:" command-id)))
    (catch Exception e
      (log/stacktrace (str "Failed to register command" command-id ":" ) e))))

(defn register-all-commands!
  "Register all commands from metadata with Brigadier.

  Args:
    dispatcher: Brigadier command dispatcher
    opts: Optional map
      :platform - keyword for logging context (:forge/:fabric/etc.)

  Returns:
    nil"
  ([^CommandDispatcher dispatcher]
   (register-all-commands! dispatcher nil))
  ([^CommandDispatcher dispatcher {:keys [platform]}]
   (let [platform-label (or platform :generic)]
     (log/info "Registering commands with Brigadier" {:platform platform-label})

     (try
       (command-hooks/init-commands!)
       (catch Exception e
         (log/stacktrace "Failed to load command definitions:" e)))

     (doseq [command-id (cmd-meta/get-all-command-ids)]
       (register-command! dispatcher command-id))

     (log/info "Command registration complete" {:platform platform-label})
     nil)))
