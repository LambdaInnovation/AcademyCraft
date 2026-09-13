(ns cn.li.fabric1211.gui.provider-bridge
  "Fabric 1.21.1 provider bridge.

  Uses reify factories and delegates menu construction to shared provider dispatcher."
  (:require [cn.li.mcbase.gui.provider.dispatcher :as provider-dispatcher]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.mcbase.gui.provider.common :as provider-common]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.platform.neutral.gui-runtime :as gui])
  (:import [net.fabricmc.fabric.api.screenhandler.v1 ExtendedScreenHandlerFactory]
           [net.minecraft.network.chat Component]
           [net.minecraft.nbt CompoundTag]
           [net.minecraft.core BlockPos]
           [net.minecraft.server.level ServerPlayer]))

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

(defn- opening-payload
  [gui-id tile-entity player]
  (let [payload (CompoundTag.)
        ^BlockPos pos (when tile-entity (provider-common/tile->pos tile-entity player))]
    (.putInt payload "gui-id" (int gui-id))
    (.putBoolean payload "has-pos" (boolean pos))
    (when pos
      (.putInt payload "x" (.getX pos))
      (.putInt payload "y" (.getY pos))
      (.putInt payload "z" (.getZ pos)))
    payload))

(defn create-menu-provider
  "Create an ExtendedScreenHandlerFactory using the 1.21.1 typed payload API."
  [gui-id tile-entity]
  (reify ExtendedScreenHandlerFactory
    (getDisplayName [_]
      (Component/literal (gui/get-display-name gui-id)))
    (createMenu [_ sync-id _player-inventory player]
      (create-menu-from-provider! gui-id tile-entity sync-id player))
    (getScreenOpeningData [_ ^ServerPlayer player]
      (opening-payload gui-id tile-entity player))))
