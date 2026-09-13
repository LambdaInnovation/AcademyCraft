(ns cn.li.ac.block.imag-fusor.config
  "Imaginary Fusor configuration - AcademyCraft parity values"
  (:require [cn.li.ac.config.modid :as modid]))

;; Energy consumption
(def energy-per-tick
  "Energy consumed per tick while crafting (original: 12 IF/t)."
  12.0)

(def max-energy
  "Maximum energy storage (original receiver base: 2000 IF)."
  2000.0)

;; Crafting
(def craft-time-ticks
  "Craft time in ticks (original WORK_SPEED = 1/120)."
  120)


;; Inventory slots
(def input-slot-count
  "Number of crystal input slots."
  1)

(def output-slot-count
  "Number of crystal output slots."
  1)

(def imag-input-slot-index
  "Matter unit input slot index (phase liquid units)."
  2)

(def energy-slot-index
  "Index of energy item slot."
  3)

(def imag-output-slot-index
  "Matter unit output slot index (empty units)."
  4)

(def total-slots
  "Total container slots for Imag Fusor."
  5)

(def tank-size
  "Internal phase liquid tank size in mB."
  8000)

(def liquid-per-unit
  "Liquid generated when consuming one phase-liquid matter unit."
  1000)

(def check-interval
  "Ticks between recipe re-checks when idle."
  10)

(def matter-unit-item-id
  "Registry id of matter unit item used by old AcademyCraft logic."
  (modid/namespaced-path "matter_unit"))

(def matter-unit-none-meta
  "MatterUnit damage/meta for MAT_NONE in original AC."
  0)

(def matter-unit-phase-liquid-meta
  "MatterUnit damage/meta for MAT_PHASE_LIQUID in original AC."
  1)

;; Wireless receiver (original AcademyCraft TileReceiverBase uses LATENCY_MK1 = 50)
(def receiver-bandwidth
  "Wireless receiver bandwidth (IF/tick). Original LATENCY_MK1 = 50."
  50.0)

;; Sync interval
(def sync-interval
  "Ticks between GUI sync broadcasts"
  5)
