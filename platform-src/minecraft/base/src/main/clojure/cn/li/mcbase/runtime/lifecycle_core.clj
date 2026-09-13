(ns cn.li.mcbase.runtime.lifecycle-core
  "Loader-agnostic player lifecycle flow for runtime state.

  Platform layers provide concrete persistence/sync callbacks and event binding."
  (:require [cn.li.mcbase.runtime.server-runtime :as server-runtime]
            [cn.li.mcbase.runtime.spi.server-context :as server-context-spi]
            [cn.li.platform.neutral.hooks :as player-hooks]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.server.players PlayerList]
           [net.minecraft.world.entity.player Player]))

(defn- player-uuid
  [^Player player]
  (some-> player .getUUID str))

(defn- send-sync-now-for-player!
  [send-sync-now! uuid]
  (let [payload (player-hooks/build-sync-payload uuid)]
    (log/debug "[SYNC-TRACE][SERVER] full sync send"
              {:uuid uuid
               :payload? (some? payload)
               :revision (:revision payload)
               :dirty-mask (:dirty-mask payload)
               :category-id (get-in payload [:ability-data :category-id])})
    (send-sync-now! uuid payload)))

(defn- scheduler-owner
  [opts]
  (let [session-id (or (:server-session-id opts)
                       (:session-id opts))]
    (when-not session-id
      (throw (ex-info "Lifecycle sync owner requires :server-session-id"
                      {:opts (select-keys opts [:server-session-id :session-id :server-tick-id])})))
    (cond-> {:server-session-id session-id}
      (:server-tick-id opts) (assoc :server-tick-id (:server-tick-id opts)))))

(defn- server-owner
  [server]
  {:server-session-id [:server (System/identityHashCode server)]})

(defn- with-player-state-owner
  [owner f]
  (player-hooks/with-client-ctx-fn {:player-owner owner} f))

(defn on-player-login!
  [player {:keys [load-player-state! mark-player-dirty! send-sync-now! clear-player-dirty!] :as opts}]
  (when-let [uuid (player-uuid player)]
    (let [owner (scheduler-owner opts)]
      (with-player-state-owner owner
        #(do
           (when load-player-state!
             (load-player-state! player))
           (player-hooks/on-player-login! uuid)
           (when mark-player-dirty!
             (mark-player-dirty! owner uuid))
           (when send-sync-now!
             (send-sync-now-for-player! send-sync-now! uuid)
             (when clear-player-dirty!
               (clear-player-dirty! owner uuid))))))))

(defn on-player-logout!
  [player {:keys [save-player-state!] :as opts}]
  (when-let [uuid (player-uuid player)]
    (with-player-state-owner (scheduler-owner opts)
      #(do
         (when save-player-state!
           (save-player-state! player))
         (player-hooks/on-player-logout! uuid)))))

(defn on-server-stop!
  [server {:keys [cleanup-session!]}]
  (when server
    (let [owner (server-owner server)
          session-id (:server-session-id owner)]
      (with-player-state-owner owner
        #(do
           (player-hooks/on-server-stop! session-id)
           (when cleanup-session!
             (cleanup-session! session-id))))
      (when-let [runtime (server-context-spi/current-server-runtime)]
        (server-runtime/dispose-server-runtime! runtime)
        (server-context-spi/clear-server-runtime!)))))

(defn install-server-stop-cleanup!
  "Register the on-server-stop cleanup callback exactly once per process.
   Each call builds a fresh closure, so the SPI's own by-id callback dedup
   can't catch repeats here -- this guard is load-bearing. Process-scoped
   (not Framework-scoped): the SPI's callback list is its own process-level
   state with its own test-reset (reset-server-context-spi-for-test!)."
  [{:keys [cleanup-session!] :as opts}]
  (server-context-spi/install-server-context!)
  (install/process-once! ::server-stop-cleanup-installed
    #(server-context-spi/on-server-unavailable!
       (fn [server]
         (on-server-stop! server (assoc opts :cleanup-session! cleanup-session!)))))
  nil)

(defn on-player-clone!
  [old-player new-player _alive {:keys [clone-player-state! mark-player-dirty! send-sync-now! clear-player-dirty!] :as opts}]
  (when (and old-player new-player)
    (let [owner (scheduler-owner opts)]
      (with-player-state-owner owner
        #(do
           (when clone-player-state!
             (clone-player-state! old-player new-player))
           (let [old-uuid (player-uuid old-player)
                 new-uuid (player-uuid new-player)]
             (when (and old-uuid new-uuid)
               (player-hooks/on-player-clone! old-uuid new-uuid)
          (when mark-player-dirty!
            (mark-player-dirty! owner new-uuid))
          (when send-sync-now!
            (send-sync-now-for-player! send-sync-now! new-uuid)
            (when clear-player-dirty!
                       (clear-player-dirty! owner new-uuid))))))))))

