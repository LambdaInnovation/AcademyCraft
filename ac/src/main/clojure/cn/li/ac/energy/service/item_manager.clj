(ns cn.li.ac.energy.service.item-manager
  "Item energy management service — aligns with original AcademyCraft's IFItemManager.

  Handles all energy operations on items: check support, read/write energy,
  charge, pull, and container projection. Energy item type detection and
  configuration are delegated to cn.li.ac.item.item-energy-base."
  (:require [cn.li.ac.energy.domain.container :as container]
            [cn.li.ac.energy.imag-energy-item :as energy-item]
            [cn.li.ac.item.item-energy-base :as energy-base]
            [cn.li.mcmod.platform.item :as item]
            [cn.li.mcmod.platform.structured-data :as sd]))

;; ============================================================================
;; Support check — aligns with IFItemManager.isSupported()
;; ============================================================================

(defn is-energy-item-supported?
  "Return true when the item stack supports AC energy storage."
  [item-stack]
  (boolean (and item-stack (energy-base/get-energy-item-config item-stack))))

;; ============================================================================
;; Energy read/write — aligns with IFItemManager.getEnergy() / setEnergy()
;; ============================================================================

(defn get-item-energy
  "Read current energy from an item stack."
  [item-stack]
  (if (is-energy-item-supported? item-stack)
    (let [nbt-data (item/custom-data item-stack)]
      (if nbt-data
        (sd/get-double nbt-data "energy")
        0.0))
    0.0))

(defn get-item-capacity
  "Read maximum energy capacity from an item stack."
  [item-stack]
  (if (is-energy-item-supported? item-stack)
    (let [config (energy-base/get-energy-item-config item-stack)]
      (energy-item/get-max-energy config))
    0.0))

(defn get-item-bandwidth
  "Read item transfer bandwidth."
  [item-stack]
  (if (is-energy-item-supported? item-stack)
    (let [config (energy-base/get-energy-item-config item-stack)]
      (energy-item/get-bandwidth config))
    0.0))

(defn set-item-energy!
  "Set energy on an item stack in place.
  Clamps energy to [0, max] and writes all NBT tags (energy, maxEnergy,
  bandwidth, batteryType). Returns nil for unsupported items."
  [item-stack amount]
  (if (is-energy-item-supported? item-stack)
    (let [config (energy-base/get-energy-item-config item-stack)
          max-energy (energy-item/get-max-energy config)
          clamped-energy (min max-energy (max 0.0 amount))
          bandwidth (energy-item/get-bandwidth config)
          battery-type (name (energy-base/get-energy-item-type item-stack))
          tag (item/ensure-custom-data item-stack)]
      (sd/set-double! tag "energy" clamped-energy)
      (sd/set-double! tag "maxEnergy" max-energy)
      (sd/set-double! tag "bandwidth" bandwidth)
      (sd/set-string! tag "batteryType" battery-type)
      (item/set-custom-data! item-stack tag))
    nil))

;; ============================================================================
;; Charge / pull — aligns with IFItemManager.charge() / pull()
;; ============================================================================

(defn charge-energy-to-item
  "Charge an item and return leftover energy that could not be inserted.
  Respects bandwidth unless ignore-bandwidth is true."
  [item-stack amount ignore-bandwidth]
  (if (is-energy-item-supported? item-stack)
    (let [config (energy-base/get-energy-item-config item-stack)
          current (get-item-energy item-stack)
          max-energy (energy-item/get-max-energy config)
          bandwidth (energy-item/get-bandwidth config)

          ;; Calculate how much can be charged
          space (- max-energy current)
          limit (if ignore-bandwidth Double/MAX_VALUE bandwidth)
          to-charge (min amount space limit)
          leftover (- amount to-charge)]

      ;; Apply charge
      (set-item-energy! item-stack (+ current to-charge))
      leftover)
    amount)) ; Return all if not a supported energy item

(defn pull-energy-from-item
  "Pull energy from an item and return the amount actually extracted.
  Respects bandwidth unless ignore-bandwidth is true."
  [item-stack amount ignore-bandwidth]
  (if (is-energy-item-supported? item-stack)
    (let [config (energy-base/get-energy-item-config item-stack)
          current (get-item-energy item-stack)
          bandwidth (energy-item/get-bandwidth config)

          ;; Calculate how much can be pulled
          limit (if ignore-bandwidth Double/MAX_VALUE bandwidth)
          to-pull (min amount current limit)]

      ;; Apply pull
      (set-item-energy! item-stack (- current to-pull))
      to-pull)
    0.0)) ; Return 0 if not a supported energy item

;; ============================================================================
;; Container projection — immutable model for energy system
;; ============================================================================

(defn item->container
  "Project an item stack into the immutable EnergyContainer model."
  [item-stack]
  (when (is-energy-item-supported? item-stack)
    (container/energy-container
      (max 1.0 (get-item-capacity item-stack))
      (get-item-energy item-stack)
      (max 1.0 (get-item-bandwidth item-stack))
      1.0)))
