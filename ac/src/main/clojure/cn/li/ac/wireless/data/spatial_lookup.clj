(ns cn.li.ac.wireless.data.spatial-lookup
  "Spatial-index helpers for wireless world data. Index members are
  position tuples [x y z] of placed wireless node blocks."
  (:require [cn.li.ac.wireless.core.spatial-index :as si]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.world-registry :as world-registry]))

(defn add-to-spatial-index!
  "Track a placed wireless node block in the spatial index."
  [world-data vblock]
  (world-registry/update-state-value!
    world-data :spatial-index si/add-to-index (vb/pos-of vblock)))

(defn remove-from-spatial-index!
  "Untrack a broken wireless node block from the spatial index."
  [world-data vblock]
  (world-registry/update-state-value!
    world-data :spatial-index si/remove-from-index (vb/pos-of vblock)))

(defn get-nearby-chunks
  "Get chunk keys within range of a position (delegates to spatial-index)."
  [x y z search-radius]
  (si/nearby-chunk-keys x y z search-radius))

(defn get-positions-in-chunks
  "Get all node position tuples in the specified chunks."
  [world-data chunk-keys]
  (si/positions-in-index (world-registry/spatial-index world-data) chunk-keys))
