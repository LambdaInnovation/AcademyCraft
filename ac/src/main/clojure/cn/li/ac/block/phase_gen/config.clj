(ns cn.li.ac.block.phase-gen.config
  "Phase Generator configuration aligned with AcademyCraft TilePhaseGen."
  (:require [cn.li.ac.config.common :as config-common]
            [cn.li.ac.config.modid :as modid]))

;; Energy generation parity:
;; CONSUME_PER_TICK = 100 mB, GEN_PER_MB = 0.5 IF
(def default-liquid-consume-per-tick
  "Maximum internal phase-liquid drain per tick (mB)."
  100)

(def default-gen-per-mb
  "Generated energy per drained mB of phase liquid."
  0.5)

;; Energy storage parity:
;; TilePhaseGen -> TileGeneratorBase(..., 6000, ...)
(def default-max-energy
  "Maximum energy storage."
  6000.0)

(def default-bandwidth
  "Per-tick cap on charging energy items from the buffer.
   TilePhaseGen -> TileGeneratorBase(..., IFConstants.LATENCY_MK1 = 50)."
  50.0)

;; Internal tank parity:
;; TANK_SIZE = 8000, PER_UNIT = 1000
(def default-tank-size
  "Internal phase-liquid tank size (mB)."
  8000)

(def default-liquid-per-unit
  "Liquid added when consuming one phase-liquid matter unit."
  1000)

(def descriptors
  [{:key :phase-gen-liquid-consume-per-tick
    :section :generators.phase.generation
    :path "generators.phase.generation.liquid-consume-per-tick"
    :type :int
    :default default-liquid-consume-per-tick
    :min 0
    :max 1000000
    :comment "Phase Generator maximum phase-liquid drain per tick in mB."}
   {:key :phase-gen-energy-per-mb
    :section :generators.phase.generation
    :path "generators.phase.generation.energy-per-mb"
    :type :double
    :default default-gen-per-mb
    :min 0.0
    :max 1000000.0
    :comment "Phase Generator energy generated per drained mB of phase liquid."}
   {:key :phase-gen-max-energy
    :section :generators.phase.energy
    :path "generators.phase.energy.max-energy"
    :type :double
    :default default-max-energy
    :min 0.0
    :max 100000000.0
    :comment "Phase Generator internal energy storage in IF."}
   {:key :phase-gen-tank-size
    :section :generators.phase.liquid
    :path "generators.phase.liquid.tank-size"
    :type :int
    :default default-tank-size
    :min 0
    :max 100000000
    :comment "Phase Generator internal phase-liquid tank size in mB."}
   {:key :phase-gen-liquid-per-unit
    :section :generators.phase.liquid
    :path "generators.phase.liquid.liquid-per-unit"
    :type :int
    :default default-liquid-per-unit
    :min 0
    :max 100000000
    :comment "Phase liquid added when consuming one phase-liquid matter unit."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/wireless-devices-config)))

(defn liquid-consume-per-tick [] (:phase-gen-liquid-consume-per-tick (cfg)))
(defn gen-per-mb [] (:phase-gen-energy-per-mb (cfg)))
(defn max-energy [] (:phase-gen-max-energy (cfg)))
(defn tank-size [] (:phase-gen-tank-size (cfg)))
(defn liquid-per-unit [] (:phase-gen-liquid-per-unit (cfg)))

;; Inventory layout parity (ContainerPhaseGen):
(def liquid-in-slot 0)
(def liquid-out-slot 1)
(def output-slot 2)
(def total-slots 3)

;; Matter unit mapping reused by parity logic.
(def matter-unit-item-id (modid/namespaced-path "matter_unit"))
(def matter-unit-none-meta 0)
(def matter-unit-phase-liquid-meta 1)
