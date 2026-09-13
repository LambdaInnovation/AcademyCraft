(ns cn.li.fabric1211.integration.events.interact
  "Fabric interaction event handlers extracted from monolithic events namespace.

  Attack-block/attack-entity/use-entity suppression is NOT handled here:
  upstream AcademyCraft gates vanilla attack/use exclusively through
  ControlOverrider (our vanilla-input-control SPI), which clears the
  attack/use KeyMappings only for slots that have a skill delegate. This
  namespace only dispatches AC block right-click logic."
  (:require [cn.li.platform.neutral.event-runtime :as dispatcher]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.event-runtime :as interaction-result]
            [cn.li.mc1211.integration.event-handlers :as event-handlers]
            [cn.li.fabric1211.adapter.gui-registry :as gui-registry-impl])
  (:import [net.minecraft.core BlockPos]
           [net.minecraft.world InteractionHand InteractionResult]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block.state BlockState]
           [net.minecraft.world.phys BlockHitResult]))

(defn- is-gui-result?
  [ret]
  (interaction-result/gui-open-result? ret))

(defn- open-gui-for-result
  [gui-id player ^Level world _pos tile-entity]
  (when (and tile-entity (not (.isClientSide world)))
    (gui-registry-impl/open-gui-for-player player gui-id tile-entity)))

(defn handle-right-click
  [event-data]
  (event-handlers/handle-block-right-click
    event-data
    dispatcher/on-block-right-click
    is-gui-result?
    open-gui-for-result
    ""))

(defn handle-use-block
  [^Player player ^Level world ^InteractionHand hand ^BlockHitResult hit-result]
  (try
    (let [^BlockPos pos (.getBlockPos hit-result)
          ^BlockState block-state (.getBlockState world pos)
          item-stack (.getItemInHand player hand)
          ret (handle-right-click
                {:x (.getX pos)
                 :y (.getY pos)
                 :z (.getZ pos)
                 :pos pos
                 :sneaking (.isShiftKeyDown player)
                 :player player
                 :hand hand
                 :item-stack item-stack
                 :world world
                 :block (.getBlock block-state)})]
      (if (interaction-result/interaction-consumed? ret)
        InteractionResult/SUCCESS
        InteractionResult/PASS))
    (catch Throwable t
      (log/stacktrace "Error handling use block event" t)
      (.printStackTrace t)
      InteractionResult/PASS)))
