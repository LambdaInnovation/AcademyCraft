(ns cn.li.ac.block.wind-gen.config
  "Wind Generator configuration aligned with AcademyCraft 1.12 behavior."
  (:require [cn.li.ac.config.common :as config-common]))

;; Structure
(def default-min-pillars
  "Minimum pillar count required for a complete wind generator tower."
  8)

(def default-max-pillars
  "Maximum pillar count allowed for a valid wind generator tower."
  40)

;; Generation
(def default-max-generation-speed
  "Maximum IF/t at full altitude factor."
  15.0)

(def default-base-height
  "Altitude baseline used by the original height curve."
  70.0)

(def default-height-range
  "Altitude range over which factor lerps from 0.5 to 1.0."
  90.0)

(def default-min-height-factor
  "Lower clamp of altitude generation factor."
  0.5)

(def default-max-height-factor
  "Upper clamp of altitude generation factor."
  1.0)

;; Energy storage
(def default-max-energy-base
  "Base internal buffer (same as original TileWindGenBase)."
  20000.0)

(def default-structure-update-interval
  "Ticks between structure scans (original: 10)."
  10)

;; Sync interval
(def default-sync-interval
  "Ticks between GUI sync broadcasts"
  20)

(def descriptors
  [{:key :wind-min-pillars
    :section :generators.wind.structure
    :path "generators.wind.structure.min-pillars"
    :type :int
    :default default-min-pillars
    :min 1
    :max 256
    :comment "Wind generator minimum pillar count required for a complete tower."}
   {:key :wind-max-pillars
    :section :generators.wind.structure
    :path "generators.wind.structure.max-pillars"
    :type :int
    :default default-max-pillars
    :min 1
    :max 512
    :comment "Wind generator maximum pillar count scanned for a valid tower."}
   {:key :wind-max-generation-speed
    :section :generators.wind.generation
    :path "generators.wind.generation.max-speed"
    :type :double
    :default default-max-generation-speed
    :min 0.0
    :max 1000000.0
    :comment "Wind generator maximum output in IF/t at full altitude factor."}
   {:key :wind-base-height
    :section :generators.wind.generation
    :path "generators.wind.generation.base-height"
    :type :double
    :default default-base-height
    :min -2048.0
    :max 4096.0
    :comment "Wind generator altitude baseline for the height multiplier curve."}
   {:key :wind-height-range
    :section :generators.wind.generation
    :path "generators.wind.generation.height-range"
    :type :double
    :default default-height-range
    :min 1.0
    :max 4096.0
    :comment "Wind generator altitude range over which output lerps from min to max factor."}
   {:key :wind-min-height-factor
    :section :generators.wind.generation
    :path "generators.wind.generation.min-height-factor"
    :type :double
    :default default-min-height-factor
    :min 0.0
    :max 100.0
    :comment "Wind generator lower clamp of altitude generation factor."}
   {:key :wind-max-height-factor
    :section :generators.wind.generation
    :path "generators.wind.generation.max-height-factor"
    :type :double
    :default default-max-height-factor
    :min 0.0
    :max 100.0
    :comment "Wind generator upper clamp of altitude generation factor."}
   {:key :wind-max-energy-base
    :section :generators.wind.energy
    :path "generators.wind.energy.max-energy"
    :type :double
    :default default-max-energy-base
    :min 0.0
    :max 100000000.0
    :comment "Wind generator base internal energy buffer in IF."}
   {:key :wind-structure-update-interval
    :section :generators.wind.performance
    :path "generators.wind.performance.structure-update-interval"
    :type :int
    :default default-structure-update-interval
    :min 1
    :max 1200
    :comment "Ticks between wind generator structure scans."}
   {:key :wind-sync-interval
    :section :generators.wind.performance
    :path "generators.wind.performance.sync-interval"
    :type :int
    :default default-sync-interval
    :min 1
    :max 1200
    :comment "Ticks between wind generator GUI sync broadcasts."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/wireless-devices-config)))

(defn min-pillars [] (:wind-min-pillars (cfg)))
(defn max-pillars [] (:wind-max-pillars (cfg)))
(defn max-generation-speed [] (:wind-max-generation-speed (cfg)))
(defn base-height [] (:wind-base-height (cfg)))
(defn height-range [] (:wind-height-range (cfg)))
(defn min-height-factor [] (:wind-min-height-factor (cfg)))
(defn max-height-factor [] (:wind-max-height-factor (cfg)))
(defn max-energy-base [] (:wind-max-energy-base (cfg)))
(defn structure-update-interval [] (:wind-structure-update-interval (cfg)))
(defn sync-interval [] (:wind-sync-interval (cfg)))

(defn calculate-height-multiplier
  "Original height curve: lerp(0.5, 1.0, clamp((y-70)/90, 0, 1))."
  [y-level]
  (let [ratio (/ (- (double y-level) (base-height)) (height-range))
        clamped (-> ratio (max 0.0) (min 1.0))]
    (+ (min-height-factor)
       (* (- (max-height-factor) (min-height-factor)) clamped))))

(defn calculate-generation-rate
  "Calculate IF/t output from main block altitude."
  [y-level]
  (* (max-generation-speed)
     (calculate-height-multiplier y-level)))
