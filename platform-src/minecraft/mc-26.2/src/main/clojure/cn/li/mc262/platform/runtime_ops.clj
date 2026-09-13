(ns cn.li.mc262.platform.runtime-ops
  "Install versioned RuntimeAccess helpers into shared runtime-ops."
  (:require [cn.li.mcbase.platform.runtime-ops :as shared])
  (:import [cn.li.mc262.runtime BlockRegistry ItemInventory
            ItemRegistry ParticleEntity RuntimeAccess]))

(shared/install-runtime-ops!
  {:playerRaytraceBlock (fn [& args] (apply RuntimeAccess/playerRaytraceBlock args))
   :getEntityLevel (fn [& args] (apply RuntimeAccess/getEntityLevel args))
   :getBlockKey (fn [& args] (apply BlockRegistry/getBlockKey args))
   :getEntityClass (fn [] (RuntimeAccess/getEntityClass))
   :getPlayerClass (fn [] (RuntimeAccess/getPlayerClass))
   :getServerPlayerClass (fn [] (RuntimeAccess/getServerPlayerClass))
   :getInventoryClass (fn [] (RuntimeAccess/getInventoryClass))
   :getAbstractContainerMenuClass (fn [] (RuntimeAccess/getAbstractContainerMenuClass))
   :getItemStackClass (fn [] (RuntimeAccess/getItemStackClass))
   :getItemClass (fn [] (RuntimeAccess/getItemClass))
   :getBlockStateClass (fn [] (RuntimeAccess/getBlockStateClass))
   :getLevelClass (fn [] (RuntimeAccess/getLevelClass))
   :getItemKeyString (fn [& args] (apply ItemInventory/getItemKeyString args))
   :itemStackOf (fn [& args] (apply RuntimeAccess/itemStackOf args))
   :createItemStackById (fn [& args] (apply ItemRegistry/createItemStackById args))
   :isItemStackEmpty (fn [& args] (apply ItemInventory/isItemStackEmpty args))
   :getPlayerContainerMenu (fn [& args] (apply RuntimeAccess/getPlayerContainerMenu args))
   :spawnEntityByIdFromPlayer (fn [& args] (apply ParticleEntity/spawnEntityByIdFromPlayer args))
   :spawnTrackedEntityByIdFromPlayer (fn [& args] (apply ParticleEntity/spawnTrackedEntityByIdFromPlayer args))
   :getInventoryPlayer (fn [& args] (apply RuntimeAccess/getInventoryPlayer args))
   :getMenuContainerId (fn [& args] (apply RuntimeAccess/getMenuContainerId args))})

(def standard-runtime-ops shared/standard-runtime-ops)
