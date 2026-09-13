(ns cn.li.ac.foundation.position
  "Unified position operation utilities for wireless system.
  
  Centralizes all position calculations to ensure consistency and reduce duplication:
  - Chunk coordinate conversion
  - Distance calculations
  - Nearby chunks discovery
  
  Design principle: Pure functions, no world state dependency."
  (:require [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Chunk Coordinate Conversion
;; ============================================================================

(defn pos->chunk-key
  "Convert world coordinates (x, y, z) to chunk key [cx, cy, cz].

  Chunks are 16×16×16 blocks. Uses arithmetic shift so negative
  coordinates bucket uniformly (floor division), matching Minecraft's
  own chunk coordinate math.

  Args:
    x (int): World x coordinate
    y (int): World y coordinate
    z (int): World z coordinate

  Returns:
    [cx cy cz]: Chunk coordinates"
  [x y z]
  [(bit-shift-right (long x) 4)
   (bit-shift-right (long y) 4)
   (bit-shift-right (long z) 4)])

(defn chunk-key->bounds
  "Get the bounding box for a chunk key.
  
  Returns:
    {:min [x y z] :max [x y z]}: Min/max coordinates in chunk"
  [[cx cy cz]]
  {:min [(* cx 16) (* cy 16) (* cz 16)]
   :max [(+ (* cx 16) 15) (+ (* cy 16) 15) (+ (* cz 16) 15)]})

;; ============================================================================
;; Distance Calculations
;; ============================================================================

(defn distance-squared
  "Calculate squared Euclidean distance between two positions.
  
  Used for performance (avoids sqrt when not needed).
  
  Args:
    [x1 y1 z1] [x2 y2 z2]: Positions
    
  Returns:
    double: Squared distance"
  [[x1 y1 z1] [x2 y2 z2]]
  (let [dx (- x2 x1)
        dy (- y2 y1)
        dz (- z2 z1)]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn distance
  "Calculate Euclidean distance between two positions.
  
  Args:
    [x1 y1 z1] [x2 y2 z2]: Positions
    
  Returns:
    double: Distance"
  [pos1 pos2]
  (Math/sqrt (distance-squared pos1 pos2)))

(defn manhattan-distance
  "Calculate Manhattan distance (L∞ metric).
  
  Used for axis-aligned searches.
  
  Args:
    [x1 y1 z1] [x2 y2 z2]: Positions
    
  Returns:
    int: Manhattan distance"
  [[x1 y1 z1] [x2 y2 z2]]
  (+ (Math/abs (int (- x2 x1)))
     (Math/abs (int (- y2 y1)))
     (Math/abs (int (- z2 z1)))))

;; ============================================================================
;; Nearby Chunks Discovery
;; ============================================================================

(defn- conj-chunk-keys-dz!
  [out cx cy cz dx dy lo hi]
  (let [cx (long cx) cy (long cy) cz (long cz)
        dx (long dx) dy (long dy)
        lo (long lo) hi (long hi)]
    (loop [dz lo
           out out]
      (if (>= dz hi)
        out
        (recur (unchecked-inc dz)
               (conj! out [(+ cx dx) (+ cy dy) (+ cz dz)]))))))

(defn- conj-chunk-keys-dy!
  [out cx cy cz dx lo hi]
  (let [cx (long cx) cy (long cy) cz (long cz)
        dx (long dx) lo (long lo) hi (long hi)]
    (loop [dy lo
           out out]
      (if (>= dy hi)
        out
        (recur (unchecked-inc dy)
               (conj-chunk-keys-dz! out cx cy cz dx dy lo hi))))))

(defn nearby-chunk-keys
  "Find all chunks within a given search radius from position (x, y, z).

  Returns a sequence of chunk keys whose bounding boxes overlap the search sphere.
  This uses chunk-range approximation (conservative, may include chunks beyond radius).

  Args:
    x (int): World x coordinate
    y (int): World y coordinate
    z (int): World z coordinate
    search-radius (int): Search radius in blocks

  Returns:
    Sequence of [cx cy cz] chunk keys"
  [x y z search-radius]
  (let [radius (long search-radius)
        chunk-range (inc (quot radius 16))
        [cx cy cz] (pos->chunk-key x y z)
        lo (- chunk-range)
        hi (inc chunk-range)]
    ;; Primitive nested loops + transient — avoids `for` class stacking and
    ;; intermediate persistent vectors / range seqs on spatial queries.
    (persistent!
     (loop [dx lo
            out (transient [])]
       (if (>= dx hi)
         out
         (recur (unchecked-inc dx)
                (conj-chunk-keys-dy! out cx cy cz dx lo hi)))))))

(defn chunk-range
  "Calculate chunk range from center position and search radius.

  Args:
    x, y, z (int): Center position coordinates
    search-radius (int): Search radius in blocks

  Returns:
    {:center-chunk [cx cy cz]
     :range chunk-range
     :num-chunks int}: Chunk range info"
  [x y z search-radius]
  (let [radius (long search-radius)
        chunk-range (inc (quot radius 16))
        [cx cy cz] (pos->chunk-key x y z)
        num-chunks (* (inc (* 2 chunk-range))
                      (inc (* 2 chunk-range))
                      (inc (* 2 chunk-range)))]
    {:center-chunk [cx cy cz]
     :range chunk-range
     :num-chunks num-chunks}))

;; ============================================================================
;; Validation
;; ============================================================================

(defn valid-position?
  "Check if position coordinates are valid (not NaN, not infinite).
  
  Args:
    x, y, z: Coordinates (any numeric type)
    
  Returns:
    boolean: true if all are finite numbers"
  [x y z]
  (and (Double/isFinite (double x))
       (Double/isFinite (double y))
       (Double/isFinite (double z))))

(defn valid-position-triple?
  "Check if a [x y z] triple is valid.
  
  Args:
    [x y z]: Position triple
    
  Returns:
    boolean: true if valid"
  [[x y z]]
  (valid-position? x y z))

(defn ^{:added "1.0"}
  position->map
  "Convert position to map representation for serialization.
  
  Args:
    x, y, z: Coordinates
    
  Returns:
    {:x x :y y :z z}"
  [x y z]
  {:x (int x) :y (int y) :z (int z)})

(defn ^{:added "1.0"}
  map->position
  "Convert map back to position triple.
  
  Args:
    {:x x :y y :z z}: Position map
    
  Returns:
    [x y z]"
  [{:keys [x y z]}]
  [x y z])
