(ns cn.li.ac.wireless.core.vblock-resolver
  "Runtime resolver for wireless virtual block references.

  This namespace owns world/chunk/tile/capability lookup. It deliberately stays
  separate from NBT codecs so persistence remains data-only."
  (:require [cn.li.ac.wireless.core.capability-lookup :as cap-lookup]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world])
  (:import [cn.li.acapi.wireless WirelessCapabilityKeys]))

(defn vblock-pos
  "Get BlockPos for a vblock using the platform abstraction."
  [vblock]
  (pos/create-block-pos (:x vblock) (:y vblock) (:z vblock)))

(defn is-chunk-loaded?
  "Check if the chunk containing this vblock is loaded."
  [vblock w]
  (let [chunk-x (bit-shift-right (:x vblock) 4)
        chunk-z (bit-shift-right (:z vblock) 4)]
    (world/chunk-loaded? w chunk-x chunk-z)))

(defn- has-capability?
  "Delegates to the shared capability-lookup/tile-capability — no more duplication."
  [tile cap-key]
  (some? (cap-lookup/tile-capability tile cap-key)))

(defn- tile-has-wireless-matrix? [tile]
  (if (map? tile)
    (contains? tile :plate-count)
    (has-capability? tile WirelessCapabilityKeys/MATRIX)))

(defn- tile-has-wireless-node? [tile]
  (if (map? tile)
    (contains? tile :node-type)
    (has-capability? tile WirelessCapabilityKeys/NODE)))

(defn- tile-has-wireless-generator? [tile]
  (has-capability? tile WirelessCapabilityKeys/GENERATOR))

(defn- tile-has-wireless-receiver? [tile]
  (has-capability? tile WirelessCapabilityKeys/RECEIVER))

(defn vblock-get
  "Get the TileEntity/state for this vblock with chunk and capability checks."
  [vblock w]
  (when w
    (when (or (:ignore-chunk vblock) (is-chunk-loaded? vblock w))
      (let [block-pos (vblock-pos vblock)
            tile      (world/get-tile-entity w block-pos)]
        (when tile
          (case (:block-type vblock)
            :matrix    (when (tile-has-wireless-matrix?    tile) tile)
            :node      (when (tile-has-wireless-node?      tile) tile)
            :node-conn (when (tile-has-wireless-node?      tile) tile)
            :generator (when (tile-has-wireless-generator? tile) tile)
            :receiver  (when (tile-has-wireless-receiver?  tile) tile)
            nil))))))
