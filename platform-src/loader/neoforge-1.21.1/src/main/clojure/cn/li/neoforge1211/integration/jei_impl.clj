(ns cn.li.neoforge1211.integration.jei-impl
  "JEI is not a declared capability for neoforge-1.21.1.
  Stub keeps the namespace loadable for AOT without mezz.jei on the classpath.
  Restore real IModPlugin wiring when JEI NeoForge artifacts are catalogued."
  (:require [cn.li.mcmod.util.log :as log]))

(defn create-jei-plugin
  "No-op: JEI is unavailable on this target."
  []
  (log/debug "JEI plugin stub: no JEI capability on neoforge-1.21.1")
  nil)

(defn init-jei!
  "No-op: JEI is unavailable on this target."
  []
  (log/debug "JEI integration stubbed (no JEI dependency on neoforge-1.21.1)"))
