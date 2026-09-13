(ns cn.li.fabric262.runtime.block-manipulation
  "Fabric block-manipulation runtime for IBlockManipulation protocol.
  Delegates to mc262 adapter.block-manipulation using Fabric server-context.
  break-guard uses vanilla build and spawn-protection checks."
  (:require [cn.li.mc262.runtime.block-manipulation-core]
            [cn.li.mcbase.runtime.adapter.block-manipulation :as block-manipulation]
            [cn.li.fabric262.adapter.server-context :as server-ctx]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.core BlockPos]
           [net.minecraft.server.level ServerLevel ServerPlayer]))

(defn- fabric-break-guard [^ServerLevel level ^BlockPos pos ^ServerPlayer player]
  (try
    (and (.mayBuild player)
         (not (.isEmptyBlock level pos))
         (let [server (.getServer level)]
           (if server
             (not (.isUnderSpawnProtection server level pos player))
             true)))
    (catch Exception e
      (log/warn "Fabric block break guard failed:" (ex-message e))
      false)))

(defn fabric-block-manipulation []
  (block-manipulation/create-block-manipulation
    server-ctx/get-server
    fabric-break-guard))

(defn install-block-manipulation! []
  (block-manipulation/install-block-manipulation! (fabric-block-manipulation)
                                                  "Fabric block manipulation"))
