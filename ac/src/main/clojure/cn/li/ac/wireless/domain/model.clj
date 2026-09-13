(ns cn.li.ac.wireless.domain.model
  "Pure wireless domain model helpers.

  These helpers operate on immutable maps and contain no world/capability side
  effects. Runtime/service layers are responsible for persistence and effects."
  (:require [cn.li.ac.wireless.core.vblock :as vb]))

(defn network-has-capacity?
  [network capacity]
  (< (count (or (:nodes network) [])) (long capacity)))

(defn connection-load
  [connection]
  (+ (count (or (:receivers connection) []))
     (count (or (:generators connection) []))))

(defn connection-has-capacity?
  [connection capacity]
  (< (connection-load connection) (long capacity)))

(defn matrix-in-range?
  [matrix-vb node-vb matrix-range]
  (let [dist-sq (vb/dist-sq node-vb matrix-vb)]
    (<= dist-sq (* matrix-range matrix-range))))
