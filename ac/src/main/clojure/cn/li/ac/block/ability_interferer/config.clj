(ns cn.li.ac.block.ability-interferer.config
  "Ability Interferer configuration"
  (:require [cn.li.ac.config.common :as config-common]))

;; Range settings
(def default-min-range 10.0) ; Minimum interference range (blocks)

(def default-max-range 100.0) ; Maximum interference range (blocks)

(def default-default-range 10.0) ; Default interference range (blocks)

;; Energy / transfer (mirrors classic interferer behavior)
(def default-battery-pull-per-tick 50.0) ; Maximum IF pulled from battery each tick

(def default-max-energy 10000.0) ; Maximum energy storage

;; Operation
(def default-check-interval 10) ; Ticks between player detection checks

(def default-sync-interval 20) ; Ticks between GUI sync broadcasts

(def default-energy-cost-per-range-squared 1.0)

(def descriptors
  [{:key :ability-interferer-min-range
    :section :devices.ability-interferer.range
    :path "devices.ability-interferer.range.min"
    :type :double
    :default default-min-range
    :min 0.0
    :max 100000.0
    :comment "Ability Interferer minimum configurable range in blocks."}
   {:key :ability-interferer-max-range
    :section :devices.ability-interferer.range
    :path "devices.ability-interferer.range.max"
    :type :double
    :default default-max-range
    :min 0.0
    :max 100000.0
    :comment "Ability Interferer maximum configurable range in blocks."}
   {:key :ability-interferer-default-range
    :section :devices.ability-interferer.range
    :path "devices.ability-interferer.range.default"
    :type :double
    :default default-default-range
    :min 0.0
    :max 100000.0
    :comment "Ability Interferer default range in blocks."}
   {:key :ability-interferer-battery-pull-per-tick
    :section :devices.ability-interferer.energy
    :path "devices.ability-interferer.energy.battery-pull-per-tick"
    :type :double
    :default default-battery-pull-per-tick
    :min 0.0
    :max 100000000.0
    :comment "Ability Interferer maximum IF pulled from the battery slot each tick."}
   {:key :ability-interferer-max-energy
    :section :devices.ability-interferer.energy
    :path "devices.ability-interferer.energy.max-energy"
    :type :double
    :default default-max-energy
    :min 0.0
    :max 100000000.0
    :comment "Ability Interferer internal energy buffer in IF."}
   {:key :ability-interferer-energy-cost-per-range-squared
    :section :devices.ability-interferer.energy
    :path "devices.ability-interferer.energy.cost-per-range-squared"
    :type :double
    :default default-energy-cost-per-range-squared
    :min 0.0
    :max 1000000.0
    :comment "Ability Interferer IF cost multiplier for each enabled check: range^2 * this value."}
   {:key :ability-interferer-check-interval
    :section :devices.ability-interferer.performance
    :path "devices.ability-interferer.performance.check-interval"
    :type :int
    :default default-check-interval
    :min 1
    :max 1200
    :comment "Ticks between Ability Interferer player detection checks."}
   {:key :ability-interferer-sync-interval
    :section :devices.ability-interferer.performance
    :path "devices.ability-interferer.performance.sync-interval"
    :type :int
    :default default-sync-interval
    :min 1
    :max 1200
    :comment "Ticks between Ability Interferer GUI sync broadcasts."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/ability-devices-config)))

(defn min-range [] (:ability-interferer-min-range (cfg)))
(defn max-range [] (:ability-interferer-max-range (cfg)))
(defn default-range [] (:ability-interferer-default-range (cfg)))
(defn battery-pull-per-tick [] (:ability-interferer-battery-pull-per-tick (cfg)))
(defn max-energy [] (:ability-interferer-max-energy (cfg)))
(defn energy-cost-per-range-squared [] (:ability-interferer-energy-cost-per-range-squared (cfg)))
(defn check-interval [] (:ability-interferer-check-interval (cfg)))
(defn sync-interval [] (:ability-interferer-sync-interval (cfg)))

(defn calculate-energy-cost
  [range]
  (let [r (double range)]
    (* r r (double (energy-cost-per-range-squared)))))

(defn clamp-range
  [v]
  (let [r (double (or v (default-range)))]
    (-> r
        (max (min-range))
        (min (max-range)))))
