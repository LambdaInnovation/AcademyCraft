(ns cn.li.mcbase.gui.provider.dispatcher
  "Shared GUI provider callback dispatcher used by Forge/Fabric wrappers."
  (:require [cn.li.mcbase.gui.provider.common :as provider-common]
            [cn.li.mcbase.runtime.spi.gui-registry :as registry-api]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.world.entity.player Player]))

(defn create-menu-from-provider!
  "Build a menu proxy from provider callback context.

  Required opts keys:
  - :gui-id
  - :tile-entity
  - :window-id
  - :player
  - :platform-key
  - :create-container-fn    (fn [handler gui-id player world pos] -> clj-container)
  - :create-menu-proxy-fn   (fn [window-id menu-type clj-container opts] -> menu)

  Optional keys:
  - :log-prefix             string"
  [{:keys [gui-id
           tile-entity
           window-id
           player
           platform-key
           create-container-fn
           create-menu-proxy-fn
           log-prefix]
    :or {log-prefix "[MENU-PROVIDER]"}}]
  (let [handler (gui-handler/get-gui-handler)
        ^Player player player
        world (.level player)
        pos (provider-common/tile->pos tile-entity player)]
    (log/debug log-prefix "createMenu called: gui-id=" gui-id "window-id=" window-id "player=" (.getGameProfile player))
    (log/debug log-prefix "Creating server-side container...")
    (let [clj-container (create-container-fn handler gui-id player world pos)]
      (when-not clj-container
        (throw (ex-info "Failed to create Clojure container"
                        {:gui-id gui-id :player player :platform platform-key})))
      (let [menu-type (registry-api/get-menu-type platform-key gui-id)]
        (when-not menu-type
          (throw (ex-info "MenuType not registered"
                          {:gui-id gui-id :platform platform-key})))
        (log/debug log-prefix "MenuType found, creating menu proxy...")
        (create-menu-proxy-fn window-id menu-type clj-container {:player player})))))
