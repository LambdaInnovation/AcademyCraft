(ns cn.li.neoforge262.client.init
  "NeoForge 26.2 client init.

   Client setup for session, input, runtime tick, rendering and model hooks."
  (:require [cn.li.platform.neutral.hooks :as power-runtime]
          [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.spi.key-scheme-provider :as key-scheme-spi]
            [cn.li.mcmod.spi.vanilla-input-control :as vanilla-spi]
            [cn.li.platform.bootstrap :as platform-bootstrap]
            [cn.li.platform.neutral.integration-runtime :as tutorial-hooks]
            [cn.li.platform.neutral.client-runtime :as client-bridge]
            [cn.li.platform.neutral.client-runtime :as content-actions]
            [cn.li.neoforge262.runtime.owner :as runtime-owner]
            [cn.li.mcbase.client.session :as mc-session]
            [cn.li.mcbase.client.overlay.state :as overlay-state]
            [cn.li.mc262.client.font.msdf-setup :as msdf-setup]
            [cn.li.mc262.client.i18n :as i18n]
            [cn.li.neoforge262.client.key-mapping-adapter :as key-mapping-adapter]
            [cn.li.mcbase.client.audio.media-playback :as media-playback-bridge]
            [cn.li.mc262.client.effects.particle :as particle]
            [cn.li.mc262.client.effects.sound :as sound]
            [cn.li.mc262.gui.cgui.font :as cgui-font]
            [cn.li.mc262.integration.recipe-query :as recipe-query]
            [cn.li.mc262.key-scheme-provider-core :as key-scheme-core]
            [cn.li.mc262.vanilla-input-control-core :as vanilla-control]
            [cn.li.neoforge262.client.keyboard-event-handler :as keyboard-event-handler]
            [cn.li.neoforge262.client.runtime-bridge :as runtime-bridge]
            [cn.li.neoforge262.client.fov-renderer :as fov-renderer]
            [cn.li.neoforge262.client.hand-effect-renderer :as hand-effect-renderer]
            [cn.li.neoforge262.client.level-effect-renderer :as level-effect-renderer]
            [cn.li.neoforge262.client.overlay-renderer :as overlay-renderer]
            [cn.li.neoforge262.adapter.gui-registry :as gui-registry]
            [cn.li.neoforge262.gui.network.shared :as gui-network]
            [cn.li.neoforge262.gui.screen-impl :as gui-screen-impl]
            [cn.li.mcbase.client.request.bridge :as request-bridge]
            [cn.li.mc262.client.render.pose :as pose-impl]
            [cn.li.mc262.client.render.buffer :as buffer-impl]
            [cn.li.platform.neutral.client-runtime :as pose]
            [cn.li.platform.neutral.client-runtime :as buffer]
            [cn.li.mc262.gui.reactive.host :as reactive-host]
            [cn.li.mc262.gui.reactive.terminal-render :as terminal-render]
            [cn.li.platform.neutral.client-runtime :as widget-registry]
            [cn.li.mcmod.util.render :as render]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.platform.neutral.client-runtime :as render-init]
            [cn.li.platform.neutral.client-runtime :as tesr-api]
            [cn.li.neoforgebase.registry.state :as registry-state])
  (:import [net.minecraft.client Minecraft]
           [cn.li.mcver McAccess McClientAccess]
           [net.minecraft.client.renderer.block FluidModel$Unbaked]
           [net.minecraft.client.resources.model.sprite Material]
           [net.minecraft.client.multiplayer ClientLevel]
           [net.minecraft.client.player LocalPlayer]
           [net.minecraft.network.chat Component]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.level.material FluidState]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.neoforge.client.event RegisterFluidModelsEvent ClientTickEvent$Post]
           [net.neoforged.neoforge.client.fluid FluidTintSource]
           [net.neoforged.neoforge.client.event EntityRenderersEvent$RegisterRenderers]
           [net.neoforged.bus.api EventPriority]
           [com.mojang.blaze3d.platform Window]
           ;; cn.li.mcver.McAccess (server-safe) plus McClientAccess (client-only
           ;; methods, split out so the dedicated server can load McAccess).
           ;; Importing the deprecated cn.li.mc262.bridge.McAccess instead would
           ;; be a hard name collision; its dayTime simply delegates anyway.
           [cn.li.mcver ResourceLocations]
           [cn.li.neoforge262.bridge ClientTimeInterop]
           [cn.li.mc262.client GuiGraphicsHelper ClientHelper]
           [cn.li.mc262.client.render GuiRenderPipelines ScriptedBlockEntityBerProvider]
           [cn.li.neoforge262.client ModClientRenderSetup]))

