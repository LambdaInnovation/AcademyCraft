(ns cn.li.neoforge1211.commands
  "Brigadier command registration for NeoForge 1.21.1.

  Delegates tree-building to the shared cn.li.mc1211.command.brigadier-tree
  namespace; this file only contains the Forge-specific command registration
  entry point called from ForgeEventBusManager.java."
  (:require [cn.li.mc1211.command.executor-core]
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

