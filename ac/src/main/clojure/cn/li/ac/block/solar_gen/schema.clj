(ns cn.li.ac.block.solar-gen.schema
  "Solar Generator unified field schema - PURE DATA ONLY.

  Schema is organized by MINECRAFT BLOCK CONCEPTS:
  1. NBT-PERSISTED: Fields saved to disk (server-side state)
  2. GUI-CONTAINER: Client-side GUI container atoms
  3. EPHEMERAL-FIELDS: Runtime state, not persisted

  CRITICAL: This file contains PURE DATA ONLY (no function definitions).
  It can be safely imported by both server-side (block.clj) and client-side (gui.clj) code."
  (:require [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.block.inventory-helpers :as inv-helpers]
            [cn.li.ac.config.nbt-keys :as nbt-keys]
            [cn.li.ac.block.solar-gen.config :as solar-config]))

;; ============================================================================
;; 1. NBT-PERSISTED FIELDS
;; ============================================================================
;; Fields saved to NBT (disk persistence).
;; Used by: block.clj (server-side)

(def nbt-persisted-fields
  [;; Energy storage
   {:key :energy
    :nbt-key (nbt-keys/get-key :energy)
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0.0
    :doc "Current energy stored"}

   ;; Inventory
   {:key :inventory
    :nbt-key (nbt-keys/get-key :battery)
    :type :inventory
    :default [nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory
    :doc "Battery item slot"}])

;; ============================================================================
;; 2. GUI-CONTAINER FIELDS
;; ============================================================================
;; Fields that exist in GUI container (client-side atoms).
;; Used by: gui.clj (client-side)

(def gui-container-fields
  [;; Synced from server
   {:key :energy
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0.0
    :doc "Current energy (synced from server)"}

   ;; Derived/computed fields
   {:key :max-energy
    :gui-only? true
    :gui-init (fn [s] (double (get s :max-energy (solar-config/max-energy))))
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0.0
    :doc "Max energy capacity (constant for solar gen)"}

   {:key :status
    :gui-only? true
    :gui-init (fn [s] (str (get s :status "STOPPED")))
    :gui-sync? true
    :gui-coerce str
    :gui-data-slot? true
    :gui-data-slot-status-codes ["STOPPED" "WEAK" "STRONG"]
    :gui-close-reset ""
    :doc "Generation status (STOPPED/WEAK/STRONG)"}

   {:key :gen-speed
    :gui-only? true
    :gui-init (fn [s] (double (get s :gen-speed 0.0)))
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0.0
    :doc "Current generation rate (IF/T)"}

   {:key :tab-index
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? false
    :gui-data-slot? false
    :gui-coerce int
    :gui-close-reset 0
    :doc "Current GUI tab index (client-only)"}

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

(def ephemeral-fields
  [{:key :status
    :type :string
    :default "STOPPED"
    :persist? false
    :gui-sync? true
    :gui-coerce str
    :gui-data-slot? true
    :gui-data-slot-status-codes ["STOPPED" "WEAK" "STRONG"]
    :doc "Generation status (server-side, synced to GUI)"}

   {:key :gen-speed
    :type :double
    :default 0.0
    :persist? false
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :doc "Current generation rate (server-side, synced to GUI)"}])

;; ============================================================================
;; UNIFIED SCHEMA
;; ============================================================================
;; Merge all field groups, deduplicating by :key.

(def unified-solar-schema
  (state-schema/merge-field-definitions
    [nbt-persisted-fields
     gui-container-fields
     ephemeral-fields]))

;; ============================================================================
;; SCHEMA GROUPS (for documentation)
;; ============================================================================

(def schema-groups
  {:nbt-persisted (count nbt-persisted-fields)      ; 2 fields
   :gui-container (count gui-container-fields)      ; 6 fields
   :ephemeral (count ephemeral-fields)              ; 2 fields
   :total-unique (count unified-solar-schema)})     ; 8 unique fields