(defn- bind-texture-forge!
  [texture]
  (ClientHelper/bindTextureForSetup texture))

(defn register-renderers
  []
  (log/debug "Registering block renderers for NeoForge 26.2...")
  (try
    (render/register-texture-binder! bind-texture-forge!)
    (render-init/register-default-renderer-init-fns!)
    (render-init/register-all-renderers!)
    (log/info "Block renderers registered")
    (catch Exception e
      (log/stacktrace "Failed to register block renderers" e))))

(defn register-fluid-render-layers!
  "Register 26.2 FluidModel definitions for each content fluid."
  [^RegisterFluidModelsEvent event]
  (doseq [fluid-id (registry-metadata/get-all-fluid-ids)
          :let [fluid-spec (registry-metadata/get-fluid-spec fluid-id)
                rendering (:rendering fluid-spec)
                still-id (:still-texture rendering)
                flowing-id (:flowing-texture rendering)
                overlay-id (:overlay-texture rendering)
                tint (int (or (:tint-color rendering) -1))
                source (registry-state/get-registered-fluid-source fluid-id)
                flowing (registry-state/get-registered-fluid-flowing fluid-id)]
          :when (and event source flowing still-id flowing-id)]
    (let [still (Material. (ResourceLocations/parse (str still-id)))
          flowing-material (Material. (ResourceLocations/parse (str flowing-id)))
          overlay (when overlay-id
                    (Material. (ResourceLocations/parse (str overlay-id))))
          tint-source (reify FluidTintSource
                        (^int color [_ ^FluidState _fluid-state] tint))
          model (FluidModel$Unbaked. still flowing-material overlay tint-source)]
      (ModClientRenderSetup/registerFluidModel event model source flowing))))

(defn init-render-bindings!
  "Install pose and submit-node-backed render-buffer ops for scripted TESRs."
  []
  (pose/install-pose-ops!
   {:y-rotation pose-impl/rotate-y
    :x-rotation pose-impl/rotate-x
    :z-rotation pose-impl/rotate-z
    :axis-rotation pose-impl/rotate-axis
    :push-pose pose-impl/push-pose
    :pop-pose pose-impl/pop-pose
    :translate pose-impl/translate
    :scale pose-impl/scale
    :get-matrix pose-impl/get-pose-matrix}
   "neoforge-26.2-client")
  (buffer/install-render-buffer-ops!
   {:solid buffer-impl/get-solid-buffer
    :translucent buffer-impl/get-translucent-buffer
    :cutout-no-cull buffer-impl/get-cutout-no-cull-buffer
    :submit-vertex pose-impl/submit-vertex
    :translucent-see-through buffer-impl/get-translucent-see-through-buffer
    :submit-vertex-no-overlay pose-impl/submit-vertex-no-overlay
    :triangle-vertex-order (fn [] [0 1 2 2])}
   "neoforge-26.2-client")
  nil)

(defn ensure-client-render-platform-for-ber!
  []
  (init-render-bindings!))

(defn register-scripted-block-entity-renderers!
  "Bind universal BER for scripted tiles under EntityRenderersEvent.RegisterRenderers."
  [^EntityRenderersEvent$RegisterRenderers evt]
  (ensure-client-render-platform-for-ber!)
  ;; Register the AC renderer loaders while the only BER registration event
  ;; is still open. FMLClientSetupEvent occurs later on 26.2.
  ((platform-bootstrap/client-init-callback!))
  (render-init/register-default-renderer-init-fns!)
  (render-init/register-all-renderers!)
  (log/debug "RegisterRenderers - attaching scripted block entity renderers")
  (when (empty? (tesr-api/scripted-renderers-snapshot))
    (throw (IllegalStateException.
            "Scripted renderer registry is empty before RegisterRenderers event")))
  (doseq [tile-id (registry-metadata/get-all-tile-ids)]
    (let [block-ids (or (seq (registry-metadata/get-tile-block-ids tile-id)) [tile-id])]
      (when (some tesr-api/get-scripted-tile-renderer block-ids)
        (when-let [be-type (registry-state/get-registered-block-entity-type tile-id)]
          (.registerBlockEntityRenderer
            evt
            be-type
            (ScriptedBlockEntityBerProvider/provider))
          (log/debug (str "  BER registered for tile-id " tile-id)))))))

(defn- open-screen-dispatcher
  "Dispatch open-screen to a registered reactive widget factory."
  [arg payload]
  (when (keyword? arg)
    (let [widget (widget-registry/create-widget arg payload)]
      (reactive-host/open-reactive-screen!
        (:runtime widget) (:title widget "Screen") {:on-close (:on-close widget)}))))

