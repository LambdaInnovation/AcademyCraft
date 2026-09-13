(ns cn.li.ac.wireless.core.capability-resolver
  "Resolve wireless capabilities from tiles or VBlocks."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-lookup :as cap-lookup])
  (:import [cn.li.acapi.wireless WirelessCapabilityKeys]))

(defn matrix-capability [tile]
  (cap-lookup/tile-capability tile WirelessCapabilityKeys/MATRIX))

(defn node-capability [tile]
  (cap-lookup/tile-capability tile WirelessCapabilityKeys/NODE))

(defn generator-capability [tile]
  (cap-lookup/tile-capability tile WirelessCapabilityKeys/GENERATOR))

(defn receiver-capability [tile]
  (cap-lookup/tile-capability tile WirelessCapabilityKeys/RECEIVER))

(defn resolve-tile
  "Resolve a VBlock to the platform tile after chunk/type checks."
  [world vblock]
  (vb/vblock-get vblock world))

(defn resolve-matrix-cap [world matrix-vblock]
  (matrix-capability (resolve-tile world matrix-vblock)))

(defn resolve-node-cap [world node-vblock]
  (node-capability (resolve-tile world node-vblock)))

(defn resolve-generator-cap [world generator-vblock]
  (generator-capability (resolve-tile world generator-vblock)))

(defn resolve-receiver-cap [world receiver-vblock]
  (receiver-capability (resolve-tile world receiver-vblock)))

(defn resolve-capability
  "Resolve the capability implied by a VBlock's block type."
  [world vblock]
  (case (:block-type vblock)
    :matrix (resolve-matrix-cap world vblock)
    :node (resolve-node-cap world vblock)
    :node-conn (resolve-node-cap world vblock)
    :generator (resolve-generator-cap world vblock)
    :receiver (resolve-receiver-cap world vblock)
    nil))

(def ^:private cache-miss ::miss)

(defn resolve-cap-cached
  "Resolve `(resolve-fn world vblock)` through a tick-scoped mutable cache.

  `cache` is a java.util.HashMap owned by the per-tick ctx: single-threaded
  (server thread) use only, discarded at tick end — capability identity is
  stable within one tick, so misses are cached too. `nil` cache falls through
  to a direct resolve (non-tick callers).

  Keyed by the VBlock record itself (defrecord equals/hashCode over all
  fields) — zero allocation on the lookup path, vs. a fresh 4-element vector
  (plus boxed ints) built on every call under the old `[x y z block-type]`
  key, even on a cache hit."
  [^java.util.HashMap cache resolve-fn world vblock]
  (if (nil? cache)
    (resolve-fn world vblock)
    (let [hit (.get cache vblock)]
      (cond
        (identical? cache-miss hit) nil
        (some? hit) hit
        :else (let [v (resolve-fn world vblock)]
                (.put cache vblock (or v cache-miss))
                v)))))
