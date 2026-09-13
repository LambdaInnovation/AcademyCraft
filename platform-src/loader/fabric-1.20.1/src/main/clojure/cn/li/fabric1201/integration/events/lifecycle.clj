(ns cn.li.fabric1201.integration.events.lifecycle
  "Fabric player/server lifecycle handlers extracted from monolithic events namespace."
  (:require [cn.li.mcbase.integration.event-support :as event-support]
            [cn.li.mc1201.runtime.nbt-core :as runtime-nbt]
            [cn.li.mcbase.runtime.sync-core :as runtime-sync]
            [cn.li.platform.bootstrap :as platform-bootstrap]
            [cn.li.fabric1201.adapter.network :as runtime-network]
            [cn.li.mc1201.runtime.lifecycle-core :as lifecycle-core])
  (:import [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel ServerPlayer]))

;; Immutable loader VTable, installed after common neutral providers.  Do not
;; resolve the callback at namespace construction: Fabric loads listener
;; namespaces before common bootstrap has installed those provider values.
(def ^:private tick-callbacks nil)

(defn install-runtime-callbacks!
  "Capture neutral callbacks once during listener registration.

   The resulting map contains concrete IFns; server ticks only consume this
   immutable map and never resolve namespaces or dereference an installation
   atom."
  []
  (when-not tick-callbacks
    (let [world-tick! (platform-bootstrap/world-tick-callback!)]
      (alter-var-root
        #'tick-callbacks
        (constantly
          {:mark-player-dirty! runtime-sync/mark-player-dirty!
           :tick-sync! runtime-sync/tick-sync!
           :send-sync-fn runtime-network/send-sync-to-client!
           :world-tick! (fn [_runtime level]
                          (world-tick! level))}))))
  nil)

(defn- dimension-id
  [^ServerLevel level]
  (some-> level .dimension .location str))

(defn- server-tick-id
  [^MinecraftServer server]
  (some-> server .getTickCount))

(defn- server-session-id
  [^MinecraftServer server]
  (when server
    [:server (System/identityHashCode server)]))

(defn- lifecycle-owner
  [^MinecraftServer server]
  (cond-> {:server-session-id (server-session-id server)}
    (server-tick-id server) (assoc :server-tick-id (server-tick-id server))))

(defn- player-owner
  [^ServerPlayer player]
  (lifecycle-owner (some-> player .getServer)))

(defn handle-player-login
  [^ServerPlayer player]
  (event-support/guarded-call
    "fabric player login"
    nil
    (fn []
      (lifecycle-core/on-player-login! player (merge (player-owner player)
                                                     {:load-player-state! runtime-nbt/load-player-state!
                                                      :mark-player-dirty! runtime-sync/mark-player-dirty!
                                                      :send-sync-now! runtime-network/send-sync-to-client!
                                                      :clear-player-dirty! runtime-sync/clear-player-dirty!})))))

(defn handle-player-logout
  [^ServerPlayer player]
  (event-support/guarded-call
    "fabric player logout"
    nil
    (fn []
      (lifecycle-core/on-player-logout! player (merge (player-owner player)
                                                      {:save-player-state! runtime-nbt/save-player-state!})))))

(defn handle-player-clone
  [^ServerPlayer old-player ^ServerPlayer new-player alive]
  (event-support/guarded-call
    "fabric player clone"
    nil
    (fn []
      (lifecycle-core/on-player-clone! old-player new-player alive
                                       (merge (player-owner new-player)
                                              {:clone-player-state! runtime-nbt/clone-player-state!
                                               :mark-player-dirty! runtime-sync/mark-player-dirty!
                                               :send-sync-now! runtime-network/send-sync-to-client!
                                               :clear-player-dirty! runtime-sync/clear-player-dirty!})))))

(defn handle-player-death
  [entity]
  (event-support/guarded-call
    "fabric player death"
    nil
    (fn []
      (when (instance? ServerPlayer entity)
        (let [^ServerPlayer p entity]
          (lifecycle-core/on-player-death! p (merge (player-owner p)
                                                    {:save-player-state! runtime-nbt/save-player-state!})))))))

(defn handle-player-dimension-change
  [^ServerPlayer player ^ServerLevel origin ^ServerLevel destination]
  (event-support/guarded-call
    "fabric player dimension change"
    nil
    (fn []
      (lifecycle-core/on-player-dimension-change! player
                                                  (dimension-id origin)
                                                  (dimension-id destination)
                     (merge (player-owner player)
                       {:mark-player-dirty! runtime-sync/mark-player-dirty!
                        :tick-sync! runtime-sync/tick-sync!
                        :send-sync-fn runtime-network/send-sync-to-client!
                        :send-sync-now! runtime-network/send-sync-to-client!
                        :clear-player-dirty! runtime-sync/clear-player-dirty!})))))

(defn handle-player-tick
  [^MinecraftServer server]
  (event-support/guarded-call
    "fabric server tick"
    nil
    (fn []
      (lifecycle-core/run-server-tick! server tick-callbacks))))

(defn install-server-stop-cleanup!
  []
  (lifecycle-core/install-server-stop-cleanup!
    {:cleanup-session! (fn [session-id]
                         (runtime-sync/clear-session-scheduler-state! session-id))}))
