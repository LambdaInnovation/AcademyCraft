(ns cn.li.mc1201.runtime.ender-dragon-parts-install
  "Install versioned EnderDragonParts into shared multipart-entity."
  (:require [cn.li.mcbase.runtime.multipart-entity :as multipart])
  (:import [cn.li.mcver EnderDragonParts]
           [net.minecraft.world.entity Entity]))

(defn install!
  []
  (multipart/install-ender-dragon-parent-resolver!
    (fn [entity]
      (when (instance? Entity entity)
        (EnderDragonParts/parentOrNull ^Entity entity))))
  nil)

(install!)
