(ns cn.li.mcbase.gui.provider.common
  "Shared helper functions for GUI provider bridges."
  (:import [net.minecraft.world.entity.player Player]
           [net.minecraft.world.level.block.entity BlockEntity]))

(defn tile->pos
  [tile-entity ^Player player]
  (cond
    (nil? tile-entity)
    (.blockPosition player)

    (map? tile-entity)
    (or (:pos tile-entity) (.blockPosition player))

    :else
    (try
      (.getBlockPos ^BlockEntity tile-entity)
      (catch Exception _
        (.blockPosition player)))))
