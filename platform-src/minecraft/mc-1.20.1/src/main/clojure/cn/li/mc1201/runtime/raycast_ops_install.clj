(ns cn.li.mc1201.runtime.raycast-ops-install
  "Install versioned Raycast Java helpers into shared raycast-core."
  (:require [cn.li.mcbase.runtime.raycast-core :as raycast-core])
  (:import [cn.li.mc1201.runtime Raycast]))

(defn install!
  []
  (raycast-core/install-raycast-ops!
    {:raycastBlocks (fn [& args] (apply Raycast/raycastBlocks args))
     :raycastBlocksMatching (fn [& args] (apply Raycast/raycastBlocksMatching args))
     :raycastCollidableBlocksOrWater (fn [& args] (apply Raycast/raycastCollidableBlocksOrWater args))
     :raycastEntities (fn [& args] (apply Raycast/raycastEntities args))
     :raycastCombined (fn [& args] (apply Raycast/raycastCombined args))
     :raycastCombinedExcluding (fn [& args] (apply Raycast/raycastCombinedExcluding args))
     :raycastCombinedAll (fn [& args] (apply Raycast/raycastCombinedAll args))
     :raycastCombinedFromPlayer (fn [& args] (apply Raycast/raycastCombinedFromPlayer args))
     :getPlayerLookVector (fn [& args] (apply Raycast/getPlayerLookVector args))
     :getPlayerPosition (fn [& args] (apply Raycast/getPlayerPosition args))
     :raycastFromPlayer (fn [& args] (apply Raycast/raycastFromPlayer args))})
  nil)

(install!)
