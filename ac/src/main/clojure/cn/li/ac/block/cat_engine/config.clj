(ns cn.li.ac.block.cat-engine.config
  "Cat Engine configuration aligned to AcademyCraft 1.12 semantics."
  (:require [cn.li.ac.config.common :as config-common]))

(def default-max-energy
  "Internal energy buffer upper bound. Mirrors TileGeneratorBase ctor (max=2000)."
  2000.0)

(def default-generation-per-tick
  "Energy generated each server tick before transmission."
  500.0)

(def default-generator-bandwidth
  "Wireless generator bandwidth. Matches upstream TileCatEngine(..., 200) = 200 IF/t."
  200.0)

(def descriptors
  [{:key :cat-engine-max-energy
    :section :generators.cat-engine.energy
    :path "generators.cat-engine.energy.max-energy"
    :type :double
    :default default-max-energy
    :min 0.0
    :max 100000000.0
    :comment "Cat Engine internal energy buffer in IF."}
   {:key :cat-engine-generation-per-tick
    :section :generators.cat-engine.generation
    :path "generators.cat-engine.generation.per-tick"
    :type :double
    :default default-generation-per-tick
    :min 0.0
    :max 1000000.0
    :comment "Cat Engine energy generated each server tick before wireless transmission."}
   {:key :cat-engine-generator-bandwidth
    :section :generators.cat-engine.generation
    :path "generators.cat-engine.generation.bandwidth"
    :type :double
    :default default-generator-bandwidth
    :min 0.0
    :max 1000000.0
    :comment "Cat Engine wireless generator bandwidth in IF/t."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/wireless-devices-config)))

(defn max-energy [] (:cat-engine-max-energy (cfg)))
(defn generation-per-tick [] (:cat-engine-generation-per-tick (cfg)))
(defn generator-bandwidth [] (:cat-engine-generator-bandwidth (cfg)))
