(ns cn.li.ac.wireless.data.entity-commit
  "O(1) commit/resolve for wireless network/connection entities.

  Entity identity is its immutable world position (matrix pos for networks,
  node pos for connections). Commits assoc into the position-keyed maps only
  when the entity is currently registered — registration itself goes through
  `wireless.domain.topology` via `wireless.service.commands`."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.world-registry :as world-registry]))

(defn- commit-if-registered
  [entities pos new-entity]
  (if (contains? entities pos)
    (assoc entities pos new-entity)
    entities))

(defn commit-network!
  "Persist `new-net` into world state when its matrix position is registered.
  Returns `new-net`."
  [world-data new-net]
  (world-registry/update-state-value!
    world-data :networks
    commit-if-registered (vb/pos-of (:matrix new-net)) new-net)
  new-net)

(defn commit-connection!
  "Persist `new-conn` into world state when its node position is registered.
  Returns `new-conn`."
  [world-data new-conn]
  (world-registry/update-state-value!
    world-data :connections
    commit-if-registered (vb/pos-of (:node new-conn)) new-conn)
  new-conn)

(defn resolve-network
  "Return the currently registered network at `network`'s matrix position,
  or `network` itself when unregistered."
  [world-data network]
  (or (get (world-registry/networks world-data) (vb/pos-of (:matrix network)))
      network))

(defn resolve-network-by-ssid
  "Return the currently registered network for `ssid`, or nil."
  [world-data ssid]
  (when-let [mpos (get (world-registry/net-by-ssid world-data) ssid)]
    (get (world-registry/networks world-data) mpos)))

(defn resolve-connection
  "Return the currently registered connection at `connection`'s node position,
  or `connection` itself when unregistered."
  [world-data connection]
  (or (get (world-registry/connections world-data) (vb/pos-of (:node connection)))
      connection))
