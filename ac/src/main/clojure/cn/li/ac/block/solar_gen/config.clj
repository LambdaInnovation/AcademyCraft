(ns cn.li.ac.block.solar-gen.config
  (:require [cn.li.ac.config.common :as config-common]))

(def descriptors
  [{:key :solar-max-energy
    :section :solar-gen
    :path "solar-gen.max-energy"
    :type :double
    :default 1000.0
    :min 0.0
    :max 100000000.0
    :comment "Solar generator internal energy capacity."}
   {:key :solar-generation-rate
    :section :solar-gen
    :path "solar-gen.generation-rate"
    :type :double
    :default 3.0
    :min 0.0
    :max 1000000.0
    :comment "Solar generator IF/t under full sun."}
   {:key :solar-rain-multiplier
    :section :solar-gen
    :path "solar-gen.rain-multiplier"
    :type :double
    :default 0.2
    :min 0.0
    :max 10.0
    :comment "Solar generator output multiplier while raining."}
   {:key :solar-daytime-threshold-ticks
    :section :solar-gen
    :path "solar-gen.daytime-threshold-ticks"
    :type :int
    :default 12500
    :min 0
    :max 24000
    :comment "Last daytime tick at which solar generators may produce energy."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/wireless-devices-config)))

(defn max-energy []
  (:solar-max-energy (cfg)))

(defn generation-rate []
  (:solar-generation-rate (cfg)))

(defn rain-multiplier []
  (:solar-rain-multiplier (cfg)))

(defn daytime-threshold-ticks []
  (:solar-daytime-threshold-ticks (cfg)))