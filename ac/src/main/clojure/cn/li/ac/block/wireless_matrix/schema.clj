(ns cn.li.ac.block.wireless-matrix.schema
  "Wireless Matrix unified field schema - PURE DATA ONLY.

  Schema is organized by MINECRAFT BLOCK CONCEPTS:
  1. NBT-PERSISTED: Fields saved to disk (server-side state)
  2. GUI-CONTAINER: Client-side GUI container atoms
  3. EPHEMERAL-FIELDS: Runtime state, not persisted

  CRITICAL: This file contains PURE DATA ONLY (no function definitions).
  It can be safely imported by both server-side (block.clj) and client-side (gui.clj) code."
  (:require [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.block.inventory-helpers :as inv-helpers]
            [cn.li.ac.config.nbt-keys :as nbt-keys]))

;; ============================================================================
;; 1. NBT-PERSISTED FIELDS
;; ============================================================================
;; Fields saved to NBT (disk persistence).
;; Used by: block.clj (server-side)

(def nbt-persisted-fields
  [;; Identity
   {:key :placer-name
    :nbt-key (nbt-keys/get-key :placer)
    :type :string
    :default ""
    :persist? true
    :gui-sync? true
    :gui-data-slot? false
    :doc "Player who placed this block (owner)"}

   ;; Installed components
   {:key :plate-count
    :nbt-key (nbt-keys/get-key :plate-count)
    :type :int
    :default 0
    :persist? true
    :gui-sync? true
    :doc "Number of constraint plates installed (0-3)"}

   {:key :core-level
    :nbt-key (nbt-keys/get-key :core-level)
    :type :int
    :default 0
    :persist? true
    :gui-sync? true
    :doc "Material core level (0-3)"}

   ;; Multiblock structure
   {:key :direction
    :nbt-key (nbt-keys/get-key :direction)
    :type :keyword
    :default :north
    :persist? true
    :gui-sync? false
    :doc "Multiblock facing direction"}

   {:key :sub-id
    :nbt-key (nbt-keys/get-key :sub-id)
    :type :int
    :default 0
    :persist? true
    :gui-sync? false
    :doc "Multiblock part ID (0=controller, 1-7=parts)"}

   {:key :controller-pos-x
    :nbt-key (nbt-keys/get-key :controller-pos-x)
    :type :int
    :default 0
    :persist? true
    :gui-sync? false
    :doc "Controller X position"}

   {:key :controller-pos-y
    :nbt-key (nbt-keys/get-key :controller-pos-y)
    :type :int
    :default 0
    :persist? true
    :gui-sync? false
    :doc "Controller Y position"}

   {:key :controller-pos-z
    :nbt-key (nbt-keys/get-key :controller-pos-z)
    :type :int
    :default 0
    :persist? true
    :gui-sync? false
    :doc "Controller Z position"}

   ;; Inventory (custom load/save for ItemStack handling)
   {:key :inventory
    :nbt-key (nbt-keys/get-key :matrix-inventory)
    :type :inventory
    :default [nil nil nil nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory
    :doc "Item inventory slots (3 plates + 1 core)"}])

;; ============================================================================
;; 2. GUI-CONTAINER FIELDS
;; ============================================================================
;; Fields that exist in GUI container (client-side atoms).
;; Used by: gui.clj (client-side)

(def gui-container-fields
  [;; Synced from server
   {:key :core-level
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Core level (synced from server)"}

   {:key :plate-count
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Plate count (synced from server)"}

   ;; Derived/computed fields
   {:key :is-working
    :gui-only? true
    :gui-init (fn [_] false)
    :gui-sync? true
    :gui-coerce boolean
    :gui-close-reset false
    :doc "Matrix operational state (derived from core-level and plate-count)"}

   {:key :capacity
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Current network device count (queried from server)"}

   {:key :max-capacity
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Max network capacity (derived from core-level and plate-count)"}

   {:key :bandwidth
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? true
    :gui-coerce long
    :gui-close-reset 0
    :doc "Network bandwidth (derived from core-level and plate-count)"}

   {:key :range
    :gui-only? true
    :gui-init (fn [_] 0.0)
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0.0
    :doc "Network range (derived from core-level and plate-count)"}

   {:key :sync-ticker
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? false
    :gui-data-slot? false
    :gui-coerce int
    :gui-close-reset 0
    :doc "Query throttle ticker (client-only)"}])

;; ============================================================================
;; 3. EPHEMERAL-FIELDS (Not Persisted)
;; ============================================================================
;; Fields that exist at runtime but are not saved to NBT.
;; Used by: block.clj (server-side)

(def ephemeral-fields [])

;; ============================================================================
;; UNIFIED SCHEMA
;; ============================================================================
;; Merge all field groups, deduplicating by :key.

(def unified-matrix-schema
  (state-schema/merge-field-definitions
    [nbt-persisted-fields
     gui-container-fields
     ephemeral-fields]))

;; ============================================================================
;; SCHEMA GROUPS (for documentation)
;; ============================================================================

(def schema-groups
  {:nbt-persisted (count nbt-persisted-fields)      ; 9 fields
   :gui-container (count gui-container-fields)      ; 8 fields
   :ephemeral (count ephemeral-fields)              ; 1 field
   :total-unique (count unified-matrix-schema)})    ; 18 unique fields
