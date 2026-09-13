(ns cn.li.fabric262.gui.screen-impl
  "Fabric 26.2 Client-side Screen Implementation

  Platform-agnostic design: Reads GUI metadata and loops through all GUIs
  to register, eliminating hardcoded game concepts.

  Uses shared CGUI runtime host path for :cgui-screen-container payloads."
  (:require [cn.li.mcbase.gui.screen.registry :as screen-registry]
            [cn.li.mc262.gui.reactive.host-container]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client.gui.screens MenuScreens]))

(defn- register-one-screen!
  [gui-id menu-type screen-creator _factory-fn-kw]
  (when menu-type
    (MenuScreens/register
      menu-type
      (reify net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor
        (create [_ menu player-inventory title]
          (screen-creator menu player-inventory title)))))
  (log/debug "Registered screen factory for GUI ID" gui-id))

(defn register-screens! []
  (log/debug "Registering GUI screens for Fabric 26.2")
  (try
    (screen-registry/register-platform-screens!
      (target/current-target-key!)
      {:label "Fabric 26.2"
       :register-menu-screen! register-one-screen!})
    (log/info "Screen factories registered successfully (Fabric)")
    (catch Exception e
      (log/stacktrace "Failed to register screen factories:" e)
      (.printStackTrace e))))

(defn init-client! []
  (log/debug "Initializing Fabric 26.2 client GUI system")
  (register-screens!)
  (log/debug "Fabric 26.2 client GUI system initialized"))
