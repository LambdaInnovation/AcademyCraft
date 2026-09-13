(ns cn.li.forge1201.integration.events.gui-open-port
  "Small port to isolate GUI opening side-effects from event flow orchestration."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.forge1201.adapter.gui-registry :as gui-registry-impl])
  (:import [net.minecraft.world.level Level]))

(defn open-gui-for-result
  [gui-id player world _pos tile-entity]
  (when (and tile-entity (not (.isClientSide ^Level world)))
    (log/debug "[RIGHT-CLICK] Opening GUI on server side...")
    (gui-registry-impl/open-gui-for-player player gui-id tile-entity)))
