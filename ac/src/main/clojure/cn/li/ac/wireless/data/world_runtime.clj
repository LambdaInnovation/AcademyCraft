(ns cn.li.ac.wireless.data.world-runtime
  (:require [cn.li.ac.wireless.config :as network-config]
            [cn.li.ac.wireless.core.scheduling :as sched]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.network-runtime :as network-runtime]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.store :as store]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.runtime.node-transfer :as node-transfer]
            [cn.li.mcmod.platform.world :as platform-world]
            [cn.li.mcmod.util.log :as log]))

(defn network-impl-validator
  "Unregister disposed networks and networks whose matrix block is gone."
  [world-data world]
  (reduce-kv
    (fn [_ _ item]
      (when (or (network-state/is-disposed? item)
                (and (vb/is-chunk-loaded? (:matrix item) world)
                     (nil? (resolver/resolve-matrix-cap world (:matrix item)))))
        (store/unregister-network! world-data item))
      nil)
    nil (world-registry/networks world-data)))

(defn node-connection-impl-validator
  "Unregister disposed node connections."
  [world-data world]
  (reduce-kv
    (fn [_ _ item]
      (when (node-conn/is-disposed? item)
        (store/unregister-connection! world-data item))
      nil)
    nil (world-registry/connections world-data)))

(defn- drain-pending-rebuilds!
  "Commit a bounded batch of NBT-decoded networks/connections queued by
  world-data-from-nbt at world load. Spreads a large topology's rebuild
  across many ticks instead of rebuilding it all in the tick a world loads —
  see wireless.config's world-load-rebuild-batch-size."
  [world-data ^long batch-size]
  (doseq [net (world-registry/drain-rebuild-queue! world-data
                world-registry/network-rebuild-queue-key batch-size)]
    (store/rebuild-network-indexes! world-data net))
  (doseq [conn (world-registry/drain-rebuild-queue! world-data
                 world-registry/connection-rebuild-queue-key batch-size)]
    (store/rebuild-connection-indexes! world-data conn)))

(defn- tick-due-networks!
  "Advance only the networks whose due-tick bucket has come due, then
  reschedule each for its next occurrence `interval` ticks out. Replaces a
  full reduce-kv over every registered network — cost is O(due networks),
  not O(all networks). A drained position with no matching network (raced
  with unregister-network!) is silently skipped; it was never rescheduled
  so it does not recur."
  [world-data world ctx interval]
  (let [game-time (long (:game-time ctx))
        networks (world-registry/networks world-data)
        due-positions (world-registry/drain-due! world-data
                        world-registry/network-due-bucket-key game-time)]
    (doseq [pos due-positions]
      (when-let [net (get networks pos)]
        (network-runtime/tick-wireless-net! net world ctx)
        (world-registry/schedule-due! world-data world-registry/network-due-bucket-key
          (+ game-time interval) pos)))))

(defn tick-world-data!
  "Advance all wireless runtime state for one server tick.

  Builds a per-tick ctx once — game time, config snapshot, and a tick-scoped
  capability cache (java.util.HashMap; single-threaded, discarded at tick
  end) — and threads it through network and connection ticking. Disposed
  entities are swept out periodically instead of only at world save.

  Networks are driven from a due-tick bucket schedule (see store.clj's
  schedule-network-due!) instead of a full scan — each fires only on its own
  stagger slot. Connections still iterate every registered connection every
  tick: energy transfer runs unconditionally each tick (only the connection's
  own integrity validation is interval-staggered inside tick-node-conn!), so
  bucketing the driver loop would silently throttle live transfer instead of
  just skipping redundant work.

  Pending world-load rebuilds (see drain-pending-rebuilds!) are committed
  before the tick's state snapshot is taken, so entries rebuilt this tick
  are ticked starting this same tick rather than the next one."
  [world-data world]
  (drain-pending-rebuilds! world-data (long (network-config/world-load-rebuild-batch-size)))
  (let [state (world-registry/world-state world-data)
        cfg (network-config/cfg)
        game-time (long (or (platform-world/game-time world) 0))
        ctx {:game-time game-time
             :cfg cfg
             :cap-cache (java.util.HashMap.)}]
    (world-registry/cache-game-time! world-data game-time)
    (tick-due-networks! world-data world ctx (long (get cfg :network-update-interval-ticks)))
    (reduce-kv (fn [_ _ conn]
                 (when-not (node-conn/is-disposed? conn)
                   (node-transfer/tick-node-conn! conn world ctx))
                 nil)
               nil (get state :connections))
    (when (sched/due? game-time (long (get cfg :sweep-interval-ticks)) ::sweep)
      (network-impl-validator world-data world)
      (node-connection-impl-validator world-data world))))

(defn get-statistics
  "Get statistics about this world's wireless system."
  [world-data]
  {:networks (count (world-registry/networks world-data))
   :connections (count (world-registry/connections world-data))})

(defn print-statistics
  "Print statistics to log."
  [world-data]
  (let [stats (get-statistics world-data)]
    (log/debug "=== Wireless System Statistics ===")
    (log/debug (format "Networks: %d" (:networks stats)))
    (log/debug (format "Connections: %d" (:connections stats)))))
