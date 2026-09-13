(ns cn.li.fabric1201.client.level-effect-renderer
  "CLIENT-ONLY Fabric level effect renderer adapter."
  (:require [cn.li.mc1201.client.effects.level-renderer :as shared-level]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [com.mojang.blaze3d.vertex PoseStack]
           [net.fabricmc.fabric.api.client.event.lifecycle.v1 ClientTickEvents ClientTickEvents$EndTick]
           [net.fabricmc.fabric.api.client.rendering.v1 WorldRenderContext WorldRenderEvents WorldRenderEvents$AfterTranslucent]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]
           [net.minecraft.client.renderer MultiBufferSource$BufferSource]))

(defn- on-client-tick []
  (shared-level/tick-level-effects!))

(defn- on-after-translucent-render [^WorldRenderContext ctx]
  (try
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [^LocalPlayer player (.player mc)]
        (let [camera (.camera ctx)
              cam-vec (.getPosition camera)
              cam-pos {:x (.-x cam-vec) :y (.-y cam-vec) :z (.-z cam-vec)}
              tick (.getGameTime (.level player))
              ^PoseStack pose-stack (.matrixStack ctx)
              ^MultiBufferSource$BufferSource buffer-source (or (.consumers ctx)
                                                                 (.bufferSource (.renderBuffers mc)))]
          (when (and pose-stack buffer-source)
            (shared-level/render-level-plan!
             {:player player
              :pose-stack pose-stack
              :buffer-source buffer-source
              :camera-pos cam-pos
              :tick tick})))))
    (catch Exception e
      (log/debug "Fabric level effect render failed:" (ex-message e)))))

(defn init!
  []
  (install/process-once! ::tick-listener-registered
    #(.register ClientTickEvents/END_CLIENT_TICK
                (reify ClientTickEvents$EndTick
                  (onEndTick [_ _client]
                    (on-client-tick)))))
  (install/process-once! ::render-listener-registered
    #(.register WorldRenderEvents/AFTER_TRANSLUCENT
                (reify WorldRenderEvents$AfterTranslucent
                  (afterTranslucent [_ context]
                    (on-after-translucent-render context)))))
  (log/debug "Fabric level effect renderer initialized"))
