(ns cn.li.ac.wireless.service.queries
  "Application-level wireless read queries.

  Resolves tiles to vblocks and reads immutable world snapshots. Public callers
  should use `cn.li.ac.wireless.api`."
  (:require [cn.li.ac.wireless.config :as wireless-config]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.spatial-lookup :as spatial]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.domain.model :as model]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos])
  (:import [cn.li.acapi.wireless IWirelessNode]))

(defn- tile-level
  [tile]
  (platform-be/be-get-world-safe tile))

(defn find-network-by-matrix
  [matrix-tile]
  (let [world (tile-level matrix-tile)
        world-data (world-registry/get-world-data world)
        matrix-vb (vb/create-vmatrix matrix-tile)]
    (lookup/get-network-by-matrix world-data matrix-vb)))

(defn find-network-by-node
  [node-tile]
  (let [world (tile-level node-tile)
        world-data (world-registry/get-world-data world)
        node-vb (vb/create-vnode node-tile)]
    (lookup/get-network-by-node world-data node-vb)))

(defn find-network-by-ssid
  [world ssid]
  (let [world-data (world-registry/get-world-data world)]
    (lookup/get-network-by-ssid world-data ssid)))

(defn find-networks-in-range
  [world x y z range max-results]
  (let [world-data (world-registry/get-world-data world)]
    (lookup/range-search-networks world-data x y z range max-results)))

(defn find-node-connection-by-node
  [node-tile]
  (let [world (platform-be/be-get-world-safe node-tile)
        world-data (world-registry/get-world-data world)
        node-vb (vb/create-vnode-conn node-tile)]
    (lookup/get-node-connection world-data node-vb)))

(defn find-node-connection-by-generator
  [gen-tile]
  (let [world (platform-be/be-get-world-safe gen-tile)
        world-data (world-registry/get-world-data world)
        gen-vb (vb/create-vgenerator gen-tile)]
    (lookup/get-node-connection world-data gen-vb)))

(defn find-node-connection-by-receiver
  [rec-tile]
  (let [world (platform-be/be-get-world-safe rec-tile)
        world-data (world-registry/get-world-data world)
        rec-vb (vb/create-vreceiver rec-tile)]
    (lookup/get-node-connection world-data rec-vb)))

(defn find-available-nodes-at
  "Find node capabilities in range of coordinates that can accept links.
  Candidates come from the spatial index (position tuples of placed node
  blocks); stale entries fail capability resolution and drop out."
  [world x y z]
  (let [search-range (wireless-config/node-search-range)
        max-results (wireless-config/max-results)
        world-data (world-registry/get-world-data world)
        nearby-chunks (spatial/get-nearby-chunks x y z search-range)
        candidate-positions (spatial/get-positions-in-chunks world-data nearby-chunks)
        range-sq (* search-range search-range)
        matching-nodes
        (reduce
          (fn [acc [nx ny nz]]
            (let [node-vb (vb/create-vnode nx ny nz)
                  dist-sq (vb/dist-sq-pos node-vb x y z)]
              (if (<= dist-sq range-sq)
                (if-let [node (resolver/resolve-node-cap world node-vb)]
                  (let [node-range (.getRange ^IWirelessNode node)
                        node-range-sq (* node-range node-range)]
                    (if (<= dist-sq node-range-sq)
                      (if-let [conn (lookup/get-node-connection world-data node-vb)]
                        (if (model/connection-has-capacity?
                              {:receivers (node-conn/get-receivers conn)
                               :generators (node-conn/get-generators conn)}
                              (node-conn/get-capacity conn world))
                          (conj acc node)
                          acc)
                        (conj acc node))
                      acc))
                  acc)
                acc)))
          []
          candidate-positions)]
    (vec (take max-results matching-nodes))))

(defn find-available-nodes
  [world block-pos]
  (find-available-nodes-at world (pos/pos-x block-pos) (pos/pos-y block-pos) (pos/pos-z block-pos)))
