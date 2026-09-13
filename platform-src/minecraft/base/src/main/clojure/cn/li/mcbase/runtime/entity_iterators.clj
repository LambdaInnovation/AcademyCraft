(ns cn.li.mcbase.runtime.entity-iterators
  "Shared Minecraft-side entity iteration/query helpers."
  (:require [cn.li.mcbase.runtime.entity-lookup :as lookup]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.world.entity Entity]))

(defn entity-type-id-for-entity
  [^Entity entity]
  (when entity
    (str (.getKey BuiltInRegistries/ENTITY_TYPE (.getType entity)))))

(defn entity-type-id
  [^MinecraftServer server world-id entity-uuid]
  (when-let [level (lookup/resolve-level server world-id)]
    (some-> (lookup/get-entity-by-uuid level entity-uuid)
            (entity-type-id-for-entity))))

(defn create-entity-type-id-fn
  "Create a platform-neutral entity type lookup function backed by a server supplier."
  [get-server]
  (fn [world-id entity-uuid]
    (try
      (entity-type-id (get-server) world-id entity-uuid)
      (catch Exception e
        (log/warn "Failed to query entity type id:" world-id entity-uuid (ex-message e))
        nil))))
