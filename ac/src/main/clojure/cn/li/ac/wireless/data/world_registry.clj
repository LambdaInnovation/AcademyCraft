(ns cn.li.ac.wireless.data.world-registry
  "Thread-confined wireless world runtime.

  Framework owns only lifecycle references. Topology reads and writes go through
  the mutable WorldRuntime stored in WiWorldData and never swap the Framework atom."
  (:require [cn.li.ac.wireless.core.spatial-index :as si]
            [cn.li.mcmod.events.world-state-notify :as world-state-notify]
            [cn.li.mcmod.events.world-owner-key :as world-owner-key]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap HashSet]))

(definterface IWorldRuntime
  (^Object runtimeState [])
  (^void setRuntimeState [state])
  (^java.util.HashMap transientValues [])
  (^boolean dirty [])
  (^void markDirty [])
  (^void clearDirty []))

(deftype WorldRuntime [^:unsynchronized-mutable state
                       ^HashMap transient-values
                       ^:unsynchronized-mutable ^long dirty-flag]
  IWorldRuntime
  (runtimeState [_] state)
  (setRuntimeState [_ next-state] (set! state next-state))
  (transientValues [_] transient-values)
  (dirty [_] (not (zero? dirty-flag)))
  (markDirty [_] (set! dirty-flag 1))
  (clearDirty [_] (set! dirty-flag 0)))

(defrecord WiWorldData [world-key runtime])

(defn initial-world-state
  []
  {:networks {}
   :connections {}
   :net-by-ssid {}
   :node-to-net {}
   :device-to-node {}
   :spatial-index (si/create-spatial-index-value)})

(def ^:private worlds-path [:service :wireless-worlds])

(defn- registry-path [world-key]
  (conj worlds-path world-key))

(defn world-key
  [world]
  (world-owner-key/world-key world))

(defn- new-world-data
  [world-key]
  (->WiWorldData world-key
                 (WorldRuntime. (initial-world-state) (HashMap.) 0)))

(defn- get-world-data-record
  [world-key]
  (when-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom (conj (registry-path world-key) :world-data))))

(defn- ensure-world-entry!
  [world-key]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in (registry-path world-key)
           (fn [entry]
             (or entry {:world-data (new-world-data world-key)}))))
  nil)

(defn set-on-world-state-changed-fn!
  [f]
  (world-state-notify/set-on-world-state-changed-fn! f))

(defn create-world-data
  [world]
  (let [key (world-key world)]
    (ensure-world-entry! key)
    (get-world-data-record key)))

(defn get-world-data
  [world]
  (create-world-data world))

(defn get-world-data-non-create
  [world]
  (get-world-data-record (world-key world)))

(defn register-world-data!
  [world wi-data]
  (let [key (world-key world)
        registered (if (:runtime wi-data)
                     (assoc wi-data :world-key key)
                     (new-world-data key))]
    (when-let [fw-atom (fw/fw-atom)]
      (swap! fw-atom assoc-in (registry-path key) {:world-data registered}))
    registered))

(defn remove-world-data!
  [world]
  (let [key (world-key world)]
    (when-let [fw-atom (fw/fw-atom)]
      (swap! fw-atom update-in worlds-path dissoc key))
    (log/debug (format "Removed WiWorldData for world: %s" key)))
  nil)

(defn- remove-session-entries
  [entries session-id]
  (if entries
    (into {} (remove (fn [[[entry-session-id _world-id] _entry]]
                       (= session-id entry-session-id))
                     entries))
    {}))

(defn clear-session-world-data!
  [owner-or-session-id]
  (let [session-id (if (map? owner-or-session-id)
                     (first (world-owner-key/world-key owner-or-session-id))
                     owner-or-session-id)]
    (when-let [fw-atom (fw/fw-atom)]
      (swap! fw-atom update-in worlds-path remove-session-entries session-id)))
  nil)

(defn world-state
  [world-data]
  (if-let [^IWorldRuntime runtime (:runtime world-data)]
    (.runtimeState runtime)
    (initial-world-state)))

(defn state-value
  [world-data key]
  (get (world-state world-data) key))

(defn- update-runtime-state!
  [world-data f args]
  (let [^IWorldRuntime runtime (:runtime world-data)
        next-state (apply f (.runtimeState runtime) args)]
    (.setRuntimeState runtime next-state)
    (.markDirty runtime)
    (world-state-notify/notify-world-state-changed! (:world-key world-data))
    next-state))

(defn set-state-value!
  [world-data key value]
  (update-runtime-state! world-data assoc [key value])
  value)

(defn update-state-value!
  [world-data key f & args]
  (update-runtime-state! world-data update (into [key f] args))
  (state-value world-data key))

(defn update-state!
  [world-data f & args]
  (update-runtime-state! world-data f args))

(defn transient-value
  [world-data key]
  (when-let [^IWorldRuntime runtime (:runtime world-data)]
    (.get (.transientValues runtime) key)))

(defn update-transient-value!
  [world-data key f & args]
  (when-let [^IWorldRuntime runtime (:runtime world-data)]
    (let [^HashMap values (.transientValues runtime)]
      (.put values key (apply f (.get values key) args))))
  nil)

