(ns cn.li.neoforge262.client.overlay-renderer
  "CLIENT-ONLY Forge overlay event adapter — drives reactive overlay-host.

  26.2: RenderGuiEvent$Post supplies GuiGraphicsExtractor (not GuiGraphics);
  overlay-host layout, clock, and render-tape submission are live."
  (:require [cn.li.mc262.gui.reactive.overlay-host :as overlay-host]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.client-runtime :as client-bridge]
            [cn.li.mcmod.util.log :as log])
  (:import [net.neoforged.neoforge.client.event RenderGuiEvent$Post]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
           [net.minecraft.client Minecraft]
           [cn.li.neoforge262.bridge ClientTimeInterop]))

(defn- bridge-build-fn [w h]
  (client-bridge/reactive-overlay-build w h))

(defn- bridge-update-fn [rt]
  (client-bridge/reactive-overlay-update rt))

(defn- on-render-gui-overlay [^RenderGuiEvent$Post event]
  (let [^Minecraft mc (Minecraft/getInstance)
        w (.getGuiScaledWidth (.getWindow mc))
        h (.getGuiScaledHeight (.getWindow mc))
        pt (ClientTimeInterop/getFrameTime mc)]
    (client-session/with-current-client-session
      #(overlay-host/update-overlay!
         (.getGuiGraphics event) "default" w h pt
         bridge-build-fn bridge-update-fn))))

(defn on-mode-switch-key-state!
  ([is-down]
   (client-bridge/reactive-overlay-mode-switch! is-down))
  ([_owner is-down]
   (client-bridge/reactive-overlay-mode-switch! is-down)))

(defn init! []
  (.addListener (NeoForge/EVENT_BUS)
                EventPriority/NORMAL false RenderGuiEvent$Post
                (reify java.util.function.Consumer
                  (accept [_ evt] (on-render-gui-overlay evt))))
  (log/debug "Reactive overlay renderer initialized"))
