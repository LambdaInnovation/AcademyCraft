(ns cn.li.fabric262.gui.provider-bridge
  "Fabric 26.2 provider bridge.

  Uses reify factories and delegates menu construction to shared provider dispatcher."
  (:require [cn.li.mcbase.gui.provider.dispatcher :as provider-dispatcher]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.mcbase.gui.provider.common :as provider-common]
            [cn.li.mc262.gui.registry.common :as registry-common]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.platform.neutral.gui-runtime :as gui])
  (:import [net.minecraft.server.level ServerPlayer]))

(defn- create-menu-proxy
  ([window-id menu-type clj-container]
   (create-menu-proxy window-id menu-type clj-container nil))
  ([window-id menu-type clj-container opts]
   (menu-proxy/create-menu-proxy-with-defaults
     window-id
     menu-type
     clj-container
     (merge {:call-super-removed? true
             :remove-log-message "Fabric menu closed for player"
             :quick-move-error-prefix "Error in Fabric quickMoveStack:"}
            opts))))

(defn- create-menu-from-provider!
  [gui-id tile-entity sync-id player]
  (provider-dispatcher/create-menu-from-provider!
    {:gui-id gui-id
     :tile-entity tile-entity
     :window-id sync-id
     :player player
     :platform-key (target/current-target-key!)
     :create-container-fn (fn [handler gid p world pos]
                            (gui-handler/get-server-container handler gid p world pos))
     :create-menu-proxy-fn create-menu-proxy
     :log-prefix "[FABRIC-MENU-PROVIDER]"}))

(defn create-menu-provider
  "26.2 removed ExtendedScreenHandlerFactory; the loader adapter is disabled."
  [_gui-id _tile-entity]
  nil)
