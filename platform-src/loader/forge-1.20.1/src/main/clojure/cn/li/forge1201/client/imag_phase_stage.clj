(ns cn.li.forge1201.client.imag-phase-stage
  "CLIENT-ONLY: post-translucent stage renderer for the Imag Phase pool flash.

  The imag-phase TESR runs in the block-entity pass, which 1.20.1 renders
  BEFORE the translucent terrain layer — the pool's opaque black fluid
  surface draws over it and hides the flash. This hook fires at
  RenderLevelStageEvent AFTER_TRANSLUCENT_BLOCKS (after the fluid surface,
  matching upstream's pass-1 TESR position) and drains the per-frame queue of
  pool cells into `imag-phase-render/draw-pending!`, then flushes the
  buffer source immediately.

  The immediate flush is load-bearing: without it the flash batch sits
  pending in the BufferSource until the next flush-all — on FANCY graphics
  that lands right after the translucent layer (flash ends up visible, one
  frame late) but on FABULOUS it lands at the START of the NEXT frame's
  translucent phase, where the translucent target is cleared right after,
  erasing the flash entirely. Flushing here draws with the current output
  target (the translucent target while the terrain's render state is still
  active, the main target otherwise) so the flash composites over the
  surface and is blitted with it."
  (:require [cn.li.ac.block.imag-phase.render :as imag-phase-render]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client Minecraft]
           [net.minecraftforge.client.event RenderLevelStageEvent RenderLevelStageEvent$Stage]
           [net.minecraftforge.common MinecraftForge]
           [net.minecraftforge.eventbus.api EventPriority]))

;; AFTER_TRANSLUCENT_BLOCKS fires at the end of each renderSectionLayer
;; (translucent) — twice per frame (the 1.20.1 level renders the translucent
;; layer in two passes). AFTER_PARTICLES is dispatched directly in renderLevel
;; (bytecode-verified) and also lands after the fluid surface. Subscribing to
;; both is safe: `draw-pending!` drains the per-frame queue on the first fire,
;; so the later fires in the same frame find it empty and draw nothing.
(def ^:private stage-eligible?
  (fn [^RenderLevelStageEvent evt]
    (let [stage (.getStage evt)]
      (or (identical? stage RenderLevelStageEvent$Stage/AFTER_TRANSLUCENT_BLOCKS)
          (identical? stage RenderLevelStageEvent$Stage/AFTER_PARTICLES)))))

(defn- on-render-level-stage [^RenderLevelStageEvent evt]
  (when (stage-eligible? evt)
    (try
      (let [^Minecraft mc (Minecraft/getInstance)
            camera (.getMainCamera (.gameRenderer mc))
            cam (.getPosition camera)
            buffer-source (.bufferSource (.renderBuffers mc))]
        ;; Flush only when the queue actually had cells — the stage fires
        ;; twice per frame and the second fire finds the queue empty (drained
        ;; at the first), so its flush would be a pointless no-op.
        (when (pos? (imag-phase-render/draw-pending!
                      {:pose-stack (.getPoseStack evt)
                       :buffer-source buffer-source
                       :camera-pos {:x (.x cam) :y (.y cam) :z (.z cam)}}))
          ;; See the ns docstring: the batch must draw NOW, while the current
          ;; output target is still the right one (the translucent target
          ;; under FABULOUS, the main target under FANCY).
          (.endBatch buffer-source)))
      (catch Exception e
        (log/debug "Imag phase stage render failed:" (ex-message e))))))

(defn init! []
  (install/process-once! ::stage-listener-registered
    #(.addListener (MinecraftForge/EVENT_BUS)
                   EventPriority/NORMAL false RenderLevelStageEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-render-level-stage evt))))))
