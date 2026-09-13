(ns cn.li.neoforge262.gui.screen-impl
  "NeoForge 26.2 Client-side Screen Implementation.

  MenuScreens/register is package-private; registration runs on
  RegisterMenuScreensEvent (see ModClientRenderSetup / lifecycle listeners).

  ScreenEvent$BackgroundRendered was removed in 26.2 — on-render-tail is a no-op.
  create-screen uses DelegatingCGuiContainerScreen and the live reactive host."
  (:require [cn.li.mcbase.gui.screen.registry :as screen-registry]
            [cn.li.mc262.gui.reactive.host-container]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client.gui.screens MenuScreens$ScreenConstructor]
           [net.neoforged.neoforge.client.event RegisterMenuScreensEvent]))

(defn- register-one-screen!
  [^RegisterMenuScreensEvent event gui-id menu-type screen-creator factory-fn-kw]
  (log/debug "[SCREEN-INIT] Registering GUI ID:" gui-id "menu-type:" menu-type "factory-fn-kw:" factory-fn-kw)
  (when menu-type
    (.register event
               menu-type
               (reify MenuScreens$ScreenConstructor
                 (create [_ menu player-inventory title]
                   (log/debug "[SCREEN-FACTORY] Creating screen for GUI ID" gui-id "factory-fn-kw:" factory-fn-kw)
                   (screen-creator menu player-inventory title)))))
  (log/debug "Registered screen for GUI ID" gui-id))

(defn register-screens-on-event!
  "Register screen factories from RegisterMenuScreensEvent (mod bus / client)."
  [^RegisterMenuScreensEvent event]
  (log/debug "Registering GUI screens for NeoForge 26.2 via RegisterMenuScreensEvent")
  (try
    (screen-registry/register-platform-screens!
     (target/current-target-key!)
     {:label "NeoForge 26.2"
      :screen-opts-fn (fn [_gui-id _menu-type _factory-fn-kw]
                        {:on-render-tail! (fn [_screen _gg _mx _my _pt] nil)})
      :register-menu-screen! (fn [gui-id menu-type screen-creator factory-fn-kw]
                               (register-one-screen! event gui-id menu-type screen-creator factory-fn-kw))})
    (log/info "Screen factories registered successfully")
    (catch Exception e
      (log/stacktrace "Failed to register screen factories:" e)
      (.printStackTrace e))))

(defn init-client!
  "Client GUI init hook (FMLClientSetup). Menu screens register on
  RegisterMenuScreensEvent."
  []
  (log/debug "NeoForge 26.2 client GUI system ready (screens via RegisterMenuScreensEvent)"))
