(ns cn.li.forge1201.adapter.gui-registry
  "Forge 1.20.1 GUI Registration Implementation

  Platform-agnostic design: Uses metadata-driven approach."
  (:require [cn.li.platform.neutral.gui-runtime :as gui]
            [cn.li.platform.neutral.gui-runtime :as gui-handler]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.mcbase.gui.provider-bridge :as provider-bridge]
            [cn.li.mcbase.runtime.spi.gui-registry :as registry-api]
            [cn.li.mc1201.gui.registry.common :as registry-common]
            [cn.li.mcbase.gui.registry.open :as open-core]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.runtime.deferred :as deferred]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.forge1201.shim ForgeBootstrapHelper ForgeContainerFactory]
           [net.minecraftforge.network NetworkHooks]
           [net.minecraftforge.common.extensions IForgeMenuType]
           [net.minecraftforge.registries DeferredRegister RegistryObject]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.world MenuProvider]
           [net.minecraft.core BlockPos]))


;; ============================================================================
;; MenuType Registry
;; ============================================================================

;; Registries are locked before FMLCommonSetupEvent; MenuType must be registered
;; via DeferredRegister during RegisterEvent, just like blocks and items.
;; DeferredRegister creation is inherently process-scoped (Forge registries
;; cannot reopen once locked) — deferred/deferred's own lazy-once semantics
;; are the correct guard here, not framework-once!.
(def ^:private menu-register-holder
  (deferred/deferred #(ForgeBootstrapHelper/createMenusRegister modid/mod-id)))

(def ^:private gui-menu-types
  "Map from GUI ID to RegistryObject<MenuType>.
  Call (.get ro) to get the actual MenuType after registration fires."
  (atom {}))

(def ^:private client-owner-wrapper
  "Client-only owner wrapper installed by cn.li.forge1201.client.init.
  This common namespace must not require client session namespaces at load time."
  (atom (fn [_]
          (throw (ex-info "Forge client owner wrapper is not installed"
                          {:namespace 'cn.li.forge1201.adapter.gui-registry})))))

(defn install-client-owner-wrapper!
  [wrapper-fn]
  (reset! client-owner-wrapper wrapper-fn)
  nil)

(defn menu-register
  []
  @menu-register-holder)

(defn- gui-menu-types-snapshot
  []
  @gui-menu-types)

(defn- assoc-gui-menu-type!
  [gui-id menu-type]
  (swap! gui-menu-types assoc gui-id menu-type)
  nil)

(defn- clear-gui-menu-types!
  []
  (reset! gui-menu-types {})
  nil)

(declare install-registry-contract!)

(defn get-menu-type
  "Get registered MenuType for a GUI ID.
  Resolves the RegistryObject; safe to call after RegisterEvent fires.
  
  Args:
  - gui-id: int
  
  Returns: MenuType or nil"
  [gui-id]
  (when-let [registered (get (gui-menu-types-snapshot) gui-id)]
    (if (instance? RegistryObject registered)
      (.get ^RegistryObject registered)
      registered)))

(defn- create-client-menu-from-packet!
  "Client-side menu factory invoked by Forge when recreating menu from open-screen packet."
  [gui-id window-id player-inventory buf]
  (log/debug "[CLIENT-MENU-FACTORY] IContainerFactory.create called! window-id=" window-id)
  (try
    (let [handler (gui-handler/get-gui-handler)
          {:keys [gui-id buf-gui-id pos]} (registry-common/read-extended-open-payload buf)
          resolved-gui-id (or buf-gui-id gui-id)]
      (log/debug "[CLIENT-MENU-FACTORY] gui-id from buf=" gui-id "resolved=" resolved-gui-id "pos=" pos)
      (let [result (registry-common/create-client-menu!
                     {:gui-id resolved-gui-id
                      :window-id window-id
                      :player-inventory player-inventory
                      :pos pos
                      :handler handler
                      :create-container-fn (fn [h gid p world block-pos]
                                             (log/debug "[CLIENT-MENU-FACTORY] Creating clj-container for gui-id=" gid)
                                             (gui-handler/get-server-container h gid p world block-pos))
                      :create-menu-proxy-fn (fn [wid menu-type clj-container opts]
                                              (log/debug "[CLIENT-MENU-FACTORY] Creating menu proxy wid=" wid "menu-type=" menu-type)
                                              (menu-proxy/create-menu-proxy wid menu-type clj-container opts))
                      :resolve-menu-type-fn get-menu-type
                      :bridge-opts (menu-proxy/menu-proxy-opts
                                                      {:call-super-removed? true})
                      :error-prefix "Failed to create container for GUI"
                      :with-owner! #(@client-owner-wrapper %)})]
        (log/debug "[CLIENT-MENU-FACTORY] Menu created successfully, returning to Forge. menu=" (type result))
        result))
    (catch Throwable e
      (log/stacktrace "[CLIENT-MENU-FACTORY] Failed to create client menu:" e)
      (log/stacktrace "[CLIENT-MENU-FACTORY] Stack trace:" e)
      (throw e))))

(defn create-menu-type
  "Create a MenuType for a GUI
  
  Args:
  - gui-id: int
  
  Returns: MenuType instance"
  [gui-id]
  (IForgeMenuType/create
    (ForgeContainerFactory.
      (fn [window-id player-inventory buf]
        (create-client-menu-from-packet! gui-id window-id player-inventory buf)))))

(defn register-menu-types!
  "Populate menu-register DeferredRegister with all GUI menu types.
  Must be called before menu-register is registered with the mod event bus
  (i.e. during Forge bootstrap, not during FMLCommonSetupEvent)."
  []
  (log/debug "Queueing GUI menu types into DeferredRegister")
  (install-registry-contract!)
  (doseq [gui-id (gui/get-all-gui-ids)]
    (let [registry-name (gui/get-registry-name gui-id)
          ;; Create IForgeMenuType eagerly; the embedded IContainerFactory is a
          ;; lazy callback that resolves get-menu-type at GUI-open time (runtime).
          menu-type (create-menu-type gui-id)
          ^DeferredRegister deferred-register (menu-register)
          ro (.register deferred-register registry-name
               (reify java.util.function.Supplier
                 (get [_]
                   ;; Called by Forge during RegisterEvent — registries are open here.
                   ;; Store menu-type in platform adapter's metadata system
                   menu-type)))]
      (assoc-gui-menu-type! gui-id ro)
      (log/debug "Queued menu type:" registry-name "for GUI ID" gui-id)))
      (log/info "Queued" (count (gui-menu-types-snapshot)) "menu types"))

;; ============================================================================
;; GUI Opening
;; ============================================================================

(defn open-gui-for-player
  "Open GUI for player using NetworkHooks
  
  Args:
  - player: ServerPlayerEntity
  - gui-id: int
  - tile-entity: TileEntity (optional, can be nil)"
  [^ServerPlayer player gui-id tile-entity]
  (open-core/log-open-start! "[OPEN-GUI-FOR-PLAYER]" player gui-id tile-entity)
  (try
    (log/debug "[OPEN-GUI-FOR-PLAYER] Creating MenuProvider...")
    (let [^MenuProvider provider (provider-bridge/create-menu-provider gui-id tile-entity)
          ^BlockPos pos (open-core/resolve-optional-block-pos tile-entity)]
      (log/debug "[OPEN-GUI-FOR-PLAYER] MenuProvider created, pos=" pos "calling NetworkHooks...")
      (if pos
        (do
          (log/debug "[OPEN-GUI-FOR-PLAYER] Opening screen with position data to client...")
          (NetworkHooks/openScreen
            player
            provider
            (reify java.util.function.Consumer
              (accept [_ buf]
                (registry-common/write-extended-open-payload! buf gui-id pos)))))
        (do
          (log/debug "[OPEN-GUI-FOR-PLAYER] Opening screen without position data...")
          (NetworkHooks/openScreen player provider)))
      (log/debug "[OPEN-GUI-FOR-PLAYER] NetworkHooks called, GUI open request queued"))
    (catch Exception e
      (open-core/log-open-error! "[OPEN-GUI-FOR-PLAYER]" e))))

;; ============================================================================
;; Registry Implementation
;; ============================================================================

(defn- install-registry-contract!
  []
  (registry-api/register-registry-impl!
    (target/current-target-key!)
    {:register-menu-type! (fn [gui-id menu-type]
                            (assoc-gui-menu-type! gui-id menu-type)
                            nil)
     :get-menu-type get-menu-type
     :list-menu-types (fn [] (gui-menu-types-snapshot))
     :invalidate-menu-registry! clear-gui-menu-types!}))

(defn register-gui-handler! []
  (install-registry-contract!)
  (log/debug "Forge 1.20.1 GUI handler ready (menu types registered via DeferredRegister)"))
