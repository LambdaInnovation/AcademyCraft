(ns cn.li.ac.item.item-energy-base
  "Energy item base definitions — aligns with original AcademyCraft's ItemEnergyBase.

  Defines energy item configurations and type-detection helpers.
  Energy item configs are plain function maps (not protocol records).

  Energy operations (charge, pull, set, get) live in
  cn.li.ac.energy.service.item-manager, aligning with the original's IFItemManager."
  (:require [clojure.string :as str]
            [cn.li.ac.energy.imag-energy-item :as energy-item]
            [cn.li.mcmod.config :as modid]
            [cn.li.mcmod.platform.item :as item]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Energy Item Factory — plain function map
;; ============================================================================

(defn- make-energy-item
  "Create an energy item function map."
  [max-energy bandwidth]
  {:get-max-energy (fn [] max-energy)
   :get-bandwidth (fn [] bandwidth)})

;; ============================================================================
;; Energy Item Configurations
;; ============================================================================

(def energy-item-configs
  ^{:doc "Energy item configurations: name -> [max-energy bandwidth]"}
  {:energy-unit        [10000.0 20.0]
   ;; Upstream ItemDeveloper: 10,000 IF, charge bandwidth 50 IF/t (its 0.3 is
   ;; the developer syncRate, not the item bandwidth).
   :developer-portable [10000.0 50.0]})

(defn create-energy-item
  "Create an energy item spec with specified configuration."
  [config-key]
  (if-let [[max-energy bandwidth] (get energy-item-configs config-key)]
    (make-energy-item max-energy bandwidth)
    (throw (IllegalArgumentException.
             (str "Unknown energy item config: " config-key)))))

;; ============================================================================
;; Type Detection — matches items to their energy item configs
;; ============================================================================

(defn get-energy-item-type
  "Determine the energy item type from an ItemStack.
  Checks NBT batteryType tag first, then falls back to registry name matching."
  [item-stack]
  (when item-stack
    (let [item-obj (item/object item-stack)
          registry-id (some-> item-obj item/registry-name str)
          expected-energy-id (str modid/mod-id ":energy_unit")
          expected-portable-id (str modid/mod-id ":developer_portable")
          nbt-data (item/custom-data item-stack)
          nbt-type (when nbt-data (sd/get-string nbt-data "batteryType"))]
      (or (when (seq nbt-type)
            ;; Registry battery-type strings use underscores ("energy_unit");
            ;; config keys use kebab-case (:energy-unit) — normalize so the
            ;; NBT path resolves like the registry-name path.
            (keyword (str/replace nbt-type "_" "-")))
          (cond
            (= registry-id expected-energy-id) :energy-unit
            (= registry-id expected-portable-id) :developer-portable
            :else nil)))))

(defn get-energy-item-config
  "Get the energy item config for an ItemStack.
  Returns nil if the item is not a recognized energy item."
  [item-stack]
  (when-let [item-type (get-energy-item-type item-stack)]
    (create-energy-item item-type)))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init-energy-items! []
  (log/debug "Energy item adapters initialized: energy-unit, developer-portable"))
