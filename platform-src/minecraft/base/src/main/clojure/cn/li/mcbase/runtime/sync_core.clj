(ns cn.li.mcbase.runtime.sync-core
  "Allocation-bounded dirty-player sync scheduler.

  The server tick coordinator calls tick-sync! once per server tick. Mutable
  HashMaps and primitive counters are confined to that thread; no per-call
  atom, persistent scheduler map, result map, or periodic full snapshot is
  created."
  (:require [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.runtime.deferred :as deferred])
  (:import [java.util ArrayList HashMap Iterator Map$Entry]))

(def default-flush-interval-ticks 1)

(definterface ISyncSession
  (^java.util.HashMap dirtyPlayers [])
  (^long tickCounter [])
  (^long lastServerTick [])
  (^void advance [^long server-tick]))

(deftype SyncSession
  [^HashMap dirty-players
   ^:unsynchronized-mutable ^long tick-counter
   ^:unsynchronized-mutable ^long last-server-tick]
  ISyncSession
  (dirtyPlayers [_] dirty-players)
  (tickCounter [_] tick-counter)
  (lastServerTick [_] last-server-tick)
  (advance [_ server-tick]
    (set! tick-counter (unchecked-inc tick-counter))
    (set! last-server-tick server-tick)))

(definterface ISyncScheduler
  (^java.util.HashMap sessions [])
  (^long flushInterval []))

(deftype SyncScheduler [^HashMap session-states ^long flush-interval]
  ISyncScheduler
  (sessions [_] session-states)
  (flushInterval [_] flush-interval))

(defn create-sync-scheduler-runtime
  ([]
   (create-sync-scheduler-runtime {}))
  ([{:keys [flush-interval-ticks]
     :or {flush-interval-ticks default-flush-interval-ticks}}]
   (let [interval (long flush-interval-ticks)]
     (when-not (pos? interval)
       (throw (IllegalArgumentException. "flush-interval-ticks must be positive")))
     (SyncScheduler. (HashMap.) interval))))

(def ^:private default-sync-scheduler-runtime-holder
  (deferred/deferred create-sync-scheduler-runtime))

(def ^:private sync-scheduler-runtime-override nil)

(defn- sync-scheduler-runtime?
  [runtime]
  (instance? ISyncScheduler runtime))

(defn call-with-sync-scheduler-runtime
  [runtime f]
  (when-not (sync-scheduler-runtime? runtime)
    (throw (ex-info "Expected sync scheduler runtime" {:runtime runtime})))
  (let [previous sync-scheduler-runtime-override]
    (alter-var-root #'sync-scheduler-runtime-override (constantly runtime))
    (try
      (f)
      (finally
        (alter-var-root #'sync-scheduler-runtime-override (constantly previous))))))

(defmacro with-sync-scheduler-runtime
  [runtime & body]
  `(call-with-sync-scheduler-runtime ~runtime (fn [] ~@body)))

(defn- current-runtime
  ^ISyncScheduler []
  (or sync-scheduler-runtime-override
      @default-sync-scheduler-runtime-holder))

(defn- require-session-id
  [owner]
  (or (:server-session-id owner)
      (throw (ex-info "Sync scheduler owner requires :server-session-id"
                      {:owner owner}))))

(defn- ensure-session
  ^ISyncSession [^ISyncScheduler runtime session-id]
  (let [^HashMap sessions (.sessions runtime)]
    (or (.get sessions session-id)
        (let [session (SyncSession. (HashMap.) 0 Long/MIN_VALUE)]
          (.put sessions session-id session)
          session))))

(defn mark-player-dirty!
  [owner uuid]
  (let [session-id (require-session-id owner)
        ^ISyncSession session (ensure-session (current-runtime) session-id)]
    (.put (.dirtyPlayers session) uuid (.tickCounter session)))
  nil)

(defn clear-player-dirty!
  [owner uuid]
  (let [session-id (require-session-id owner)]
    (when-let [^ISyncSession session (.get (.sessions (current-runtime)) session-id)]
      (.remove (.dirtyPlayers session) uuid)))
  nil)

(defn mark-all-dirty!
  [owner]
  (doseq [uuid (power-runtime/list-player-uuids)]
    (mark-player-dirty! owner uuid))
  nil)

(defn clear-session-scheduler-state!
  [session-id]
  (.remove (.sessions (current-runtime)) session-id)
  nil)

(defn- remove-flushed-entry!
  [^Iterator iterator ^Map$Entry entry ^long flush-tick]
  (when (<= (long (.getValue entry)) flush-tick)
    (.remove iterator)))

(defn- flush-dirty!
  [^ISyncSession session send-fn]
  (let [flush-tick (.tickCounter session)
        ^Iterator iterator (.iterator (.entrySet (.dirtyPlayers session)))]
    (while (.hasNext iterator)
      (let [^Map$Entry entry (.next iterator)
            uuid (.getKey entry)]
        (when-let [payload (power-runtime/build-sync-payload uuid false)]
          (try
            (when send-fn
              (send-fn uuid payload))
            (power-runtime/mark-player-clean! uuid)
            (remove-flushed-entry! iterator entry flush-tick)
            (catch Throwable _
              nil)))))))

(defn tick-sync!
  "Advance and flush one session. Must be called once at server-tick end."
  [send-fn owner]
  (let [session-id (require-session-id owner)
        server-tick (long (or (:server-tick-id owner) Long/MIN_VALUE))
        ^ISyncScheduler runtime (current-runtime)
        ^ISyncSession session (ensure-session runtime session-id)]
    (when-not (= server-tick (.lastServerTick session))
      (.advance session server-tick)
      (when (and (not (.isEmpty (.dirtyPlayers session)))
                 (zero? (mod (.tickCounter session) (.flushInterval runtime))))
        (flush-dirty! session send-fn))))
  nil)

(defn scheduler-snapshot
  []
  (let [^ISyncScheduler runtime (current-runtime)
        sessions (transient {})]
    (doseq [^Map$Entry session-entry (.entrySet (.sessions runtime))]
      (let [session-id (.getKey session-entry)
            ^ISyncSession session (.getValue session-entry)
            dirty (transient {})]
        (doseq [^Map$Entry dirty-entry (.entrySet (.dirtyPlayers session))]
          (assoc! dirty (.getKey dirty-entry)
                  {:last-dirty-tick (.getValue dirty-entry)}))
        (assoc! sessions session-id
                {:tick-counter (.tickCounter session)
                 :last-server-tick-id (.lastServerTick session)
                 :dirty-players (persistent! dirty)})))
    (persistent! sessions)))

(defn reset-scheduler-for-test!
  ([]
   (reset-scheduler-for-test! {}))
  ([states]
   (let [^ISyncScheduler runtime (current-runtime)
         ^HashMap sessions (.sessions runtime)]
     (.clear sessions)
     (doseq [[session-id state] states]
       (let [session (SyncSession. (HashMap.)
                                   (long (or (:tick-counter state) 0))
                                   (long (or (:last-server-tick-id state) Long/MIN_VALUE)))]
         (doseq [[uuid entry] (:dirty-players state)]
           (.put (.dirtyPlayers ^ISyncSession session)
                 uuid
                 (long (or (:last-dirty-tick entry) 0))))
         (.put sessions session-id session))))
   nil))
