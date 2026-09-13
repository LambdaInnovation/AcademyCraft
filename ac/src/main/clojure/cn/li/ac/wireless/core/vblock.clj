(ns cn.li.ac.wireless.core.vblock
  "Virtual block reference system for wireless network.

  Provides position-based TileEntity references that:
  - Support NBT serialization (avoiding direct TileEntity refs)
  - Lazy loading (only fetch when needed)
  - Chunk loading safety
  - Distance calculations

  Type checking (Design-3):
  - For Clojure state maps: use :plate-count / :node-type keys
  - For Java ScriptedBlockEntity: use getCapability().isPresent() via CapabilitySlots"
  (:require [cn.li.ac.foundation.vblock :as foundation-vb]
            [cn.li.ac.wireless.core.vblock-resolver :as resolver]
            [cn.li.mcmod.platform.position :as pos]))

;; ============================================================================
;; VBlock Record
;; ============================================================================

(defrecord VBlock
  [x              ; int - x coordinate
   y              ; int - y coordinate
   z              ; int - z coordinate
   block-type     ; keyword - :matrix/:node/:node-conn/:generator/:receiver
   ignore-chunk]) ; boolean - if true, force load chunk

(defn from-foundation
  "Create a wireless runtime VBlock from the pure foundation representation."
  [vblock]
  (->VBlock (:x vblock) (:y vblock) (:z vblock) (:block-type vblock) (:ignore-chunk vblock)))

(defn to-foundation
  "Convert a wireless runtime VBlock to the pure foundation representation."
  [vblock]
  (foundation-vb/vblock (:x vblock) (:y vblock) (:z vblock) (:block-type vblock) (:ignore-chunk vblock)))

;; ============================================================================
;; Factory Functions
;; ============================================================================

(defn create-vblock
  "Create a virtual block reference from a TileEntity"
  [tile-entity block-type ignore-chunk]
  ;; MC 1.17+ renamed getPos() to getBlockPos() on BlockEntity; try both.
  (let [block-pos (or (try (pos/block-pos tile-entity) (catch Exception _ nil))
                      (try (pos/pos tile-entity) (catch Exception _ nil)))]
    (from-foundation
      (foundation-vb/vblock
        (pos/pos-x block-pos)
        (pos/pos-y block-pos)
        (pos/pos-z block-pos)
        block-type
        ignore-chunk))))

(defn create-vmatrix
  "Create virtual matrix reference (always ignore chunk)"
  ([matrix-tile]
   (create-vblock matrix-tile :matrix true))
  ([x y z]
    (from-foundation (foundation-vb/vmatrix x y z))))

(defn create-vnode
  "Create virtual node reference for WirelessNet (check chunks)"
  ([node-tile]
   (create-vblock node-tile :node false))
  ([x y z]
    (from-foundation (foundation-vb/vnode x y z))))

(defn create-vnode-conn
  "Create virtual node reference for NodeConn (ignore chunks)"
  [node-tile]
  (create-vblock node-tile :node-conn true))

(defn create-vgenerator
  "Create virtual generator reference (ignore chunks)"
  [gen-tile]
  (create-vblock gen-tile :generator true))

(defn create-vreceiver
  "Create virtual receiver reference (ignore chunks)"
  [rec-tile]
  (create-vblock rec-tile :receiver true))

;; ============================================================================
;; Core Operations
;; ============================================================================

(defn pos-of
  "Position tuple [x y z] for a vblock — the canonical topology map key.
  All world-state maps (:networks/:connections/:node-to-net/:device-to-node/
  :spatial-index) key by this tuple, never by the vblock record itself."
  [vblock]
  (foundation-vb/vblock->position vblock))

(defn vblock-pos
  "Get BlockPos for vblock using platform abstraction"
  [vblock]
  (resolver/vblock-pos vblock))

(defn is-chunk-loaded?
  "Check if the chunk containing this vblock is loaded"
  [vblock w]
  (resolver/is-chunk-loaded? vblock w))

(defn vblock-get
  "Get the TileEntity/state for this vblock (lazy loading).

  Returns nil if:
  - Chunk not loaded (unless ignore-chunk)
  - World is nil
  - TileEntity doesn't exist
  - TileEntity type doesn't match expected block-type

  Returns the BlockEntity instance (which holds customState internally)."
  [vblock w]
  (resolver/vblock-get vblock w))

(defn dist-sq
  "Calculate squared distance between two vblocks"
  [vblock1 vblock2]
  (foundation-vb/vblock-distance-squared vblock1 vblock2))

(defn dist-sq-pos
  "Calculate squared distance from vblock to coordinates"
  [vblock x y z]
  (foundation-vb/vblock-distance-squared
    vblock
    (foundation-vb/vblock x y z (:block-type vblock) (:ignore-chunk vblock))))

;; ============================================================================
;; Equality and Hashing
;; ============================================================================

(defn vblock-equals?
  "Check if two vblocks refer to the same position"
  [vb1 vb2]
  (= (foundation-vb/vblock->position vb1)
     (foundation-vb/vblock->position vb2)))

(defn vblock-hash
  "Calculate hash code for vblock (for use in maps)"
  [vblock]
  (bit-xor (:x vblock) (:y vblock) (:z vblock)))

;; Records keep full-field equality; this helper intentionally compares only
;; world position because topology indexes are position-keyed.

;; ============================================================================
;; Utility Functions
;; ============================================================================

(defn vblock-to-string
  "Convert vblock to debug string"
  [vblock]
  (format "%s[%d, %d, %d]"
          (name (:block-type vblock))
          (:x vblock)
          (:y vblock)
          (:z vblock)))

(defn validate-vblock
  "Validate that vblock has valid coordinates"
  [vblock]
  (and (integer? (:x vblock))
       (integer? (:y vblock))
       (integer? (:z vblock))
       (keyword? (:block-type vblock))
       (boolean? (:ignore-chunk vblock))))

;; ============================================================================
;; Collection Helpers
;; ============================================================================

(defn remove-invalid-vblocks
  "Remove vblocks that don't resolve to valid TileEntities"
  [vblocks w]
  (filterv #(some? (vblock-get % w)) vblocks))

(defn find-vblock-by-pos
  "Find vblock in collection by coordinates"
  [vblocks x y z]
  (first (filter #(= [x y z] (foundation-vb/vblock->position %))
                 vblocks)))
