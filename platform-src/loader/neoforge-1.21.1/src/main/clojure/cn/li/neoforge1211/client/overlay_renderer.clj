(ns cn.li.neoforge1211.client.overlay-renderer
  "CLIENT-ONLY Forge overlay event adapter — renders via reactive overlay-host.
   Build/update fns come from the client bridge (installed by ac via merge-client-bridge!,
   keys :reactive-overlay-build / :reactive-overlay-update). Zero static ac dependency."
  (:require [cn.li.mc1211.gui.reactive.overlay-host :as overlay-host]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.client-runtime :as client-bridge]
            [cn.li.mcmod.util.log :as log])
  (:import [net.neoforged.neoforge.client.event RenderGuiEvent$Post]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
           [net.minecraft.client Minecraft]
           [cn.li.neoforge1211.bridge ClientTimeInterop]))

(defn- bridge-build-fn [w h]
  (client-bridge/reactive-overlay-build w h))

(defn- bridge-update-fn [rt]
  (client-bridge/reactive-overlay-update rt))

(defn- on-render-gui-overlay [^RenderGuiEvent$Post event]
  (let [^Minecraft mc (Minecraft/getInstance)
        w (.getGuiScaledWidth (.getWindow mc))
        h (.getGuiScaledHeight (.getWindow mc))
        pt (ClientTimeInterop/getFrameTime mc)]
    ;; Overlay render is a client dispatch boundary (hooks.core 调用规范 #2):
    ;; bind the CURRENT connection session so reactive HUD state reads resolve
    ;; the live store partition — without this the render thread sees whatever
    ;; ctx last leaked onto it and the HUD reads an empty/stale partition.
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
  ;; RenderGuiEvent$Post fires ONCE per frame after the whole vanilla GUI.
  ;; RenderGuiOverlayEvent$Post (the previous hook) fires once PER vanilla
  ;; overlay element (~10+/frame) — the HUD got drawn that many times per
  ;; frame, compositing the 35%-alpha background mask to near-opaque.
  (.addListener (NeoForge/EVENT_BUS)
                EventPriority/NORMAL false RenderGuiEvent$Post
                (reify java.util.function.Consumer
                  (accept [_ evt] (on-render-gui-overlay evt))))
  (log/debug "Reactive overlay renderer initialized"))
