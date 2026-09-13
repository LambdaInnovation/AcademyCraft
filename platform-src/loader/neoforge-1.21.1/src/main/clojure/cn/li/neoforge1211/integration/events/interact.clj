(ns cn.li.neoforge1211.integration.events.interact
  "NeoForge right-click interaction event handlers.

  Left-click/attack/entity-interact suppression is NOT handled here: upstream
  AcademyCraft gates vanilla attack/use exclusively through ControlOverrider
  (our vanilla-input-control SPI), which clears the attack/use KeyMappings only
  for slots that have a skill delegate. This event handler only dispatches AC
  block right-click logic."
  (:require [cn.li.platform.neutral.event-runtime :as dispatcher]
            [cn.li.platform.neutral.event-runtime :as interaction-result]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mc1211.integration.event-handlers :as event-handlers]
            [cn.li.neoforge1211.integration.events.event-apply :as event-apply]
            [cn.li.neoforgebase.integration.events.gui-open-port :as gui-open-port]
            [cn.li.neoforge1211.runtime.owner :as runtime-owner])
  (:import [net.neoforged.neoforge.event.entity.player PlayerInteractEvent$RightClickBlock]
           [net.minecraft.world InteractionHand]))

(defn- is-gui-result?
  [ret]
  (and (map? ret) (contains? ret :gui-id) (contains? ret :player)
       (contains? ret :world) (contains? ret :pos)))

(defn handle-right-click
  [event-data]
  (event-handlers/handle-block-right-click
    event-data
    dispatcher/on-block-right-click
    is-gui-result?
    gui-open-port/open-gui-for-result
    "[RIGHT-CLICK]"))

(defn handle-right-click-event
  [^PlayerInteractEvent$RightClickBlock evt]
  (try
    (let [pos (.getPos evt)
          level (.getLevel evt)
          player (.getEntity evt)
          hand (.getHand evt)]
      (when (= hand InteractionHand/MAIN_HAND)
        (let [block-state (.getBlockState level pos)
              item-stack (.getItemInHand player hand)
              ret (runtime-owner/with-player-owner player (if (.isClientSide level) :client :server)
                    #(handle-right-click
                      {:x (.getX pos)
                       :y (.getY pos)
                       :z (.getZ pos)
                       :pos pos
                       :sneaking (.isShiftKeyDown player)
                       :player player
                       :hand hand
                       :item-stack item-stack
                       :world level
                       :block (.getBlock block-state)}))]
          (when (interaction-result/interaction-consumed? ret)
            (when-not (.isClientSide level)
              (log/debug "[FORGE-RIGHT-CLICK-EVENT] pos=" pos "player=" (.getGameProfile player)
                        "block=" (.getBlock block-state)))
            (event-apply/apply-consumed-right-click! evt (.isClientSide level))))))
    (catch Throwable t
      (log/stacktrace "[FORGE-RIGHT-CLICK-EVENT] EXCEPTION:" t))))
