(ns cn.li.forge1201.platform.init
  "Forge platform initializer.

  Uses shared mc1201 installer with a Forge-specific adapter implementation."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as framework-platform]
            [cn.li.mc1201.bootstrap.platform-init :as platform-init]
            [cn.li.mc1201.platform.runtime-ops :as runtime-ops]
            [cn.li.mcbase.platform.class-access :as class-access]
            [cn.li.mcbase.platform.item-ops :as item-ops]
            [cn.li.mcbase.platform.player-ops :as player-ops]
            [cn.li.mcbase.platform.world-block-ops :as world-block-ops]
            [cn.li.mcbase.platform.menu-inventory-ops :as menu-inventory-ops]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.platform.adapter.minecraft-ops :as minecraft-ops]))

(defn- resolve-binding! [binding-name]
  (require 'cn.li.forge1201.platform.bindings)
  (or (ns-resolve 'cn.li.forge1201.platform.bindings binding-name)
      (throw (ex-info "Missing Forge platform binding"
                      {:binding binding-name
                       :available (->> (ns-publics 'cn.li.forge1201.platform.bindings)
                                       keys
                                       sort
                                       vec)}))))

(def ^:private forge-adapter
  (minecraft-ops/build-adapter-map
   (merge (runtime-ops/standard-runtime-ops)
          {:local-player-class (fn [] nil)
           :scripted-be-class (fn [] nil)
           :world-place-block-by-id
           (fn [level block-id pos flags]
             ((resolve-binding! 'world-place-block-by-id) level block-id pos flags))})))

(defn- be-fns-map []
  {:be-get-level (resolve-binding! 'be-get-level)
   :be-get-world (resolve-binding! 'be-get-world)
   :be-get-custom-state (resolve-binding! 'be-get-custom-state)
   :be-set-custom-state! (resolve-binding! 'be-set-custom-state!)
   :be-get-block-id (resolve-binding! 'be-get-block-id)
   :be-get-tile-id (resolve-binding! 'be-get-tile-id)
   :be-set-changed! (resolve-binding! 'be-set-changed!)
   :be-get-fluid-height (resolve-binding! 'be-get-fluid-height)
   :be-sync-to-client! (resolve-binding! 'be-sync-to-client!)})

(defn- world-fns-map []
  {:world-get-tile-entity (resolve-binding! 'world-get-tile-entity)
   :world-get-block-state (resolve-binding! 'world-get-block-state)
   :world-set-block (resolve-binding! 'world-set-block)
   :world-remove-block (resolve-binding! 'world-remove-block)
   :world-break-block (resolve-binding! 'world-break-block)
   :world-place-block-by-id (resolve-binding! 'world-place-block-by-id)
   :world-is-chunk-loaded? (resolve-binding! 'world-is-chunk-loaded?)
   :world-get-day-time (resolve-binding! 'world-get-day-time)
   :world-get-game-time (resolve-binding! 'world-get-game-time)
   :world-get-dimension-id (resolve-binding! 'world-get-dimension-id)
   :world-server-session-id (resolve-binding! 'world-server-session-id)
   :world-get-players (resolve-binding! 'world-get-players)
   :world-is-raining (resolve-binding! 'world-is-raining)
   :world-is-client-side (resolve-binding! 'world-is-client-side)
   :world-can-see-sky (resolve-binding! 'world-can-see-sky)})

(defn init-platform!
  "Initialize Forge 1.20.1 platform implementations."
  []
  (install/framework-once! ::initialized
    #(let [be-plan (minecraft-ops/resolve-be-install
                    {:mode :via-services :be-fns (be-fns-map)})]
       (class-access/install-class-access! (:class-access forge-adapter) "forge")
       (item-ops/install-item-platform-ops! (:item-ops-platform forge-adapter) "forge")
       (player-ops/install-player-ops-platform! (:player-ops-platform forge-adapter) "forge")
       (menu-inventory-ops/install-menu-inventory-ops! (:menu-inventory-ops forge-adapter) "forge")
       (world-block-ops/install-world-block-ops! (:world-block-ops forge-adapter) "forge")
       (platform-init/install-platform-services!
         forge-adapter
         (world-fns-map)
         (:be-fns-map be-plan))
       (when-let [after! (:after! be-plan)]
         (after!))
       (framework-platform/install-adapter!
         (fw/fw-atom)
         :gui-open
         {:open-player-menu! (resolve-binding! 'open-player-menu!)})
       (framework-platform/install-adapter!
         (fw/fw-atom)
         :player-persistent-data
         {:get! (resolve-binding! 'player-persistent-data)})
       (log/info "forge platform initialized for selected target")))
  nil)
