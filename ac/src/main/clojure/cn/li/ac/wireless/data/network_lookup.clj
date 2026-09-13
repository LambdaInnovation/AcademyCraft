(ns cn.li.ac.wireless.data.network-lookup
  "Network and node-connection lookup helpers over WiWorldData.

  All lookups go through position tuples (`vb/pos-of`); entity values live
  only in the :networks/:connections maps."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.world-registry :as world-registry]))

(defn- disposed? [entity]
  (boolean (get-in entity [:state :disposed])))

(defn get-network-by-matrix
  "Get network by matrix vblock."
  [world-data matrix-vblock]
  (get (world-registry/networks world-data) (vb/pos-of matrix-vblock)))

(defn get-network-by-node
  "Get network by node vblock."
  [world-data node-vblock]
  (when-let [mpos (get (world-registry/node-to-net world-data) (vb/pos-of node-vblock))]
    (get (world-registry/networks world-data) mpos)))

(defn get-network-by-ssid
  "Get network by SSID string."
  [world-data ssid]
  (when-let [mpos (get (world-registry/net-by-ssid world-data) ssid)]
    (get (world-registry/networks world-data) mpos)))

(defn get-node-connection
  "Get node connection by node/generator/receiver vblock."
  [world-data vblock]
  (let [p (vb/pos-of vblock)
        conns (world-registry/connections world-data)]
    (or (get conns p)
        (when-let [npos (get (world-registry/device-to-node world-data) p)]
          (get conns npos)))))

(defn range-search-networks
  "Search for live (non-disposed) networks whose matrix is within range of
  (x,y,z). O(n) over all networks (typically <100)."
  [world-data x y z search-radius max-results]
  (let [range-sq (* search-radius search-radius)]
    (->> (vals (world-registry/networks world-data))
         (remove disposed?)
         (filter (fn [net]
                   (when-let [matrix-vb (:matrix net)]
                     (<= (vb/dist-sq-pos matrix-vb x y z) range-sq))))
         (take max-results)
         vec)))
