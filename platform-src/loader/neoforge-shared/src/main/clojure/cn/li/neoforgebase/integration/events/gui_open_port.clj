(ns cn.li.neoforgebase.integration.events.gui-open-port
  "Shared GUI opening port. Version loaders install the concrete open-fn."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.world.level Level]))

(defonce ^:private open-gui-fn*
  (atom nil))

(defn install-open-gui!
  "Install (fn [player gui-id tile-entity] ...) used by open-gui-for-result."
  [f]
  (reset! open-gui-fn* f)
  f)

(defn open-gui-for-result
  [gui-id player world _pos tile-entity]
  (when (and tile-entity (not (.isClientSide ^Level world)))
    (log/debug "[RIGHT-CLICK] Opening GUI on server side...")
    (when-let [f @open-gui-fn*]
      (f player gui-id tile-entity))))
