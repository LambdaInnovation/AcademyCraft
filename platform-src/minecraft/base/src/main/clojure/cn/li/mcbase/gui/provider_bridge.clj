(ns cn.li.mcbase.gui.provider-bridge
  "Loader-agnostic MenuProvider bridge.

  Uses reify MenuProvider and delegates menu creation to shared provider dispatcher.
  Fabric ExtendedScreenHandlerFactory stays in the Fabric adapter."
  (:require [cn.li.mcbase.gui.provider.dispatcher :as provider-dispatcher]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.world MenuProvider]
           [net.minecraft.network.chat Component]))

(defn- create-menu-proxy
  ([window-id menu-type clj-container]
   (create-menu-proxy window-id menu-type clj-container nil))
  ([window-id menu-type clj-container opts]
   (menu-proxy/create-menu-proxy-with-defaults
     window-id
     menu-type
     clj-container
     opts)))

(defn create-menu-provider
  "Create a MenuProvider for opening GUI.

  Args:
  - gui-id: int (GUI identifier)
  - tile-entity: TileEntity instance

  Returns: MenuProvider instance"
  [gui-id tile-entity]
  (reify MenuProvider
    (getDisplayName [_]
      (Component/empty))

    (createMenu [_ window-id _player-inventory player]
      (try
        (provider-dispatcher/create-menu-from-provider!
         {:gui-id gui-id
          :tile-entity tile-entity
          :window-id window-id
          :player player
          :platform-key (target/current-target-key!)
          :create-container-fn (fn [handler gid p world pos]
                                 (gui-handler/get-server-container handler gid p world pos))
          :create-menu-proxy-fn create-menu-proxy
          :log-prefix "[MENU-PROVIDER]"})
        (catch Exception e
          (log/stacktrace "[MENU-PROVIDER] Error creating menu" e)
          (throw e))))))