(defn- open-reactive-screen-handler [& args]
  (apply reactive-host/open-reactive-screen! args))

(defn init-content-client-bridge!
  "Merge client platform adapters."
  []
  (client-bridge/merge-client-bridge!
    {:open-screen open-screen-dispatcher
     :open-reactive-screen open-reactive-screen-handler
     :slot-key-down runtime-bridge/on-slot-key-down!
     :slot-key-tick runtime-bridge/on-slot-key-tick!
     :slot-key-up runtime-bridge/on-slot-key-up!
     :slot-key-abort runtime-bridge/on-slot-key-abort!
     :movement-key-down runtime-bridge/on-movement-key-down!
     :movement-key-tick runtime-bridge/on-movement-key-tick!
     :movement-key-up runtime-bridge/on-movement-key-up!
     :local-player-item-id runtime-bridge/local-player-item-id
     :local-player-pos runtime-bridge/local-player-pos
     :local-player-eye-pos runtime-bridge/local-player-eye-pos
     :camera-position runtime-bridge/camera-position
     :local-player-look-end runtime-bridge/local-player-look-end
     :local-player-block-aim runtime-bridge/local-player-block-aim
     :camera-raycast-visible? runtime-bridge/camera-raycast-visible?
     :clear-client-activated-overlay runtime-bridge/clear-client-activated-overlay!
     :client-overlay-activated-override
     (fn [_owner]
       (when-let [owner (mc-session/current-local-player-owner)]
         (overlay-state/get-client-activated owner)))
     :client-active-overlay-app
     (fn [_owner]
       (when-let [owner (mc-session/current-local-player-owner)]
         (overlay-state/get-active-overlay-app owner)))
     :run-client-effect (fn [effect-key payload]
                          (case effect-key
                            :mcmod/spawn-local-scripted-effect
                            (runtime-bridge/spawn-local-scripted-effect! (:effect-id payload))

                            :mcmod/spawn-local-scripted-effect-at
                            (runtime-bridge/spawn-local-scripted-effect-at!
                              (:effect-id payload)
                              (:x payload) (:y payload) (:z payload))

                            :mcmod/spawn-scripted-effect-at-player
                            (runtime-bridge/spawn-scripted-effect-at-player!
                              (:effect-id payload) (:owner-uuid payload))

                            :mcmod/move-local-scripted-effect
                            (runtime-bridge/move-local-scripted-effect! (:entity-uuid payload) (:x payload) (:y payload) (:z payload))
                            :mcmod/remove-local-scripted-effect
                            (runtime-bridge/remove-local-scripted-effect! (:entity-uuid payload))

                            :mcmod/get-entity-position
                            (try
                              (McClientAccess/clientEntitySnapshot
                                (java.util.UUID/fromString (:entity-uuid payload)))
                              (catch Exception _ nil))

                            :mcmod/set-client-entity-motion
                            (try
                              (McClientAccess/setClientEntityMotion
                                (java.util.UUID/fromString (:entity-uuid payload))
                                (double (:vx payload)) (double (:vy payload)) (double (:vz payload)))
                              (catch Exception _ false))

                            :mcmod/start-loop-sound
                            (sound/start-loop-sound! (:key payload) (:sound-id payload)
                              (:volume payload) (:pitch payload)
                              (:x payload) (:y payload) (:z payload))

                            :mcmod/start-loop-sound-at-player
                            (sound/start-loop-sound-at-player!
                              (:key payload) (:sound-id payload)
                              (:volume payload) (:pitch payload)
                              (:owner-uuid payload)
                              (get payload :loop? true))

                            :mcmod/update-loop-sound-position
                            (sound/update-loop-sound-position! (:key payload)
                              (:x payload) (:y payload) (:z payload))

                            :mcmod/stop-loop-sound
                            (sound/stop-loop-sound! (:key payload))

                            (log/debug "Unhandled client effect key" effect-key)))
     :get-client-player #(.player (Minecraft/getInstance))
     :local-player-uuid mc-session/local-player-uuid
     :set-active-overlay-app (fn [app-kw player-uuid]
                                (overlay-state/set-active-overlay-app!
                                  {:client-session-id "" :player-uuid (str player-uuid)}
                                  app-kw))
     :screen-active? #(some? (some-> (Minecraft/getInstance) .gui .screen))
     :singleplayer? #(.hasSingleplayerServer (Minecraft/getInstance))
     :settings-key-name key-scheme-core/key-display-name
     :close-screen! (fn [& _]
                       (when-let [^net.minecraft.client.gui.screens.Screen s (some-> (Minecraft/getInstance) .gui .screen)]
                         (.onClose s)))
     :get-mouse-pos (fn []
                      (let [^Minecraft mc (Minecraft/getInstance)
                            mh (.mouseHandler mc)]
                        [(double (.xpos mh)) (double (.ypos mh))]))
     :send-system-message! (fn [player translatable-key & args]
                              (let [^LocalPlayer player player]
                                (.sendSystemMessage player
                                  (Component/translatable translatable-key (into-array Object args)))))
     :game-time-ms (fn []
                     (if-let [^Minecraft mc (Minecraft/getInstance)]
                       (if-let [level (.level mc)]
                         (long (+ (* (McAccess/dayTime level) 50)
                                  (* (double (ClientTimeInterop/getFrameTime mc)) 50.0)))
                         (System/currentTimeMillis))
                       (System/currentTimeMillis)))
     :font-width (fn [^String text]
                   (let [^Minecraft mc (Minecraft/getInstance)]
                     (.width (.font mc) text)))
     :get-window-size (fn []
                       (let [^Minecraft mc (Minecraft/getInstance)
                             ^Window win (.getWindow mc)]
                         [(.getGuiScaledWidth win) (.getGuiScaledHeight win)]))
     :register-font! (fn [name spec]
                       (cgui-font/register-font! name spec))
     :get-player-owner #(mc-session/current-local-player-owner)
     :font-text-width (fn [font-desc text font-size]
                        (cgui-font/text-width font-desc text font-size))
     :resolve-shader (fn [shader-name]
                       (case shader-name
                         :ring-progbar (GuiRenderPipelines/skillProgbar)
                         :skill-progbar (GuiRenderPipelines/skillProgbar)
                         ;; Depth-layering pipelines are NOT resolved on 26.2:
                         ;; every attempt to make the skill tree's depth
                         ;; stamp-then-test chain work in the extracted GUI
                         ;; pass has failed in practice (byte-exact depth
                         ;; alignment, pipeline sort-key ordering for the
                         ;; stable element sort, and verification of the GL
                         ;; depth state application all check out, yet the
                         ;; EQUAL/NOT_EQUAL-tested icons and lines never
                         ;; render — the mechanism is unusable here, unlike
                         ;; 1.20.1/1.21.1's immediate-GL passes). Keep the
                         ;; documented painter's-order degradation (1.21.1
                         ;; render.clj depth-layering notes; identical to the
                         ;; Fabric 26.2 behavior). The pipelines and shaders
                         ;; remain registered for any future GUI depth use.
                         :mono nil
                         :cpbar-overload (GuiRenderPipelines/cpbarOverload)
                         :alpha-discard nil
                         :depth-equal nil
                         :depth-notequal nil
                         nil))
     :stop-all-media! (fn [_player-uuid]
                        (sound/stop-all-media!))
     :has-recipes? (fn [item-id]
                     (recipe-query/has-recipes? item-id))
     :first-recipe-for (fn [item-id recipe-kind]
                         (recipe-query/first-recipe-for item-id recipe-kind))
     :all-recipes-for (fn [item-id recipe-kind]
                        (recipe-query/all-recipes-for item-id recipe-kind))
     :find-recipes (fn [item-id]
                     (recipe-query/find-recipes item-id))
     :blit-textured-quad! (fn [graphics texture x1 y1 x2 y2 z u0 u1 v0 v1]
                            (GuiGraphicsHelper/blitTexturedQuad
                              graphics texture
                              (float x1) (float y1) (float x2) (float y2) (float z)
                              (float u0) (float u1) (float v0) (float v1)))
     :is-glfw-key-down? (fn [key-code]
                          (try
                            (let [^Minecraft mc (Minecraft/getInstance)
                                  ^Window w (.getWindow mc)
                                  handle (.handle w)]
                              (= 1 (org.lwjgl.glfw.GLFW/glfwGetKey handle (int key-code))))
                            (catch Throwable _ false)))
     :terminal-apply-perspective! terminal-render/apply-perspective!
     :terminal-render-cursor! terminal-render/render-cursor!
     :terminal-cursor-hide! terminal-render/hide-cursor!
     :terminal-cursor-show! terminal-render/show-cursor!
     :keybind-rebind-supported? (constantly true)
     :keybind-get-key-name key-mapping-adapter/get-key-display-name
     :keybind-get-key-code key-mapping-adapter/get-key-code
     :keybind-set-key! key-mapping-adapter/set-key-mapping-key!
     :keybind-conflict? key-mapping-adapter/binding-conflict?})
  nil)

