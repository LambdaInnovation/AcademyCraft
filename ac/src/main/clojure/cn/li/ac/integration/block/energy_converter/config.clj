(ns cn.li.ac.integration.block.energy-converter.config
  "Config for upstream-style 4 converter blocks."
  (:require [cn.li.ac.config.common :as config-common]))

(def default-energy-capacity 2000.0)
(def default-transfer-bandwidth 100.0)

;; Upstream parity: 1 IF = 4 RF
(def default-rf-conversion-ratio 4.0)
(def default-eu-conversion-ratio 1.0)

(def descriptors
  [{:key :energy-converter-capacity
    :section :devices.energy-converter.energy
    :path "devices.energy-converter.energy.capacity"
    :type :double
    :default default-energy-capacity
    :min 0.0
    :max 100000000.0
    :comment "Energy Converter internal energy capacity in IF."}
   {:key :energy-converter-transfer-bandwidth
    :section :devices.energy-converter.wireless
    :path "devices.energy-converter.wireless.transfer-bandwidth"
    :type :double
    :default default-transfer-bandwidth
    :min 0.0
    :max 100000000.0
    :comment "Energy Converter wireless transfer bandwidth in IF/t."}
   {:key :energy-converter-rf-ratio
    :section :devices.energy-converter.conversion
    :path "devices.energy-converter.conversion.rf-ratio"
    :type :double
    :default default-rf-conversion-ratio
    :min 0.0
    :max 1000000.0
    :comment "Energy Converter RF conversion ratio: 1 IF equals this many RF."}
   {:key :energy-converter-eu-ratio
    :section :devices.energy-converter.conversion
    :path "devices.energy-converter.conversion.eu-ratio"
    :type :double
    :default default-eu-conversion-ratio
    :min 0.0
    :max 1000000.0
    :comment "Energy Converter EU conversion ratio: 1 IF equals this many EU."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/wireless-devices-config)))

(defn energy-capacity [] (:energy-converter-capacity (cfg)))
(defn transfer-bandwidth [] (:energy-converter-transfer-bandwidth (cfg)))
(defn rf-conversion-ratio [] (:energy-converter-rf-ratio (cfg)))
(defn eu-conversion-ratio [] (:energy-converter-eu-ratio (cfg)))

(def supported-blocks
  #{"rf-input" "rf-output" "eu-input" "eu-output"})

(defn input-block?
  [block-id]
  (contains? #{"rf-input" "eu-input"} (str block-id)))

(defn output-block?
  [block-id]
  (contains? #{"rf-output" "eu-output"} (str block-id)))

(defn conversion-ratio
  [block-id]
  (case (str block-id)
    "rf-input" (rf-conversion-ratio)
    "rf-output" (rf-conversion-ratio)
    "eu-input" (eu-conversion-ratio)
    "eu-output" (eu-conversion-ratio)
    1.0))