(defn on-player-death!
  [player {:keys [save-player-state!] :as opts}]
  (when-let [uuid (player-uuid player)]
    (with-player-state-owner (scheduler-owner opts)
      #(do
         (player-hooks/on-player-death! uuid)
         (when save-player-state!
           (save-player-state! player))))))

(defn on-player-dimension-change!
  [player from-dim to-dim {:keys [mark-player-dirty! tick-sync! send-sync-fn send-sync-now! clear-player-dirty!] :as opts}]
  (when-let [uuid (player-uuid player)]
    (let [owner (scheduler-owner opts)]
      (with-player-state-owner owner
        #(do
           (player-hooks/on-player-dimension-change! uuid from-dim to-dim)
           (when mark-player-dirty!
             (mark-player-dirty! owner uuid))
           (if send-sync-now!
             (do
               (send-sync-now-for-player! send-sync-now! uuid)
               (when clear-player-dirty!
                 (clear-player-dirty! owner uuid)))
             (when tick-sync!
               (tick-sync! send-sync-fn owner))))))))

;; ---------------------------------------------------------------------------
;; Server-owned four-phase runtime. Platform adapters call run-server-tick!
;; once, instead of invoking the global scheduler from every PlayerTickEvent.
;; ---------------------------------------------------------------------------

(defn- same-server-runtime?
  [runtime ^MinecraftServer server]
  (and runtime
       (not (.disposed ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime))
       (identical? server (.server ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime))))

(defn create-server-runtime!
  [^MinecraftServer server callbacks]
  (server-runtime/create-server-runtime!
   server callbacks (player-hooks/freeze-runtime-hooks)))

(defn ensure-server-runtime!
  [^MinecraftServer server callbacks]
  (let [current (server-context-spi/current-server-runtime)]
    (if (same-server-runtime? current server)
      current
      (do
        (when current
          (server-runtime/dispose-server-runtime! current))
        (server-context-spi/install-server-runtime!
         (create-server-runtime! server callbacks))))))

(defn- cached-player-uuid
  [runtime ^ServerPlayer player]
  (let [^HashMap cache (.players ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)
        uuid (.getUUID player)]
    (or (.get cache uuid)
        (let [value (str uuid)]
          (.put cache uuid value)
          value))))

(defn player-tick!
  [runtime ^ServerPlayer player owner]
  (let [uuid (cached-player-uuid runtime player)
        hooks (.hooks ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)
        callbacks (.callbacks ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)]
    ((get hooks :on-player-tick!) uuid player)
    (when ((get hooks :player-state-dirty?) uuid)
      (when-let [mark-dirty! (get callbacks :mark-player-dirty!)]
        (mark-dirty! owner uuid))))
  nil)

(defn world-tick!
  [runtime level]
  (when-let [tick-world! (get (.callbacks ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)
                              :world-tick!)]
    (tick-world! runtime level))
  nil)

(defn server-tick-end!
  [runtime ^long game-time owner]
  (let [hooks (.hooks ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)
        callbacks (.callbacks ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)]
    ((get hooks :on-server-tick-end!) game-time)
    (when-let [tick-sync! (get callbacks :tick-sync!)]
      (tick-sync! (get callbacks :send-sync-fn) owner)))
  nil)

(defn run-server-tick!
  "Run start -> N players -> end/sync exactly once for one server tick."
  [^MinecraftServer server callbacks]
  (let [runtime (ensure-server-runtime! server callbacks)
        game-time (long (.getTickCount server))]
    (.beginTick ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime game-time)
    (let [owner (server-runtime/runtime-owner runtime)
          old-context (player-hooks/push-player-state-owner! owner)]
      (try
        ((get (.hooks ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)
              :on-server-tick-start!)
         game-time)
        (let [^PlayerList player-list (.getPlayerList server)]
          (doseq [^ServerPlayer player (.getPlayers player-list)]
            (player-tick! runtime player owner)))
        (doseq [level (.getAllLevels server)]
          (world-tick! runtime level))
        (server-tick-end! runtime game-time owner)
        (finally
          (.finishTick ^cn.li.mcbase.runtime.server_runtime.IServerRuntime runtime)
          (player-hooks/pop-client-context! old-context))))
    runtime))