(defn- install-client-owner-hooks!
  []
  (gui-registry/install-client-owner-wrapper! mc-session/with-current-client-owner)
  (runtime-owner/install-client-owner-functions!
    {:client-session-id mc-session/client-session-id
     :with-bound-client-owner mc-session/with-bound-client-owner})
  (gui-network/install-client-owner-functions!
    {:client-session-id mc-session/client-session-id
     :local-player-uuid mc-session/local-player-uuid
     :with-bound-client-owner mc-session/with-bound-client-owner}))

(defn- install-tutorial-activated-bridge!
  []
  (tutorial-hooks/register-tutorial-activated-hook!
    (fn [player-uuid tut-id]
      (try
        (let [uuid (java.util.UUID/fromString player-uuid)
              mc (Minecraft/getInstance)
              player (if (and mc (.hasSingleplayerServer mc))
                       (some-> mc .getSingleplayerServer .getPlayerList (.getPlayer uuid))
                       (when-let [^ClientLevel level (some-> mc .level)]
                         (some (fn [^Player p]
                                 (when (= (str (.getUUID p)) (str uuid)) p))
                               (.players level))))]
          (when player
            (.post NeoForge/EVENT_BUS
                   (cn.li.neoforgebase.event.TutorialActivatedEvent. player (name tut-id)))))
        (catch Throwable e
          (log/stacktrace "install-tutorial-activated-bridge!: hook callback failed" e))))))

