(ns cn.li.fabric1201.adapter.gui-registry
  "Fabric 1.20.1 GUI Registration Implementation"
  (:require [cn.li.platform.neutral.gui-runtime :as gui]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.fabric1201.gui.provider-bridge :as provider-bridge]
            [cn.li.mcbase.runtime.spi.gui-registry :as registry-api]
            [cn.li.mc1201.gui.registry.common :as registry-common]
            [cn.li.mcbase.gui.registry.open :as open-core]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.resources ResourceLocation]
           [net.fabricmc.fabric.api.screenhandler.v1 ScreenHandlerRegistry]))

(def ^:private gui-handler-types
  "Map from GUI ID to registered MenuType instances. Lock-free CAS updates
   replace the prior ^:dynamic var + Object lock."
  (atom {}))

(def ^:private client-owner-wrapper
  "Client-only owner wrapper installed by cn.li.fabric1201.client.init.
  This adapter may be loaded by common setup and must not require client session
  namespaces at load time."
  (atom (fn [_]
          (throw (ex-info "Fabric client owner wrapper is not installed"
                          {:namespace 'cn.li.fabric1201.adapter.gui-registry})))))

(defn install-client-owner-wrapper!
  [wrapper-fn]
  (reset! client-owner-wrapper wrapper-fn)
  nil)

(defn- gui-handler-types-snapshot
  []
  @gui-handler-types)

(defn- assoc-gui-handler-type!
  [gui-id handler-type]
  (swap! gui-handler-types assoc gui-id handler-type)
  nil)

(defn- clear-gui-handler-types!
  []
  (reset! gui-handler-types {})
  nil)

(defn get-handler-type [gui-id]
  (get (gui-handler-types-snapshot) gui-id))

(defn create-extended-screen-handler-type [gui-id]
  (let [registry-name (gui/get-registry-name gui-id)]
    (ScreenHandlerRegistry/registerExtended
      (ResourceLocation. modid/mod-id registry-name)
      (reify net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry$ExtendedClientHandlerFactory
        (create [_ sync-id player-inventory buf]
          (let [{:keys [gui-id pos]} (registry-common/read-extended-open-payload buf)
                handler (gui-handler/get-gui-handler)]
            (registry-common/create-client-menu!
              {:gui-id gui-id
               :window-id sync-id
               :player-inventory player-inventory
               :pos pos
               :handler handler
               :create-container-fn (fn [h gid player world block-pos]
                                      (gui-handler/get-server-container h gid player world block-pos))
               :create-menu-proxy-fn (fn [window-id menu-type clj-container opts]
                                       (menu-proxy/create-menu-proxy window-id menu-type clj-container opts))
               :resolve-menu-type-fn get-handler-type
               :bridge-opts (menu-proxy/menu-proxy-opts
                             {:call-super-removed? true
                              :remove-log-message "Fabric menu closed for player"
                              :quick-move-error-prefix "Error in Fabric quickMoveStack:"})
               :error-prefix "Failed to create container for GUI"
               :with-owner! #(@client-owner-wrapper %)})))))))

(defn register-screen-handler-types! []
  (log/debug "Registering GUI screen handler types for Fabric 1.20.1")
  (doseq [gui-id (gui/get-all-gui-ids)]
    (let [handler-type (create-extended-screen-handler-type gui-id)
          registry-name (gui/get-registry-name gui-id)]
      (assoc-gui-handler-type! gui-id handler-type)
      (log/debug "Registered screen handler type:" registry-name "for GUI ID" gui-id)))
  (log/info "Registered" (count (gui-handler-types-snapshot)) "screen handler types"))

(defn open-gui-for-player [player gui-id tile-entity]
  (open-core/log-open-start! "[FABRIC-OPEN-GUI]" player gui-id tile-entity)
  (try
    (let [factory (provider-bridge/create-menu-provider gui-id tile-entity)]
      (open-core/open-player-menu! player factory)
      (open-core/log-open-success! "[FABRIC-OPEN-GUI]"))
    (catch Exception e
      (open-core/log-open-error! "[FABRIC-OPEN-GUI]" e))))

(defn- install-registry-contract!
  []
  (registry-api/register-registry-impl!
    (target/current-target-key!)
    {:register-menu-type! (fn [gui-id menu-type]
                            (assoc-gui-handler-type! gui-id menu-type)
                            nil)
     :get-menu-type get-handler-type
     :list-menu-types (fn [] (gui-handler-types-snapshot))
     :invalidate-menu-registry! clear-gui-handler-types!}))

(defn register-gui-handler! []
  (log/debug "Registering GUI handler for Fabric 1.20.1")
  (install-registry-contract!)
  (register-screen-handler-types!)
  (log/debug "Fabric 1.20.1 GUI handler registered"))

(defn init! []
  (log/debug "Initializing Fabric 1.20.1 GUI system")
  (register-gui-handler!)
  (log/debug "Fabric 1.20.1 GUI system initialized"))
