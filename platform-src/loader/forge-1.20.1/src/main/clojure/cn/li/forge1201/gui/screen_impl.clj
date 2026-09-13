(ns cn.li.forge1201.gui.screen-impl
  "Forge 1.20.1 Client-side Screen Implementation.

  Uses MenuScreens/register directly (matching Fabric approach) to avoid
  the extra ForgeClientHelper$ScreenFactory wrapper."
  (:require [cn.li.mcbase.gui.screen.registry :as screen-registry]
            [cn.li.mc1201.gui.reactive.host-container]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client.gui.screens Screen MenuScreens MenuScreens$ScreenConstructor]
           [net.minecraftforge.client.event ScreenEvent$BackgroundRendered]
           [net.minecraftforge.common MinecraftForge]))

(defn- register-one-screen!
  [gui-id menu-type screen-creator factory-fn-kw]
  (log/debug "[SCREEN-INIT] Registering GUI ID:" gui-id "menu-type:" menu-type "factory-fn-kw:" factory-fn-kw)
  (when menu-type
    (MenuScreens/register
     menu-type
     (reify MenuScreens$ScreenConstructor
       (create [_ menu player-inventory title]
         (log/debug "[SCREEN-FACTORY] Creating screen for GUI ID" gui-id "factory-fn-kw:" factory-fn-kw)
         (try
           (let [screen (screen-creator menu player-inventory title)]
             screen)
           (catch Throwable e
             (log/stacktrace "[SCREEN-FACTORY] Exception:" e)
             (throw e))))))
  (log/debug "Registered screen for GUI ID" gui-id)))

(defn register-screens!
  "Register screen factories with Forge."
  []
  (log/debug "Registering GUI screens for Forge 1.20.1")
  (try
    (screen-registry/register-platform-screens!
     (target/current-target-key!)
     {:label "Forge 1.20.1"
      :screen-opts-fn (fn [_gui-id _menu-type _factory-fn-kw]
                        {:on-render-tail! (fn [^Screen screen gg _mx _my _pt]
                                            (.post MinecraftForge/EVENT_BUS (ScreenEvent$BackgroundRendered. screen gg)))})
      :register-menu-screen! register-one-screen!})
    (log/info "Screen factories registered successfully")
    (catch Exception e
      (log/stacktrace "Failed to register screen factories:" e)
      (.printStackTrace e))))

(defn init-client!
  "Initialize client-side GUI system. Call during FMLClientSetupEvent."
  []
  (log/debug "Initializing Forge 1.20.1 client GUI system")
  (register-screens!)
  (log/debug "Forge 1.20.1 client GUI system initialized"))
