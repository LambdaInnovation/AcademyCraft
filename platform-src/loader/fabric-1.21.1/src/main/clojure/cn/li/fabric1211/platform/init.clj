(ns cn.li.fabric1211.platform.init
  "Fabric platform initializer."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as framework-platform]
            [cn.li.mc1211.bootstrap.platform-init :as platform-init]
            [cn.li.mc1211.bootstrap.installer-core :as core]
            [cn.li.mc1211.platform.runtime-ops :as runtime-ops]
            [cn.li.mcbase.platform.class-access :as class-access]
            [cn.li.mcbase.platform.item-ops :as item-ops]
            [cn.li.mcbase.platform.player-ops :as player-ops]
            [cn.li.mcbase.platform.world-block-ops :as world-block-ops]
            [cn.li.mcbase.platform.menu-inventory-ops :as menu-inventory-ops]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.platform.adapter.minecraft-ops :as minecraft-ops])
  (:import [cn.li.fabric1211.block.entity ScriptedBlockEntity]
           [net.minecraft.core BlockPos]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block.state BlockState]))

(defn- resolve-binding! [binding-name]
  (require 'cn.li.fabric1211.platform.bindings)
  (or (ns-resolve 'cn.li.fabric1211.platform.bindings binding-name)
      (throw (ex-info "Missing Fabric platform binding"
                      {:binding binding-name}))))

(defn- install-be-ops! []
  (core/install-be-fns!
    {:be-get-level (fn [^ScriptedBlockEntity be] (.getLevel be))
     :be-get-world (fn [^ScriptedBlockEntity be] (.getLevel be))
     :be-get-custom-state (fn [^ScriptedBlockEntity be] (.getCustomState be))
     :be-set-custom-state! (fn [^ScriptedBlockEntity be state] (.setCustomState be state))
     :be-get-block-id (fn [^ScriptedBlockEntity be] (.getBlockId be))
     :be-set-changed! (fn [^ScriptedBlockEntity be] (.setChanged be))
     :be-sync-to-client! (fn [^ScriptedBlockEntity be] (.syncCustomStateToClient be))
     :be-get-fluid-height (fn [^ScriptedBlockEntity be]
                            (try
                              (when-let [^Level level (.getLevel be)]
                                (let [^BlockPos pos (.getBlockPos be)
                                      ^BlockState state (.getBlockState level pos)
                                      fluid-state (.getFluidState state)]
                                  (if (.isEmpty fluid-state)
                                    0.0
                                    (double (.getOwnHeight fluid-state)))))
                              (catch Exception _ 0.0)))}))

(def ^:private fabric-adapter
  (minecraft-ops/build-adapter-map
   (merge (runtime-ops/standard-runtime-ops)
          {:local-player-class (fn [] nil)
           :scripted-be-class (fn [] ScriptedBlockEntity)
           :world-place-block-by-id
           (fn [level block-id pos flags]
             ((resolve-binding! 'world-place-block-by-id) level block-id pos flags))})))

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
  "Initialize Fabric 1.21.1 platform implementations."
  []
  (install/framework-once! ::initialized
    #(let [be-plan (minecraft-ops/resolve-be-install
                    {:mode :deferred :install! install-be-ops!})]
       (class-access/install-class-access! (:class-access fabric-adapter) "fabric")
       (item-ops/install-item-platform-ops! (:item-ops-platform fabric-adapter) "fabric")
       (player-ops/install-player-ops-platform! (:player-ops-platform fabric-adapter) "fabric")
       (menu-inventory-ops/install-menu-inventory-ops! (:menu-inventory-ops fabric-adapter) "fabric")
       (world-block-ops/install-world-block-ops! (:world-block-ops fabric-adapter) "fabric")
       (platform-init/install-platform-services!
         fabric-adapter
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
       (log/debug "fabric platform initialized for selected target")))
  nil)
