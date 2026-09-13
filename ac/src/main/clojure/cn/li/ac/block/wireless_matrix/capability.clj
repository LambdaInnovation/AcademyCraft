(ns cn.li.ac.block.wireless-matrix.capability
  "Wireless matrix Java/Clojure capability implementations."
  (:require [cn.li.ac.block.wireless-matrix.stats :as stats]
            [cn.li.ac.block.wireless-matrix.schema :as matrix-schema]
            [cn.li.mcmod.block.state-schema :as schema]
            [cn.li.mcmod.platform.position :as pos])
  (:import [cn.li.acapi.wireless IWirelessMatrix]))

(defn matrix-stats-for-counts
  [core-level plate-count]
  (stats/stats-for-counts (stats/required-plate-count) core-level plate-count))

(defn- matrix-params [be]
  (let [state (stats/safe-state be)
        core-lv (int (schema/get-field matrix-schema/unified-matrix-schema state :core-level))]
    (matrix-stats-for-counts core-lv (:plate-count state 0))))

(defn- be-str-field [be k]
  (str (schema/get-field matrix-schema/unified-matrix-schema (stats/safe-state be) k)))

(deftype WirelessMatrixImpl [be]
  IWirelessMatrix
  (getMatrixCapacity [_] (:capacity (matrix-params be)))
  (getMatrixBandwidth [_] (:bandwidth (matrix-params be)))
  (getMatrixRange [_] (:range (matrix-params be)))
  (getSsid [_] (be-str-field be :ssid))
  (getPassword [_] (be-str-field be :password))
  (getPlacerName [_] (be-str-field be :placer-name)))

;; Project-owned definterface + deftype: AOT solidifies stable cn.li.* symbols (safe).
;; Do not use definterface/reify for net.minecraft.* — that solidifies Mojmap names → runtime remap failure.
(definterface IMatrixJavaProxy
  (^String getPlacerName [])
  (^long getMatrixCapacity [])
  (^long getMatrixBandwidth [])
  (^double getMatrixRange [])
  (^long getLoad [])
  (^Object getPos []))

(deftype MatrixJavaProxy [be]
  IMatrixJavaProxy
  (getPlacerName [_] (be-str-field be :placer-name))
  (getMatrixCapacity [_] (long (:capacity (matrix-params be))))
  (getMatrixBandwidth [_] (long (:bandwidth (matrix-params be))))
  (getMatrixRange [_] (double (:range (matrix-params be))))
  (getLoad [_] 0)
  (getPos [_] (pos/block-pos be)))
