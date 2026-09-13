(ns cn.li.neoforge262.client.level-effect-renderer
  "CLIENT-ONLY level effect executor. AC owns the effect state and render plan;
  this loader extracts immutable state, then submits its custom geometry."
  (:require [cn.li.mc262.client.effects.level-renderer :as shared-level]
            [cn.li.ac.block.imag-phase.render :as imag-phase-render]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver ResourceLocations]
           [cn.li.mc262.client.render SubmitNodeRenderBufferAdapter]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]
           [net.minecraft.client.renderer.state.level LevelRenderState]
           [net.minecraft.util.context ContextKey]
           [net.neoforged.neoforge.client.event ClientTickEvent$Post
            ExtractLevelRenderStateEvent SubmitCustomGeometryEvent]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]))

(def ^:private ^ContextKey level-effect-plan-key
  (ContextKey. (ResourceLocations/of "academy" "level_effect_plan")))

(def ^:private ^ContextKey imag-phase-camera-key
  (ContextKey. (ResourceLocations/of "academy" "imag_phase_camera")))

(defn- on-client-tick [^ClientTickEvent$Post evt]
  (shared-level/tick-level-effects!))

(defn- camera-position-map
  [^ExtractLevelRenderStateEvent evt]
  (let [position (.position (.getCamera evt))]
    {:x (.-x position)
     :y (.-y position)
     :z (.-z position)}))

(defn- extract-level-plan!
  [^ExtractLevelRenderStateEvent evt]
  (let [^LevelRenderState render-state (.getRenderState evt)
        ^Minecraft minecraft (Minecraft/getInstance)
        ^LocalPlayer player (when minecraft (.player minecraft))
        extracted (when player
                    (shared-level/extract-level-plan!
                      {:player player
                       :camera-pos (camera-position-map evt)
                       :tick (.-gameTime render-state)}))]
              (.setRenderData render-state imag-phase-camera-key (camera-position-map evt))
    (.setRenderData render-state level-effect-plan-key extracted)))

(defn- submit-level-plan!
  [^SubmitCustomGeometryEvent evt]
  (let [^LevelRenderState render-state (.getLevelRenderState evt)
        extracted (.getRenderData render-state level-effect-plan-key)]
    (when extracted
      (shared-level/render-level-plan!
        (assoc extracted
               :pose-stack (.getPoseStack evt)
               :submit-node-collector (.getSubmitNodeCollector evt))))))

(defn- submit-imag-phase-flash!
  [^SubmitCustomGeometryEvent evt]
  (let [^LevelRenderState render-state (.getLevelRenderState evt)
        camera-pos (.getRenderData render-state imag-phase-camera-key)
        pose-stack (.getPoseStack evt)
        adapter (SubmitNodeRenderBufferAdapter.
                 (.getSubmitNodeCollector evt) pose-stack)]
    (try
      (imag-phase-render/draw-pending!
       {:pose-stack pose-stack
        :buffer-source adapter
        :camera-pos camera-pos})
      (finally
        (.finish adapter)))))

(defn- on-extract-level-render-state [^ExtractLevelRenderStateEvent evt]
  (try
    (extract-level-plan! evt)
    (catch Exception e
      (log/debug "Level effect state extraction failed:" (ex-message e)))))

(defn- on-submit-custom-geometry [^SubmitCustomGeometryEvent evt]
  (try
    (submit-level-plan! evt)
    (submit-imag-phase-flash! evt)
    (catch Exception e
      (log/debug "Level geometry submission failed:" (ex-message e)))))

(defn init! []
  (install/process-once! ::tick-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false ClientTickEvent$Post
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-client-tick evt)))))
  (install/process-once! ::extract-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false ExtractLevelRenderStateEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-extract-level-render-state evt)))))
  (install/process-once! ::submit-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false SubmitCustomGeometryEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-submit-custom-geometry evt)))))
  (log/debug "Level effect renderer initialized"))
