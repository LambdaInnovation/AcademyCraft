(ns cn.li.fabric262.commands
  "Brigadier command registration for Fabric 26.2.

  Delegates tree-building to the shared cn.li.mc262.command.brigadier-tree
  namespace; this file only contains the Fabric-specific command registration
  entry point wired via ServerLifecycleEvents."
  (:require [cn.li.mc262.command.executor-core]
            [cn.li.mcbase.command.brigadier-registry :as brig-reg])
  (:import [com.mojang.brigadier CommandDispatcher]))

;; ============================================================================
;; Command Registration
;; ============================================================================

(defn register-commands
  "Register all commands with the Brigadier dispatcher.

  Args:
    ^CommandDispatcher dispatcher: Brigadier command dispatcher

  Returns:
    nil"
  [^CommandDispatcher dispatcher]
  (brig-reg/register-all-commands! dispatcher {:platform :fabric}))
