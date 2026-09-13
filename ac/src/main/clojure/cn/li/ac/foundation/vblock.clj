(ns cn.li.ac.foundation.vblock
  "VBlock data structure - pure data representation of virtual blocks.
  
  A VBlock is a lightweight reference to a block position in the world
  without holding direct references to TileEntity or mutable state.
  
  This is the canonical data representation; all position operations
  should use foundation.position functions."
  (:require [cn.li.ac.foundation.position :as pos]))

;; ============================================================================
;; VBlock Record
;; ============================================================================

(defrecord VBlock
  [x              ; int - x coordinate
   y              ; int - y coordinate
   z              ; int - z coordinate
   block-type     ; keyword - :matrix/:node/:node-conn/:generator/:receiver
   ignore-chunk]) ; boolean - if true, force load chunk

;; ============================================================================
;; Factory Functions
;; ============================================================================

(defn vblock
  "Create a VBlock with explicit coordinates.
  
  Args:
    x (int): World x coordinate
    y (int): World y coordinate
    z (int): World z coordinate
    block-type (keyword): Type of block
    ignore-chunk (boolean, optional): Whether to ignore chunk loading (default: false)
    
  Returns:
    VBlock record"
  ([x y z block-type]
   (vblock x y z block-type false))
  ([x y z block-type ignore-chunk]
   (->VBlock x y z block-type ignore-chunk)))

(defn vblock?
  "Check if object is a VBlock instance.
  
  Args:
    obj: Object to check
    
  Returns:
    boolean"
  [obj]
  (instance? VBlock obj))

;; ============================================================================
;; VBlock Operations (Pure)
;; ============================================================================

(defn vblock->position
  "Extract position triple from VBlock.
  
  Args:
    vblock: VBlock record
    
  Returns:
    [x y z]: Position triple"
  [{:keys [x y z]}]
  [x y z])

(defn vblock->map
  "Convert VBlock to map (for serialization).
  
  Args:
    vblock: VBlock record
    
  Returns:
    map: {:x x :y y :z z :block-type keyword :ignore-chunk boolean}"
  [{:keys [x y z block-type ignore-chunk]}]
  {:x x :y y :z z :block-type block-type :ignore-chunk ignore-chunk})

(defn map->vblock
  "Construct VBlock from map (for deserialization).
  
  Args:
    m: {:x x :y y :z z :block-type keyword :ignore-chunk boolean}
    
  Returns:
    VBlock record"
  [{:keys [x y z block-type ignore-chunk]}]
  (vblock x y z block-type (boolean ignore-chunk)))

(defn vblock->chunk-key
  "Get chunk coordinate for a VBlock.
  
  Args:
    vblock: VBlock record
    
  Returns:
    [cx cy cz]: Chunk key"
  [{:keys [x y z]}]
  (pos/pos->chunk-key x y z))

(defn vblock-distance
  "Calculate distance between two VBlocks.
  
  Args:
    vblock1, vblock2: VBlock records
    
  Returns:
    double: Euclidean distance"
  [vblock1 vblock2]
  (pos/distance (vblock->position vblock1) (vblock->position vblock2)))

(defn vblock-distance-squared
  "Calculate squared distance between two VBlocks (faster).
  
  Args:
    vblock1, vblock2: VBlock records
    
  Returns:
    double: Squared distance"
  [vblock1 vblock2]
  (pos/distance-squared (vblock->position vblock1) (vblock->position vblock2)))

(defn vblock-nearby?
  "Check if two VBlocks are within search radius.
  
  Args:
    vblock1, vblock2: VBlock records
    radius (int): Search radius in blocks
    
  Returns:
    boolean"
  [vblock1 vblock2 radius]
  (<= (vblock-distance-squared vblock1 vblock2) (* radius radius)))

;; ============================================================================
;; Type-specific constructors (convenience)
;; ============================================================================

(defn vmatrix
  "Create a virtual matrix reference (always ignores chunk).
  
  Args:
    x, y, z (int): Coordinates
    
  Returns:
    VBlock with block-type :matrix and ignore-chunk true"
  [x y z]
  (vblock x y z :matrix true))

(defn vnode
  "Create a virtual node reference (checks chunks).
  
  Args:
    x, y, z (int): Coordinates
    
  Returns:
    VBlock with block-type :node and ignore-chunk false"
  [x y z]
  (vblock x y z :node false))

(defn vnode-conn
  "Create a virtual node connection reference (ignores chunks).
  
  Args:
    x, y, z (int): Coordinates
    
  Returns:
    VBlock with block-type :node-conn and ignore-chunk true"
  [x y z]
  (vblock x y z :node-conn true))

(defn vgenerator
  "Create a virtual generator reference (ignores chunks).
  
  Args:
    x, y, z (int): Coordinates
    
  Returns:
    VBlock with block-type :generator and ignore-chunk true"
  [x y z]
  (vblock x y z :generator true))

(defn vreceiver
  "Create a virtual receiver reference (ignores chunks).
  
  Args:
    x, y, z (int): Coordinates
    
  Returns:
    VBlock with block-type :receiver and ignore-chunk true"
  [x y z]
  (vblock x y z :receiver true))
