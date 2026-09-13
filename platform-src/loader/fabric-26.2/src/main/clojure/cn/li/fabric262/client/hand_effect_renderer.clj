(ns cn.li.fabric262.client.hand-effect-renderer
  "CLIENT-ONLY Fabric hand-effect adapter.

  Fabric does not expose a Forge-style RenderHandEvent in this project path,
  so this adapter currently applies shared hand-effect ticking and camera pitch
  deltas, while first-person hand transform rendering remains Forge-specific."
  (:require [cn.li.mcbase.client.effects.hand :as hand]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.fabric.api.client.event.lifecycle.v1 ClientTickEvents ClientTickEvents$EndTick]
           [net.minecraft.client Minecraft]))

(defn- on-client-tick []
  (hand/tick-hand-effects!)
  (when-let [mc (Minecraft/getInstance)]
    (when-let [player (.player mc)]
      (hand/apply-camera-pitch-deltas! player))))

(defn init!
  []
  (install/process-once! ::tick-listener-registered
    #(.register ClientTickEvents/END_CLIENT_TICK
                (reify ClientTickEvents$EndTick
                  (onEndTick [_ _client]
                    (on-client-tick)))))
  (log/debug "Fabric hand effect renderer initialized"))
