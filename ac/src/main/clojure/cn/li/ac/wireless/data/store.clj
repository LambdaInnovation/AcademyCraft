(ns cn.li.ac.wireless.data.store
  "Primitive wireless topology commits.

  Each function applies one pure `domain.topology` transform through a single
  atomic `world-registry` swap and owns the corresponding log line. No
  capability IO and no admission checks here — those live in
  `wireless.service.commands`. Data-layer callers (persistence, runtime
  sweeps, balance cleanup) use this namespace instead of reaching up into
  the service layer."
  (:require [cn.li.ac.wireless.config :as network-config]
            [cn.li.ac.wireless.core.scheduling :as sched]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.spatial-lookup :as spatial]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.domain.topology :as topology]
            [cn.li.mcmod.util.log :as log]))

(defn- schedule-network-due!
  "Schedule net's first due-tick, staggered by its matrix position (same
   phase-seed tick-wireless-net!'s own due? check uses) — the driver in
   world-runtime only calls into a network once its bucket comes due."
  [world-data net]
  (let [game-time (world-registry/cached-game-time world-data)
        interval (long (:network-update-interval-ticks (network-config/cfg)))
        due-tick (sched/next-due-tick game-time interval (vb/pos-of (:matrix net)))]
    (world-registry/schedule-due! world-data world-registry/network-due-bucket-key due-tick
      (vb/pos-of (:matrix net))))
  nil)

(defn register-network!
  [world-data net]
  (world-registry/update-state! world-data topology/register-network net)
  (schedule-network-due! world-data net)
  net)

(defn unregister-network!
  "Mark `net` disposed and remove it plus its lookup entries. Returns nil."
  [world-data net]
  (let [net (entity-commit/resolve-network world-data net)
        disposed (network-state/set-state-value net :disposed true)]
    (world-registry/update-state! world-data topology/unregister-network disposed)
    (log/info (format "Destroyed network: SSID='%s'" (network-state/get-ssid disposed)))
    nil))

(defn register-connection!
  [world-data conn]
  (world-registry/update-state! world-data topology/register-connection conn)
  conn)

(defn- clear-device-stale-entries!
  [world-data conn]
  (doseq [device (concat (get-in conn [:state :generators])
                         (get-in conn [:state :receivers]))]
    (node-conn/clear-stale-entry! world-data (vb/pos-of device))))

(defn unregister-connection!
  "Mark `conn` disposed, remove it plus its device lookups, and drop its
  transient stale timestamps. Returns nil."
  [world-data conn]
  (let [conn (entity-commit/resolve-connection world-data conn)
        disposed (assoc-in conn [:state :disposed] true)]
    (world-registry/update-state! world-data topology/unregister-connection disposed)
    (clear-device-stale-entries! world-data disposed)
    (log/info (format "Destroyed node connection: %s"
                      (vb/vblock-to-string (:node conn))))
    nil))

(defn- commit-and-unlink-node
  [state net* node-vblock]
  (-> state
      (assoc-in [:networks (vb/pos-of (:matrix net*))] net*)
      (topology/unlink-network-node node-vblock)))

(defn- commit-and-link-node
  [state net* node-vblock]
  (-> state
      (assoc-in [:networks (vb/pos-of (:matrix net*))] net*)
      (topology/link-network-node net* node-vblock)))

(defn unlink-node!
  "Remove `node-vblock` from its network's node list and lookup entry.
  Returns true when the node was present."
  [world-data network node-vblock]
  (let [network (entity-commit/resolve-network world-data network)
        {:keys [network removed?]} (topology/remove-node-from-network network node-vblock)]
    (when removed?
      (world-registry/update-state! world-data commit-and-unlink-node network node-vblock)
      (log/info (format "Removed node %s from '%s'"
                        (vb/vblock-to-string node-vblock)
                        (network-state/get-ssid network))))
    removed?))

(defn link-node!
  "Add `node-vblock` to `net*`'s node list and lookup entry (no admission
  checks — callers validate first)."
  [world-data net* node-vblock]
  (world-registry/update-state! world-data commit-and-link-node net* node-vblock)
  (log/info (format "Added node %s to network '%s'"
                    (vb/vblock-to-string node-vblock)
                    (network-state/get-ssid net*)))
  nil)

(defn- commit-and-refresh-ssid
  [state network* old-ssid new-ssid]
  (-> state
      (assoc-in [:networks (vb/pos-of (:matrix network*))] network*)
      (topology/refresh-ssid-lookup old-ssid new-ssid network*)))

(defn rename-network!
  "Commit `network*` (already carrying the new ssid) and refresh the
  ssid lookup entry. Returns nil."
  [world-data network* old-ssid new-ssid]
  (world-registry/update-state! world-data commit-and-refresh-ssid network* old-ssid new-ssid)
  (log/info (format "Network ssid changed to '%s'" new-ssid))
  nil)

(defn rebuild-network-indexes!
  [world-data net]
  (world-registry/update-state! world-data topology/rebuild-network-indexes net)
  (schedule-network-due! world-data net)
  nil)

(defn rebuild-connection-indexes!
  [world-data conn]
  (world-registry/update-state! world-data topology/rebuild-connection-indexes conn)
  nil)

(defn add-spatial-vblock!
  [world-data vblock]
  (spatial/add-to-spatial-index! world-data vblock))

(defn remove-spatial-vblock!
  [world-data vblock]
  (spatial/remove-from-spatial-index! world-data vblock))
