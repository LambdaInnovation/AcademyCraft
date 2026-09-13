(ns cn.li.ac.test.support.wireless-stubs
  (:require [cn.li.ac.test.support.nbt :as test-nbt]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.world :as pworld])
  (:import [cn.li.acapi.wireless IWirelessGenerator IWirelessMatrix IWirelessNode IWirelessReceiver]))

(deftype TestPos [^long x ^long y ^long z])

(defn- test-pos-x [p]
  (cond
    (instance? TestPos p) (.x ^TestPos p)
    (map? p) (long (:x p 0))
    :else 0))

(defn- test-pos-y [p]
  (cond
    (instance? TestPos p) (.y ^TestPos p)
    (map? p) (long (:y p 0))
    :else 0))

(defn- test-pos-z [p]
  (cond
    (instance? TestPos p) (.z ^TestPos p)
    (map? p) (long (:z p 0))
    :else 0))

(defn fake-matrix
  ([] (fake-matrix {}))
  ([{:keys [capacity bandwidth matrix-range]
     :or {capacity 64 bandwidth 128.0 matrix-range 16.0}}]
   (reify IWirelessMatrix
     (getMatrixCapacity [_] capacity)
     (getMatrixBandwidth [_] bandwidth)
     (getMatrixRange [_] matrix-range)
     (getSsid [_] "ssid-a")
     (getPassword [_] "pw")
     (getPlacerName [_] "tester"))))

(defn mutable-node
  [{:keys [energy max-energy bandwidth capacity range password]
    :or {energy 0.0 max-energy 1000.0 bandwidth 100.0 capacity 8 range 10.0 password "pw"}}]
  (let [e (atom energy)]
    (reify IWirelessNode
      (getEnergy [_] @e)
      (setEnergy [_ v] (reset! e v))
      (getMaxEnergy [_] max-energy)
      (getBandwidth [_] bandwidth)
      (getCapacity [_] capacity)
      (getRange [_] range)
      (getNodeName [_] "node-test")
      (getPassword [_] password)
      (getBlockPos [_] nil))))

(defn fake-node [password]
  (mutable-node {:password password}))

(defn generator-stub
  [{:keys [bandwidth provided-fn]
    :or {bandwidth 100.0
         provided-fn (fn [_required] 0.0)}}]
  (reify IWirelessGenerator
    (getEnergy [_] 0.0)
    (setEnergy [_ _] nil)
    (getProvidedEnergy [_ required] (double (provided-fn required)))
    (getGeneratorBandwidth [_] bandwidth)))

(defn receiver-stub
  [{:keys [bandwidth required leftover-fn]
    :or {bandwidth 100.0 required 50.0 leftover-fn (constantly 0.0)}}]
  (reify IWirelessReceiver
    (getRequiredEnergy [_] (double required))
    (injectEnergy [_ give] (double (leftover-fn give)))
    (pullEnergy [_ _] 0.0)
    (getReceiverBandwidth [_] (double bandwidth))))

(defn fake-generator []
  (generator-stub {}))

(defn fake-receiver []
  (receiver-stub {:required 0.0}))

(defn with-tile-world
  "Install platform world/position/nbt stubs so tile entities resolve from `tiles-atom` keyed by [x y z] (ints)."
  [tiles-atom f]
  (test-nbt/install-test-structured-data-ops!)
  (pos/install-position-ops!
    {:pos-x test-pos-x
     :pos-y test-pos-y
     :pos-z test-pos-z
     :pos-above (fn [p]
                  (TestPos. (inc (test-pos-x p)) (test-pos-y p) (test-pos-z p)))
     :create-block-pos (fn [x y z] (TestPos. (long x) (long y) (long z)))
     :position-get-block-pos identity
     :position-get-pos identity}
    "ac-wireless-test")
  (pworld/install-world-ops!
    {:world-is-chunk-loaded? (fn [_ _ _] true)
     :world-get-tile-entity
     (fn [_ pos]
       (let [x (test-pos-x pos)
             y (test-pos-y pos)
             z (test-pos-z pos)]
         (get @tiles-atom [x y z])))}
    "ac-wireless-test")
  (f))
