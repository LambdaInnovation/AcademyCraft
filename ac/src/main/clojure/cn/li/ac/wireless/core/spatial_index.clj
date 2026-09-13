(ns cn.li.ac.wireless.core.spatial-index
  "Chunk-bucketed spatial index for wireless node discovery.

  Immutable value: {[cx cy cz] -> #{[x y z]}}. Members are position tuples;
  entries are added when a wireless node block is placed (or rebuilt from
  save) and removed only when the block is broken. Stale entries are harmless:
  discovery resolves the node capability and filters misses."
  (:require [cn.li.ac.foundation.position :as pos]))

(defn create-spatial-index-value
  "Return a fresh spatial-index value: {[cx cy cz] -> #{[x y z]}}."
  []
  {})

(defn add-to-index
  "Add position tuple `[x y z]` to an immutable spatial index value."
  [index [x y z :as p]]
  (update index (pos/pos->chunk-key x y z) (fnil conj #{}) p))

(defn remove-from-index
  "Remove position tuple `[x y z]` from an immutable spatial index value.
   Removes the chunk bucket entirely when it becomes empty."
  [index [x y z :as p]]
  (if (or (nil? x) (nil? y) (nil? z))
    index
    (let [chunk-key (pos/pos->chunk-key x y z)]
      (if-let [chunk-set (get index chunk-key)]
        (let [new-set (disj chunk-set p)]
          (if (empty? new-set)
            (dissoc index chunk-key)
            (assoc index chunk-key new-set)))
        index))))

(defn nearby-chunk-keys
  "Return the chunk keys whose bounding boxes overlap a sphere of
  `search-radius` centered at (x, y, z)."
  [x y z search-radius]
  (pos/nearby-chunk-keys x y z search-radius))

(defn positions-in-index
  "Return the union of all position tuples in `chunk-keys` from an index value."
  [index chunk-keys]
  (reduce (fn [acc chunk-key]
            (if-let [ps (get index chunk-key)]
              (into acc ps)
              acc))
          #{}
          chunk-keys))
