(ns cn.li.neoforge1211.runtime.block-manipulation
  "Forge block-manipulation runtime for IBlockManipulation protocol.
  Loader-agnostic ops delegate to mc1211 adapter.block-manipulation.
  Break/can-break use Forge BlockEvent$BreakEvent inline."
  (:require [cn.li.mc1211.runtime.block-manipulation-core]
            [cn.li.mcbase.runtime.adapter.block-manipulation :as block-manipulation]
            [cn.li.neoforgebase.adapter.server-context :as server-context])
  (:import [net.minecraft.server.level ServerLevel ServerPlayer]
           [net.minecraft.core BlockPos]
           [net.neoforged.neoforge.event.level BlockEvent$BreakEvent]
           [cn.li.neoforgebase.bridge ForgeRuntimeBridge]))

;; Forge-specific break guard: fires BlockEvent$BreakEvent
(defn- forge-break-guard [^ServerLevel level ^BlockPos pos ^ServerPlayer player]
  (let [state (.getBlockState level pos)
        event (BlockEvent$BreakEvent. level pos state player)]
    (ForgeRuntimeBridge/postEvent event)
    (not (.isCanceled event))))

(defn forge-block-manipulation []
  (block-manipulation/create-block-manipulation
    server-context/get-server
    forge-break-guard))

(defn install-block-manipulation! []
  (block-manipulation/install-block-manipulation! (forge-block-manipulation)
                                                  "Forge block manipulation"))
