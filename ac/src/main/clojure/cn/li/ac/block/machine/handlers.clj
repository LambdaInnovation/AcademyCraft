(ns cn.li.ac.block.machine.handlers
  "Shared block machine network handler helpers."
  (:require [cn.li.mcmod.gui.container.sync-routing :as sync-routing]))

(defn open-container-tile
  "Resolve tile from validated open-container C2S payload."
  [payload player]
  (:tile-entity (sync-routing/require-open-container! payload player)))
