(ns cn.li.fabric1201.runtime.entity-damage
  "Fabric implementation of IEntityDamage protocol."
  (:require [cn.li.fabric1201.adapter.server-context :as server-context]
            [cn.li.mc1201.runtime.adapter.entity-damage :as entity-damage])
  (:import [net.minecraft.world.entity LivingEntity]
           [net.minecraft.world.level Level]
           [net.minecraft.world.phys AABB]))

(defn fabric-entity-damage []
  (entity-damage/create-entity-damage
    server-context/get-server
    {:get-living-entities-in-aabb-fn (fn [^Level level ^AABB aabb]
                                       (.getEntitiesOfClass level LivingEntity aabb))
     :living-entity?-fn #(instance? LivingEntity %)}))

(defn install-entity-damage! []
  (server-context/install-server-context!)
  (entity-damage/install-entity-damage! (fabric-entity-damage)
                                        "Fabric entity damage"))
