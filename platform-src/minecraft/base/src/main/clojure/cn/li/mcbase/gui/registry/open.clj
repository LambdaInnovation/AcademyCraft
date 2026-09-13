(ns cn.li.mcbase.gui.registry.open
  "Shared helper functions for GUI open flows in platform registry adapters."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.world.entity.player Player]
           [net.minecraft.world.level.block.entity BlockEntity]))

(defn resolve-optional-block-pos
  "Resolve BlockPos from tile entity when available, otherwise nil.
  Supports map tile metadata and native block entities."
  [tile-entity]
  (when tile-entity
    (try
      (if (map? tile-entity)
        (:pos tile-entity)
        (.getBlockPos ^BlockEntity tile-entity))
      (catch Exception _
        nil))))

(defn log-open-start!
  [prefix player gui-id tile-entity]
  (let [^Player player player]
    (log/debug prefix "Starting GUI open: gui-id=" gui-id "player=" (.getName player) "has-tile-entity=" (not (nil? tile-entity)))))

(defn log-open-success!
  [prefix]
  (log/debug prefix "GUI opened successfully"))

(defn log-open-error!
  [prefix e]
  (log/stacktrace (str prefix "Failed to open GUI") e))

(defn open-player-menu!
  "Open a player menu via the platform-injected loader-specific implementation."
  [player factory]
  (let [fw-atom (or (fw/fw-atom)
                    (throw (ex-info "Framework is not initialized for GUI open" {})))]
    (platform/call-adapter fw-atom :gui-open :open-player-menu! player factory)))
