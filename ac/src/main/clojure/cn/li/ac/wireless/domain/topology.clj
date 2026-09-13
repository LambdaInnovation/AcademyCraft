(ns cn.li.ac.wireless.domain.topology
  "Pure wireless topology transitions over world-state maps.

  All maps key by position tuple [x y z] (see `vb/pos-of`). The spatial index
  tracks placed wireless node blocks only; entries are added on placement /
  registration and removed only when the block is broken.

  No world-registry access, capability IO, or logging — `wireless.data.store`
  applies these transforms in single atomic swaps; admission rules (password,
  capacity, range) live in `wireless.service.commands`."
  (:require [cn.li.ac.wireless.core.spatial-index :as si]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.network-state :as net-state]))

(defn- connection-generators [conn]
  (vec (or (get-in conn [:state :generators]) [])))

(defn- connection-receivers [conn]
  (vec (or (get-in conn [:state :receivers]) [])))

(defn- dissoc-entries-pointing-at
  "Remove `pos->target` entries for the given vblocks, but only when they
  still point at `target` (a vblock re-linked elsewhere is left alone)."
  [m target vblocks]
  (reduce (fn [m vblock]
            (let [p (vb/pos-of vblock)]
              (if (= target (get m p))
                (dissoc m p)
                m)))
          m
          vblocks))

;; ============================================================================
;; Networks
;; ============================================================================

(defn register-network
  [state net]
  (let [mpos (vb/pos-of (:matrix net))]
    (-> state
        (update :networks assoc mpos net)
        (update :net-by-ssid assoc (net-state/get-ssid net) mpos))))

(defn link-network-node
  [state net node-vblock]
  (let [mpos (vb/pos-of (:matrix net))
        npos (vb/pos-of node-vblock)]
    (-> state
        (update :node-to-net assoc npos mpos)
        (update :spatial-index si/add-to-index npos))))

(defn unlink-network-node
  [state node-vblock]
  (update state :node-to-net dissoc (vb/pos-of node-vblock)))

(defn unregister-network
  [state net]
  (let [mpos (vb/pos-of (:matrix net))
        ssid (net-state/get-ssid net)]
    (-> state
        (update :networks dissoc mpos)
        (update :net-by-ssid (fn [m] (if (= mpos (get m ssid)) (dissoc m ssid) m)))
        (update :node-to-net dissoc-entries-pointing-at mpos (net-state/get-nodes net)))))

(defn refresh-ssid-lookup
  [state old-ssid new-ssid network]
  (let [mpos (vb/pos-of (:matrix network))]
    (-> state
        (update :net-by-ssid (fn [m] (if (= mpos (get m old-ssid)) (dissoc m old-ssid) m)))
        (update :net-by-ssid assoc new-ssid mpos))))

;; ============================================================================
;; Connections
;; ============================================================================

(defn register-connection
  [state conn]
  (let [npos (vb/pos-of (:node conn))]
    (-> state
        (update :connections assoc npos conn)
        (update :spatial-index si/add-to-index npos))))

(defn link-connection-device
  [state conn device-vblock]
  (update state :device-to-node assoc
          (vb/pos-of device-vblock) (vb/pos-of (:node conn))))

(defn unlink-connection-device
  [state device-vblock]
  (update state :device-to-node dissoc (vb/pos-of device-vblock)))

(defn unregister-connection
  [state conn]
  (let [npos (vb/pos-of (:node conn))
        devices (concat (connection-generators conn) (connection-receivers conn))]
    (-> state
        (update :connections dissoc npos)
        (update :device-to-node dissoc-entries-pointing-at npos devices))))

;; ============================================================================
;; Spatial index (placed node blocks)
;; ============================================================================

(defn track-node-position
  [state node-vblock]
  (update state :spatial-index si/add-to-index (vb/pos-of node-vblock)))

(defn untrack-node-position
  [state node-vblock]
  (update state :spatial-index si/remove-from-index (vb/pos-of node-vblock)))

;; ============================================================================
;; Rebuild (deserialization)
;; ============================================================================

(defn rebuild-network-indexes
  [state net]
  (reduce (fn [state node-vb]
            (link-network-node state net node-vb))
          (register-network state net)
          (net-state/get-nodes net)))

(defn rebuild-connection-indexes
  [state conn]
  (let [state (register-connection state conn)]
    (reduce (fn [s device] (link-connection-device s conn device))
            state
            (concat (connection-generators conn) (connection-receivers conn)))))

;; ============================================================================
;; Node membership
;; ============================================================================

(defn add-node-to-network
  [network node-vblock]
  (net-state/set-state-value
    network
    :nodes
    (conj (net-state/get-nodes network) node-vblock)))

(defn remove-node-from-network
  [network node-vblock]
  (let [nodes (net-state/get-nodes network)
        removed? (boolean (some #(vb/vblock-equals? % node-vblock) nodes))]
    {:network (net-state/set-state-value
                network
                :nodes
                (filterv #(not (vb/vblock-equals? % node-vblock)) nodes))
     :removed? removed?}))
