(ns cn.li.fabric1211.client.overlay-renderer
  "CLIENT-ONLY Fabric overlay event adapter — renders via reactive overlay-host.
   Build/update fns come from the client bridge (installed by ac via merge-client-bridge!,
   keys :reactive-overlay-build / :reactive-overlay-update). Zero static ac dependency."
  (:require [cn.li.mc1211.gui.reactive.overlay-host :as overlay-host]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.client-runtime :as client-bridge]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.fabric.api.client.rendering.v1 HudRenderCallback]
           [net.minecraft.client Minecraft]))

(defn- bridge-build-fn [w h]
  (client-bridge/reactive-overlay-build w h))

(defn- bridge-update-fn [rt]
  (client-bridge/reactive-overlay-update rt))

(defn on-mode-switch-key-state!
  ([is-down]
   (client-bridge/reactive-overlay-mode-switch! is-down))
  ([_owner is-down]
   (client-bridge/reactive-overlay-mode-switch! is-down)))

(defn init!
  []
  (install/process-once! ::hud-listener-registered
    #(.register HudRenderCallback/EVENT
                (reify HudRenderCallback
                  (onHudRender [_ graphics tick-delta]
                    (let [^Minecraft mc (Minecraft/getInstance)
                          w (.getGuiScaledWidth (.getWindow mc))
                          h (.getGuiScaledHeight (.getWindow mc))]
                      ;; Overlay render is a client dispatch boundary (hooks.core
                      ;; 调用规范 #2): bind the CURRENT connection session so
                      ;; reactive HUD state reads resolve the live store partition.
                      (client-session/with-current-client-session
                        (fn []
                          (overlay-host/update-overlay!
                            graphics "default" w h (float tick-delta)
                            bridge-build-fn bridge-update-fn))))))))
  (log/debug "Reactive overlay renderer initialized (Fabric)"))