(defn init-client-input-systems!
  []
  (try
    (key-scheme-spi/install-provider! (key-scheme-core/get-spi-implementation))
    (vanilla-spi/install-suppressor! (vanilla-control/get-spi-implementation))
    (catch Exception e
      (log/warn e "Failed to install keyboard input SPI providers")
      (log/stacktrace "Failed to install keyboard input SPI providers" e)))

  (try
    ((platform-bootstrap/post-spi-client-init-callback!))
    (catch Exception e
      (log/stacktrace "Failed to run post-SPI content keybinding init" e)
      (log/stacktrace "Failed to run post-SPI content keybinding init" e)))

  (try
    (key-mapping-adapter/register-all-keybindings-from-ac!)
    (key-mapping-adapter/register-into-system-menu!)
    (key-mapping-adapter/install-bound-key-resolver!)(catch Exception e
      (log/stacktrace "Failed to register Forge KeyMappings" e)
      (log/stacktrace "Failed to register Forge KeyMappings" e)))

  (try
    (keyboard-event-handler/install-forge-event-handler!)
    (catch Exception e
      (log/stacktrace "Failed to install Forge keyboard event handler" e)
      (log/stacktrace "Failed to install Forge keyboard event handler" e))))

(defn init-client
  "Initialize client-side systems for NeoForge 26.2 (non-render subset)."
  []
  (install/process-once! ::client-systems-initialized
    (fn []
      (log/debug "Initializing NeoForge 26.2 client-side systems (non-render path)")

      (mc-session/init-default-owner-resolver!)
      (install-client-owner-hooks!)
      (install-tutorial-activated-bridge!)
      (init-client-input-systems!)

      (init-content-client-bridge!)
      (terminal-render/install-terminal-render-bridge!)
      (init-render-bindings!)
      (render/register-texture-binder! bind-texture-forge!)
      (gui-screen-impl/init-client!)
      (i18n/install-client-i18n!)

      (try
        ((platform-bootstrap/client-init-callback!))
        (catch Exception e
          (log/stacktrace "Failed to run content client init" e)
          (log/stacktrace "Failed to run content client init" e)))
      ;; MSDF font registration needs the full bridge (the content client-init
      ;; hook may have fired earlier, before the bridge ops existed) — retry now
      ;; that the bridge is complete.
      (power-runtime/client-font-init!)
      (msdf-setup/init!)

      (runtime-bridge/init!)
      (overlay-renderer/init!)
      (particle/init!)
      (sound/init!)
      (media-playback-bridge/install-media-playback-bridge!)
      (hand-effect-renderer/init!)
      (level-effect-renderer/init!)
      (fov-renderer/init!)
      (request-bridge/init!)

      (try
        (.addListener (NeoForge/EVENT_BUS)
                      EventPriority/NORMAL
                      false
                      ClientTickEvent$Post
                      (reify java.util.function.Consumer
                        (accept [_ evt]
                          (mc-session/with-current-client-session
                            content-actions/run-client-tick-hooks!))))
        (catch Throwable _
          (log/warn "Failed to register client tick hooks")))

      (log/info "NeoForge 26.2 client init complete"))))

(defn init! [& _] (init-client))
