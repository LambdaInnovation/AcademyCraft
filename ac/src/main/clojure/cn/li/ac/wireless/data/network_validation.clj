(ns cn.li.ac.wireless.data.network-validation
  "Validation and disposal operations for wireless networks."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.network-state :as net-state]
            [cn.li.mcmod.util.log :as log]))

(defn validate!
  "Validate network integrity. Disposes the network when its matrix chunk is
  loaded but the matrix capability is gone (block destroyed).
  Returns true if valid, false if disposed."
  ([network world]
   (validate! network world nil))
  ([network world cap-cache]
   (let [matrix-vb (:matrix network)
         chunk-loaded? (vb/is-chunk-loaded? matrix-vb world)
         cap (resolver/resolve-cap-cached
               cap-cache resolver/resolve-matrix-cap world matrix-vb)]
     (if (and chunk-loaded? (not cap))
       (do
         (log/info (format "[validate] Network '%s' matrix destroyed at %s — disposing"
                           (net-state/get-ssid network)
                           (vb/vblock-to-string matrix-vb)))
         (net-state/mark-disposed! network)
         false)
       (net-state/active? network)))))

(defn is-in-range?
  "Check if coordinates are in network range."
  [network x y z world]
  (if-let [matrix (net-state/get-matrix network world)]
    (let [range (.getMatrixRange ^cn.li.acapi.wireless.IWirelessMatrix matrix)
          dist-sq (vb/dist-sq-pos (:matrix network) x y z)]
      (<= dist-sq (* range range)))
    false))

(defn dispose!
  "Dispose the network."
  [network _world]
  (net-state/mark-disposed! network)
  (log/info (format "Network '%s' disposed" (net-state/get-ssid network))))
