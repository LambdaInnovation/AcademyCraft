(ns cn.li.forge1201.runtime.entity-damage
  "Forge implementation of IEntityDamage protocol."
  (:require [cn.li.mc1201.runtime.adapter.entity-damage :as entity-damage]
            [cn.li.forge1201.adapter.server-context :as server-context])
  (:import [cn.li.mc1201.runtime WorldEntity]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.entity Entity]
           [net.minecraft.world.phys AABB]))

(defn forge-entity-damage []
  (entity-damage/create-entity-damage
    server-context/get-server
    {:get-living-entities-in-aabb-fn (fn [^ServerLevel level ^AABB aabb]
                                       (WorldEntity/getLivingEntitiesInAabb level aabb))
     :living-entity?-fn (fn [entity] (WorldEntity/isLivingEntity ^Entity entity))}))

(defn install-entity-damage! []
  (entity-damage/install-entity-damage! (forge-entity-damage)
                                        "Forge entity damage"))
