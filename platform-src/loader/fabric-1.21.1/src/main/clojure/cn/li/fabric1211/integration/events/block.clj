(ns cn.li.fabric1211.integration.events.block
  "Fabric block break/place handlers extracted from monolithic events namespace."
  (:require [cn.li.mcbase.integration.event-support :as event-support]
            [cn.li.platform.neutral.event-runtime :as dispatcher]
            [cn.li.mc1211.integration.event-handlers :as event-handlers])
  (:import [net.minecraft.core BlockPos]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block Block]
           [net.minecraft.world.level.block.state BlockState]))

(defn handle-block-break
  [^Level world ^Player player ^BlockPos pos ^BlockState state _be]
  (event-support/guarded-call
    "fabric block break"
    true
    (fn []
      (let [ret (event-handlers/handle-block-break
                  {:x (.getX pos)
                   :y (.getY pos)
                   :z (.getZ pos)
                   :pos pos
                   :player player
                   :world world
                   :block (.getBlock state)}
                  dispatcher/on-block-break
                  "[FABRIC-BLOCK-BREAK]")]
        (not (and (map? ret) (:cancel-break? ret)))))))

(defn handle-block-place-mixin
  [^Player player ^Level world ^BlockPos pos ^Block block]
  (event-support/guarded-call
    "fabric block place"
    false
    (fn []
      (let [ret (event-handlers/handle-block-place
                  {:x (.getX pos)
                   :y (.getY pos)
                   :z (.getZ pos)
                   :pos pos
                   :player player
                   :world world
                   :block block}
                  dispatcher/on-block-place
                  "[FABRIC-BLOCK-PLACE]")]
        (boolean (and (map? ret) (:cancel-place? ret)))))))
