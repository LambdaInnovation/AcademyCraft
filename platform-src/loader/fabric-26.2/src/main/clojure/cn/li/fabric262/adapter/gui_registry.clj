(ns cn.li.fabric262.adapter.gui-registry
  "Fabric 26.2 GUI Registration Implementation"
  (:require [cn.li.platform.neutral.gui-runtime :as gui]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.fabric262.gui.provider-bridge :as provider-bridge]
            [cn.li.mcbase.runtime.spi.gui-registry :as registry-api]
            [cn.li.mc262.gui.registry.common :as registry-common]
            [cn.li.mcbase.gui.registry.open :as open-core]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.resources Identifier]
           [net.minecraft.core Registry]
           [net.minecraft.network.codec ByteBufCodecs]))

(def ^:private gui-handler-types
  "Map from GUI ID to registered MenuType instances. Lock-free CAS updates
   replace the prior ^:dynamic var + Object lock."
  (atom {}))

(def ^:private client-owner-wrapper
  "Client-only owner wrapper installed by cn.li.fabric262.client.init.
  This adapter may be loaded by common setup and must not require client session
  namespaces at load time."
  (atom (fn [_]
          (throw (ex-info "Fabric client owner wrapper is not installed"
                          {:namespace 'cn.li.fabric262.adapter.gui-registry})))))

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
  ;; Fabric 26.2 removed ExtendedScreenHandlerType and its payload factory.
  ;; Keep this seam returning nil until the vanilla StreamCodec menu bridge is
  ;; implemented; callers already tolerate an unavailable menu type.
  nil)

(defn register-screen-handler-types! []
  (log/debug "Fabric 26.2 menu payload API unavailable; GUI screen handler registration deferred")
  nil)

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
  (log/debug "Registering GUI handler for Fabric 26.2")
  (install-registry-contract!)
  (register-screen-handler-types!)
  (log/debug "Fabric 26.2 GUI handler registered"))

(defn init! []
  (log/debug "Initializing Fabric 26.2 GUI system")
  (register-gui-handler!)
  (log/debug "Fabric 26.2 GUI system initialized"))
