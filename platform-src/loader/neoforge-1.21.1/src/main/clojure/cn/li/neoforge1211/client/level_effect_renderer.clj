(ns cn.li.neoforge1211.client.level-effect-renderer
  "CLIENT-ONLY level effect executor. AC owns the effect state and render plan;
  this loader only subscribes the events and delegates to the shared renderer."
  (:require [cn.li.mc1211.client.effects.level-renderer :as shared-level]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [com.mojang.blaze3d.vertex PoseStack]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]
           [net.minecraft.client.renderer MultiBufferSource$BufferSource]
           [net.neoforged.neoforge.client.event RenderLevelStageEvent RenderLevelStageEvent$Stage]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.neoforge.client.event ClientTickEvent$Post]
           [net.neoforged.bus.api EventPriority]))

(defn- render-stage-eligible? [^RenderLevelStageEvent evt]
  ;; Stage is a fixed enum-like singleton set (RenderLevelStageEvent$Stage) —identical?
  ;; avoids a fresh string allocation + two substring scans on every stage dispatch
  ;; this tick (~9 stages/frame), most of which are not eligible.
  ;;
  ;; Single stage only: the shared renderer doesn't branch on stage, so
  ;; accepting both AFTER_PARTICLES and AFTER_TRANSLUCENT_BLOCKS ran the
  ;; entire build-plan + vertex-emission pipeline twice per frame and
  ;; double-blended translucent quads. AFTER_TRANSLUCENT_BLOCKS is correct
  ;; for translucent beam/line geometry that must composite after translucent
  ;; terrain (water/glass) —matches Fabric's WorldRenderEvents/AFTER_TRANSLUCENT.
  (identical? (.getStage evt) RenderLevelStageEvent$Stage/AFTER_TRANSLUCENT_BLOCKS))

(defn- render-level-plan! [^RenderLevelStageEvent evt]
  (when (render-stage-eligible? evt)
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [^LocalPlayer player (.player mc)]
        (let [camera (.getMainCamera (.gameRenderer mc))
              cam-vec (.getPosition camera)
              cam-pos {:x (.-x cam-vec) :y (.-y cam-vec) :z (.-z cam-vec)}
              tick (.getGameTime (.level player))
              ^PoseStack pose-stack (.getPoseStack evt)
              ^MultiBufferSource$BufferSource buffer-source (.bufferSource (.renderBuffers mc))]
          (shared-level/render-level-plan!
           {:player player
            :pose-stack pose-stack
            :buffer-source buffer-source
            :camera-pos cam-pos
            :tick tick}))))))

(defn- on-client-tick [^ClientTickEvent$Post evt]
  (shared-level/tick-level-effects!))

(defn- on-render-level-stage [^RenderLevelStageEvent evt]
  (try
    (render-level-plan! evt)
    (catch Exception e
      (log/debug "Level effect render failed:" (ex-message e)))))

(defn init! []
  (install/process-once! ::tick-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false ClientTickEvent$Post
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-client-tick evt)))))
  (install/process-once! ::render-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false RenderLevelStageEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-render-level-stage evt)))))
  (log/debug "Level effect renderer initialized"))