(ns cn.li.neoforge1211.gui.screen-impl
  "NeoForge 1.21.1 Client-side Screen Implementation.

  MenuScreens/register is package-private on 1.21.1; registration runs on
  RegisterMenuScreensEvent (see ModClientRenderSetup)."
  (:require [cn.li.mcbase.gui.screen.registry :as screen-registry]
            [cn.li.mc1211.gui.reactive.host-container]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client.gui.screens Screen MenuScreens$ScreenConstructor]
           [net.neoforged.neoforge.client.event RegisterMenuScreensEvent
            ScreenEvent$BackgroundRendered]
           [net.neoforged.neoforge.common NeoForge]))

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
  (log/debug "Registering GUI screens for NeoForge 1.21.1 via RegisterMenuScreensEvent")
  (try
    (screen-registry/register-platform-screens!
     (target/current-target-key!)
     {:label "NeoForge 1.21.1"
      :screen-opts-fn (fn [_gui-id _menu-type _factory-fn-kw]
                        {:on-render-tail! (fn [^Screen screen gg _mx _my _pt]
                                            (.post NeoForge/EVENT_BUS (ScreenEvent$BackgroundRendered. screen gg)))})
      :register-menu-screen! (fn [gui-id menu-type screen-creator factory-fn-kw]
                               (register-one-screen! event gui-id menu-type screen-creator factory-fn-kw))})
    (log/info "Screen factories registered successfully")
    (catch Exception e
      (log/stacktrace "Failed to register screen factories:" e)
      (.printStackTrace e))))

(defn init-client!
  "Client GUI init hook (FMLClientSetup). Menu screens register on
  RegisterMenuScreensEvent via ModClientRenderSetup."
  []
  (log/debug "NeoForge 1.21.1 client GUI system ready (screens via RegisterMenuScreensEvent)"))
