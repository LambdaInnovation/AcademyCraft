(ns cn.li.forge1201.commands
  "Brigadier command registration for Forge 1.20.1.

  Delegates tree-building to the shared cn.li.mc1201.command.brigadier-tree
  namespace; this file only contains the Forge-specific command registration
  entry point called from ForgeEventBusManager.java."
  (:require [cn.li.mc1201.command.executor-core]
            [cn.li.mcbase.command.brigadier-registry :as brig-reg])
  (:import [com.mojang.brigadier CommandDispatcher]))


;; ============================================================================
;; Command Registration
;; ============================================================================


(defn register-all-commands
  "Register all commands from metadata with Brigadier.

  Args:
    ^CommandDispatcher dispatcher: Brigadier command dispatcher
    _build-context: Command build context (unused for now)

  Returns:
    nil"
  [^CommandDispatcher dispatcher _build-context]
  (brig-reg/register-all-commands! dispatcher {:platform :forge}))