;; ============================================================================
;; Due-tick buckets — generic, keyed by an arbitrary bucket-key so a world can
;; hold multiple independent schedules (e.g. one per entity kind). Backed by
;; transient-values (never serialized); a fresh world-load naturally starts
;; with no scheduled entries. O(due items) to drain, not O(all scheduled).
;; ============================================================================

(defn- ensure-due-buckets!
  ^HashMap [world-data bucket-key]
  (or (transient-value world-data bucket-key)
      (let [m (HashMap.)]
        (update-transient-value! world-data bucket-key (constantly m))
        m)))

(defn schedule-due!
  "Schedule `item` to be returned by drain-due! once game-time reaches
   `due-tick` under `bucket-key`."
  [world-data bucket-key ^long due-tick item]
  (let [^HashMap buckets (ensure-due-buckets! world-data bucket-key)
        ^HashSet items (or (.get buckets due-tick)
                          (let [created (HashSet.)]
                            (.put buckets due-tick created)
                            created))]
    (.add items item))
  nil)

(defn unschedule-due!
  "Remove a previously scheduled item so it won't fire. Safe to call even if
   the item was never scheduled or already fired."
  [world-data bucket-key ^long due-tick item]
  (when-let [^HashMap buckets (transient-value world-data bucket-key)]
    (when-let [^HashSet items (.get buckets due-tick)]
      (.remove items item)
      (when (.isEmpty items)
        (.remove buckets due-tick))))
  nil)

(defn drain-due!
  "Return items whose bucket is at-or-before game-time, removing those
   buckets. O(number of currently populated buckets at-or-before game-time),
   independent of total scheduled item count."
  [world-data bucket-key ^long game-time]
  (if-let [^HashMap buckets (transient-value world-data bucket-key)]
    (let [due-keys (java.util.ArrayList.)]
      (doseq [k (.keySet buckets)]
        (when (<= (long k) game-time)
          (.add due-keys k)))
      (let [result (java.util.ArrayList.)]
        (doseq [k due-keys]
          (when-let [^HashSet items (.remove buckets k)]
            (.addAll result items)))
        result))
    []))

(def network-due-bucket-key
  "Shared transient-values key for the network due-tick bucket schedule.
   Lives here (not store.clj or world-runtime.clj) so both can reference the
   same key without a circular require."
  ::network-due-buckets)

(def ^:private last-game-time-key ::last-game-time)

(defn cache-game-time!
  "Stash the current tick's game-time so registration paths without a live
   world/game-time in scope (store.clj command call sites) can still compute
   an initial due-tick. Written once per tick by tick-world-data!."
  [world-data ^long game-time]
  (update-transient-value! world-data last-game-time-key (constantly game-time))
  nil)

(defn cached-game-time
  "Last game-time cached by cache-game-time!, or 0 before the world's first
   tick (e.g. a network registered by NBT load before any tick has run —
   self-corrects on that entity's first reschedule)."
  ^long [world-data]
  (or (transient-value world-data last-game-time-key) 0))

;; ============================================================================
;; Budgeted rebuild queues — world-load defers rebuild-*-indexes! commits
;; here instead of running them all synchronously in one call; tick-world-data!
;; drains a bounded batch per tick so loading a world with many networks or
;; connections never stalls a single tick rebuilding the entire topology.
;; ============================================================================

(def network-rebuild-queue-key
  "Shared transient-values key for pending network rebuild-from-NBT entries."
  ::network-rebuild-queue)

(def connection-rebuild-queue-key
  "Shared transient-values key for pending connection rebuild-from-NBT entries."
  ::connection-rebuild-queue)

(defn enqueue-rebuild!
  [world-data queue-key item]
  (update-transient-value! world-data queue-key
    (fnil conj clojure.lang.PersistentQueue/EMPTY) item)
  nil)

(defn drain-rebuild-queue!
  "Pop up to `n` items (FIFO) from queue-key, leaving the rest queued for a
   later call. Returns a vector of the popped items, oldest first."
  [world-data queue-key ^long n]
  (let [^clojure.lang.PersistentQueue q0 (or (transient-value world-data queue-key)
                                             clojure.lang.PersistentQueue/EMPTY)]
    (if (or (<= n 0) (.isEmpty q0))
      []
      (loop [q q0 taken 0 acc (transient [])]
        (if (or (>= taken n) (.isEmpty q))
          (do
            (update-transient-value! world-data queue-key (constantly q))
            (persistent! acc))
          (recur (pop q) (unchecked-inc taken) (conj! acc (peek q))))))))

(defn networks [world-data] (state-value world-data :networks))
(defn connections [world-data] (state-value world-data :connections))
(defn net-by-ssid [world-data] (state-value world-data :net-by-ssid))
(defn node-to-net [world-data] (state-value world-data :node-to-net))
(defn device-to-node [world-data] (state-value world-data :device-to-node))
(defn spatial-index [world-data] (state-value world-data :spatial-index))

(defn registry-snapshot
  []
  (if-let [fw-atom (fw/fw-atom)]
    (let [entries (get-in @fw-atom worlds-path)]
      (reduce-kv (fn [result key entry]
                   (let [data (:world-data entry)]
                     (assoc result key {:world-data data
                                        :world-state (world-state data)})))
                 {}
                 entries))
    {}))

(defn reset-registry!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in worlds-path {}))
  nil)
