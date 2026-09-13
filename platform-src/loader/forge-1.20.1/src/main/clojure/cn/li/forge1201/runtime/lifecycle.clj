(ns cn.li.forge1201.runtime.lifecycle
  "Forge player lifecycle hooks for runtime system."
  (:require [cn.li.mc1201.runtime.nbt-core :as runtime-nbt]
            [cn.li.mc1201.runtime.lifecycle-core :as lifecycle-core]
            [cn.li.mcbase.runtime.sync-core :as runtime-sync]
            [cn.li.mc1201.runtime.world-effects-core :as world-effects]
            [cn.li.mc1201.runtime.network-core :as network-core]
            [cn.li.mcbase.runtime.spi.network-transport :as transport-spi]
            [cn.li.forge1201.runtime.adapters.registry :as runtime-adapters-registry]
            [cn.li.mcbase.runtime.adapter-registry :as adapter-registry]
            [cn.li.forge1201.runtime.lifecycle-event-binding :as lifecycle-event-binding]
            [cn.li.forge1201.adapter.network :as runtime-network]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.platform.bootstrap :as platform-bootstrap]
            [cn.li.mcmod.server.platform-bridge :as server-bridge]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraftforge.event.entity.player PlayerEvent$PlayerLoggedInEvent
                                                  PlayerEvent$PlayerLoggedOutEvent
                                   PlayerEvent$Clone
                                   PlayerEvent$PlayerChangedDimensionEvent]
           [net.minecraftforge.event.entity.living LivingDeathEvent]
           [net.minecraftforge.event TickEvent$ServerTickEvent TickEvent$Phase]
           [net.minecraft.resources ResourceKey]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerPlayer]))


(defn- server-player [player]
  (when (instance? ServerPlayer player) player))

(defn- dimension-id [^ResourceKey dimension-key]
  (some-> dimension-key .location str))

(defn- server-tick-id [^ServerPlayer player]
  (some-> player .getServer .getTickCount))

(defn- server-session-id [^ServerPlayer player]
  (when-let [server (some-> player .getServer)]
    [:server (System/identityHashCode server)]))

(defn- lifecycle-owner [^ServerPlayer player]
  (cond-> {:server-session-id (server-session-id player)}
    (server-tick-id player) (assoc :server-tick-id (server-tick-id player))))

(defn- on-player-login [^PlayerEvent$PlayerLoggedInEvent evt]
  (when-let [^ServerPlayer p (server-player (.getEntity evt))]
    (lifecycle-core/on-player-login! p (merge (lifecycle-owner p)
                                              {:load-player-state! runtime-nbt/load-player-state!
                                               :mark-player-dirty! runtime-sync/mark-player-dirty!
                                               :send-sync-now! runtime-network/send-sync-to-client!
                                               :clear-player-dirty! runtime-sync/clear-player-dirty!}))
    (power-runtime/run-server-player-login-hooks! p)))

(defn- on-player-logout [^PlayerEvent$PlayerLoggedOutEvent evt]
  (when-let [^ServerPlayer p (server-player (.getEntity evt))]
    (lifecycle-core/on-player-logout! p (merge (lifecycle-owner p)
                                               {:save-player-state! runtime-nbt/save-player-state!}))))

(defn- on-player-clone [^PlayerEvent$Clone evt]
  (when-let [^ServerPlayer oldp (server-player (.getOriginal evt))]
    (when-let [^ServerPlayer newp (server-player (.getEntity evt))]
      (lifecycle-core/on-player-clone! oldp newp (not (.isWasDeath evt))
                                       (merge (lifecycle-owner newp)
                                              {:clone-player-state! runtime-nbt/clone-player-state!
                                               :mark-player-dirty! runtime-sync/mark-player-dirty!
                                               :send-sync-now! runtime-network/send-sync-to-client!
                                               :clear-player-dirty! runtime-sync/clear-player-dirty!})))))

(defn- on-player-death [^LivingDeathEvent evt]
  (when-let [^ServerPlayer p (server-player (.getEntity evt))]
    (lifecycle-core/on-player-death! p (merge (lifecycle-owner p)
                                             {:save-player-state! runtime-nbt/save-player-state!}))))

(defn- on-player-dimension-change [^PlayerEvent$PlayerChangedDimensionEvent evt]
  (when-let [^ServerPlayer p (server-player (.getEntity evt))]
    (lifecycle-core/on-player-dimension-change! p
                                                (dimension-id (.getFrom evt))
                                                (dimension-id (.getTo evt))
                      (merge (lifecycle-owner p)
                        {:mark-player-dirty! runtime-sync/mark-player-dirty!
                    :tick-sync! runtime-sync/tick-sync!
                    :send-sync-fn runtime-network/send-sync-to-client!
                    :send-sync-now! runtime-network/send-sync-to-client!
                    :clear-player-dirty! runtime-sync/clear-player-dirty!}))))

(defn- on-server-tick
  [callbacks ^TickEvent$ServerTickEvent evt]
  (when (= TickEvent$Phase/END (.phase evt))
    (let [^MinecraftServer server (.getServer evt)]
      (lifecycle-core/run-server-tick! server callbacks))))

(defn init-common!
  "Register all forge-side lifecycle listeners for runtime bridge."
  []
  (adapter-registry/run-install-steps! (:id (target/current-target!)) runtime-adapters-registry/runtime-install-steps)
  (runtime-network/init!)
  (server-bridge/install-server-bridge!
   {:send-to-client! (fn [player-uuid msg-id payload]
                       (when-let [player (transport-spi/find-player-by-uuid player-uuid)]
                         (transport-spi/send-push-to-client! player msg-id payload)))
    :spawn-item-stack-at! world-effects/spawn-item-stack-at!})
  (lifecycle-core/install-server-stop-cleanup!
    {:cleanup-session! (fn [session-id]
                         (runtime-sync/clear-session-scheduler-state! session-id))})
  (let [world-tick-callback (platform-bootstrap/world-tick-callback!)
        tick-callbacks {:mark-player-dirty! runtime-sync/mark-player-dirty!
                        :tick-sync! runtime-sync/tick-sync!
                        :send-sync-fn runtime-network/send-sync-to-client!
                        :world-tick! (fn [_runtime level]
                                       (world-tick-callback level))}]
    (lifecycle-event-binding/register-lifecycle-listeners!
      {:on-player-login on-player-login
       :on-player-logout on-player-logout
       :on-player-clone on-player-clone
       :on-player-death on-player-death
       :on-player-dimension-change on-player-dimension-change
       :on-server-tick (partial on-server-tick tick-callbacks)}))

  ;; Initialize damage handlers after all protocols are installed
  (power-runtime/init-damage-handlers!)

  (log/debug "Forge runtime lifecycle initialized"))
