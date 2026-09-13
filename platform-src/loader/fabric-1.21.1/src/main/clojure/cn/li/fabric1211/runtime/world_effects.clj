(ns cn.li.fabric1211.runtime.world-effects
  "Fabric implementation of IWorldEffects protocol."
  (:require [cn.li.fabric1211.adapter.server-context :as server-context]
            [cn.li.mcbase.runtime.adapter.world-effects :as world-effects]
            [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.level Level$ExplosionInteraction]
           [net.minecraft.world.entity Entity EntityType LightningBolt]
           [net.minecraft.world.level.block Block]
           [net.minecraft.world.phys AABB]))

(defn- get-server ^MinecraftServer []
  (server-context/get-server))

(defn fabric-world-effects []
  (world-effects/create-world-effects
    get-server
    {:resolve-level-fn (fn [server world-id] (query-core/resolve-level-strict server world-id))
     :spawn-lightning-fn (fn [^ServerLevel level x y z visual-only?]
                           (let [^LightningBolt bolt (.create EntityType/LIGHTNING_BOLT level)]
                             (when bolt
                               (.moveTo bolt (double x) (double y) (double z))
                               (.setVisualOnly bolt (boolean visual-only?))
                               (boolean (.addFreshEntity level bolt)))))
     :create-explosion-fn (fn [^ServerLevel level ^Entity source x y z radius fire? terrain?]
                            (try
                              (.explode level source (double x) (double y) (double z)
                                        (float radius) (boolean fire?)
                                        (if terrain?
                                          Level$ExplosionInteraction/MOB
                                          Level$ExplosionInteraction/NONE))
                              true
                              (catch Throwable _
                                (.explode level source (double x) (double y) (double z)
                                          (float radius)
                                          (if terrain?
                                            Level$ExplosionInteraction/MOB
                                            Level$ExplosionInteraction/NONE))
                                true)))
     :get-entities-in-aabb-fn (fn [^ServerLevel l ^AABB aabb]
                                (.getEntities l nil aabb))
    :resolve-entity-id-fn (fn [^Entity entity]
                             (str (.getDescriptionId (.getType entity))))
    :block-id-fn (fn [^Block block _block-state]
                    (str (.getDescriptionId block)))}))

(defn install-world-effects! []
  (install/framework-once! ::installed
    #(do
       (server-context/install-server-context!)
       (world-effects/install-world-effects! (fabric-world-effects)
                                             "Fabric world effects")))
  nil)
