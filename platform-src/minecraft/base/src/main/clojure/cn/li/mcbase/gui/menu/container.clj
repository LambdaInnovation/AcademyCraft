(ns cn.li.mcbase.gui.menu.container
  "Shared Minecraft 1.20.1 container adapter for Clojure-side containers.

  Wraps a Clojure container map (via mcmod gui.adapter.platform-registry) as a
  vanilla `net.minecraft.world.Container` so Forge and Fabric slot machinery
  can interact with it without platform-specific code."
  (:require [cn.li.platform.neutral.gui-runtime :as platform])
  (:import [cn.li.mcbase.shim UniversalContainer]))

(defn create-tile-inventory-adapter
  "Return a `Container` Java skeleton backed by a Clojure container.

  Works identically on Forge and Fabric – both use the same MC 1.20.1 API."
  [clj-container]
  (UniversalContainer.
    clj-container
    (fn [c] (int (or (platform/slot-count c) 0)))
    (fn [c slot] (or (platform/slot-get-item c (int slot)) nil))
    (fn [c slot stack] (platform/slot-set-item! c (int slot) stack))
    (fn [c slot] (platform/slot-changed! c (int slot)))
    (fn [c player] (boolean (platform/safe-validate c player)))
    (fn [c slot stack] (boolean (platform/slot-can-place? c (int slot) stack)))))
