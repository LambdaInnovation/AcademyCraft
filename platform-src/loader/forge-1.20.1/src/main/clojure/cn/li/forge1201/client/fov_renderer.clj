(ns cn.li.forge1201.client.fov-renderer
  "CLIENT-ONLY camera FOV offset while skills charge (meltdowner).

  Subscribes to ViewportEvent.ComputeFov (the modern Forge way to modify the
  camera's field of view) and adds the per-frame offset contributed by the
  local player's active level effects. The offset eases up while charging and
  back to 0 on release/abort, so no bookkeeping is needed here."
  (:require [cn.li.mc1201.client.effects.level-renderer :as shared-level]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]
           [net.minecraftforge.client.event ViewportEvent$ComputeFov]
           [net.minecraftforge.common MinecraftForge]
           [net.minecraftforge.eventbus.api EventPriority]))

(defn- on-compute-fov [^ViewportEvent$ComputeFov evt]
  (try
    (when-let [^LocalPlayer player (some-> (Minecraft/getInstance) .player)]
      (let [offset (double (shared-level/current-fov-offset (str (.getUUID player))))]
        (when (pos? offset)
          (.setFOV evt (float (+ (.getFOV evt) offset))))))
    (catch Exception e
      (log/debug "FOV offset failed:" (ex-message e)))))

(defn init! []
  (install/process-once! ::fov-listener-registered
    #(.addListener (MinecraftForge/EVENT_BUS)
                   EventPriority/NORMAL false ViewportEvent$ComputeFov
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-compute-fov evt)))))
  (log/debug "FOV renderer initialized"))
