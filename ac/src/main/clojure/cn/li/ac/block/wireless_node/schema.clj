(ns cn.li.ac.block.wireless-node.schema
  "Wireless Node unified field schema.

  Schema is organized by MINECRAFT BLOCK CONCEPTS (reusable patterns):
  1. NBT-PERSISTED: Fields saved to disk (server-side state)
  2. BLOCKSTATE-PROPERTIES: Visual block state in world
  3. GUI-CONTAINER: Client-side GUI container atoms
  4. NETWORK-EDITABLE: Fields editable via network messages
  5. DERIVED-FIELDS: Client-side computed/queried fields
  6. EPHEMERAL-FIELDS: Runtime state, not persisted

  This organization makes the schema pattern reusable for other blocks.

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
  [;; Basic properties
   {:key :node-type
    :nbt-key (nbt-keys/get-key :node-type)
    :type :keyword
    :default :basic
    :persist? true
    :gui-sync? true
    :gui-data-slot-enum {:basic 0 :standard 1 :advanced 2}
    :gui-coerce keyword
    :gui-close-reset :basic
    :doc "Node tier: :basic, :standard, or :advanced"}

   {:key :placer-name
    :nbt-key (nbt-keys/get-key :placer)
    :type :string
    :default ""
    :persist? true
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "Player who placed this block (owner)"}

   {:key :placer-uuid
    :nbt-key (nbt-keys/get-key :placer-uuid)
    :type :string
    :default ""
    :persist? true
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "UUID of the player who placed this block"}

   ;; Energy storage
   {:key :energy
    :nbt-key (nbt-keys/get-key :energy)
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0
    :doc "Current energy stored"}

   ;; Network configuration
   {:key :node-name
    :nbt-key (nbt-keys/get-key :node-name)
    :type :string
    :default "Unnamed"
    :persist? true
    :gui-sync? true
    :gui-data-slot? false
    :gui-container-key :ssid
    :gui-payload-key :node-name
    :gui-close-reset ""
    :doc "Network SSID"}

   {:key :password
    :nbt-key (nbt-keys/get-key :password)
    :type :string
    :default ""
    :persist? true
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "Network password"}

   {:key :enabled
    :nbt-key (nbt-keys/get-key :enabled)
    :type :boolean
    :default false
    :persist? true
    :gui-sync? true
    :gui-coerce boolean
    :gui-container-key :is-online
    :gui-payload-key :enabled
    :gui-close-reset false
    :doc "Connection enabled state"}

   ;; Inventory
   {:key :inventory
    :nbt-key (nbt-keys/get-key :node-inventory)
    :type :inventory
    :default [nil nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory
    :doc "Item inventory slots"}])

;; ============================================================================
;; 2. BLOCKSTATE-PROPERTIES FIELDS
;; ============================================================================
;; Fields that affect block visual appearance in world.
;; Used by: block.clj (server-side), blockstate.clj (datagen)

(def blockstate-property-fields
  [{:key :energy
    :block-state {:prop "energy"
                  :type :integer
                  :min 0 :max 4 :default 0
                  :xf 'cn.li.ac.block.wireless-node.logic/energy->blockstate-level}
    :doc "Energy level (0-4) displayed as visual bar"}

   {:key :enabled
    :block-state {:prop "connected"
                  :type :boolean
                  :default false}
    :doc "Connection state (shows glow effect when true)"}])

;; ============================================================================
;; 3. GUI-CONTAINER FIELDS
;; ============================================================================
;; Fields that exist in GUI container (client-side atoms).
;; Used by: gui.clj (client-side)

(def gui-container-fields
  [;; Synced from server
   {:key :energy
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :gui-close-reset 0
    :doc "Current energy (synced from server)"}

   {:key :node-type
    :gui-sync? true
    :gui-data-slot-enum {:basic 0 :standard 1 :advanced 2}
    :gui-coerce keyword
    :gui-close-reset :basic
    :doc "Node tier (synced from server)"}

   {:key :placer-name
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "Owner name (synced from server)"}

   {:key :placer-uuid
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "Owner UUID (synced from server)"}

   {:key :node-name
    :gui-container-key :ssid
    :gui-payload-key :node-name
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "Network SSID (synced from server)"}

   {:key :password
    :gui-sync? true
    :gui-data-slot? false
    :gui-close-reset ""
    :doc "Network password (synced from server)"}

   {:key :enabled
    :gui-container-key :is-online
    :gui-payload-key :enabled
    :gui-sync? true
    :gui-coerce boolean
    :gui-close-reset false
    :doc "Connection state (synced from server)"}

   {:key :charging-in
    :gui-sync? true
    :gui-coerce boolean
    :gui-close-reset false
    :doc "Charging from input slot (ephemeral, synced from server)"}

   {:key :charging-out
    :gui-sync? true
    :gui-coerce boolean
    :gui-close-reset false
    :doc "Charging to output slot (ephemeral, synced from server)"}

   {:key :tab-index
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? false
    :gui-data-slot? false
    :gui-coerce int
    :gui-close-reset 0
    :doc "Current GUI tab index (client-only)"}])

;; ============================================================================
;; 4. NETWORK-EDITABLE FIELDS
;; ============================================================================
;; Fields that can be edited by player via network messages.
;; Used by: block.clj (server-side network handlers)

(def network-editable-fields
  [{:key :node-name
    :network-editable? true
    :network-msg :change-name
    :gui-payload-key :node-name
    :doc "SSID editable by owner"}

   {:key :password
    :network-editable? true
    :network-msg :change-password
    :doc "Password editable by owner"}])

;; ============================================================================
;; 5. DERIVED-FIELDS (Client-Side)
;; ============================================================================
;; Fields computed or queried on client side.
;; Used by: gui.clj (client-side)

(def derived-fields
  [{:key :max-energy
    :gui-only? true
    :gui-init (fn [s] (int (get s :max-energy 15000)))
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Max energy capacity (derived from node-type)"}

   {:key :transfer-rate
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Current transfer rate (derived from charging flags)"}

   {:key :capacity
    :gui-only? true
    :gui-init (fn [s] (int (get s :capacity 0)))
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Network capacity (synced from server tick)"}

   {:key :max-capacity
    :gui-only? true
    :gui-init (fn [s] (int (get s :max-capacity 0)))
    :gui-sync? true
    :gui-coerce int
    :gui-close-reset 0
    :doc "Max network capacity (synced from server tick)"}

   {:key :charge-ticker
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? false
    :gui-coerce int
    :gui-close-reset 0
    :doc "Animation ticker (client-only)"}

   {:key :sync-ticker
    :gui-only? true
    :gui-init (fn [_] 0)
    :gui-sync? false
    :gui-data-slot? false
    :gui-coerce int
    :gui-close-reset 0
    :doc "Query throttle ticker (client-only)"}])

;; ============================================================================
;; 6. EPHEMERAL-FIELDS (Not Persisted)
;; ============================================================================
;; Fields that exist at runtime but are not saved to NBT.
;; Used by: block.clj (server-side), gui.clj (client-side)

(def ephemeral-fields
  [{:key :charging-in
    :type :boolean
    :default false
    :persist? false
    :gui-sync? true
    :gui-coerce boolean
    :gui-close-reset false
    :doc "Currently charging from input slot (server-side, synced to GUI)"}

   {:key :charging-out
    :type :boolean
    :default false
    :persist? false
    :gui-sync? true
    :gui-coerce boolean
    :gui-close-reset false
    :doc "Currently charging to output slot (server-side, synced to GUI)"}

   ])

;; ============================================================================
;; UNIFIED SCHEMA
;; ============================================================================
;; Merge all field groups, deduplicating by :key.
;; Fields can appear in multiple groups (e.g., :energy in both nbt-persisted and blockstate-property).

(def unified-node-schema
  (state-schema/merge-field-definitions
    [nbt-persisted-fields
     blockstate-property-fields
     gui-container-fields
     network-editable-fields
     derived-fields
     ephemeral-fields]))

;; ============================================================================
;; SCHEMA GROUPS (for documentation)
;; ============================================================================

(def schema-groups
  {:nbt-persisted (count nbt-persisted-fields)           ; 8 fields
   :blockstate-properties (count blockstate-property-fields) ; 2 fields
   :gui-container (count gui-container-fields)           ; 10 fields
   :network-editable (count network-editable-fields)     ; 2 fields
   :derived (count derived-fields)                       ; 6 fields
   :ephemeral (count ephemeral-fields)                   ; 3 fields
   :total-unique (count unified-node-schema)})           ; 18 unique fields

;; ============================================================================
;; USAGE DOCUMENTATION
;; ============================================================================

(def usage-doc
  "Wireless Node Schema Organization

  This schema demonstrates a reusable pattern for Minecraft blocks:

  1. NBT-PERSISTED (8 fields)
     - Saved to disk, loaded on world load
     - Used by: block.clj (server-side NBT load/save)
     - Examples: node-type, placer-name, placer-uuid, energy, node-name, password, enabled, inventory

  2. BLOCKSTATE-PROPERTIES (2 fields)
     - Affect block visual appearance in world
     - Used by: block.clj (update BlockState), blockstate.clj (datagen)
     - Examples: energy (0-4 bar), enabled (glow effect)

  3. GUI-CONTAINER (10 fields)
     - Client-side GUI container atoms
     - Used by: gui.clj (create-container, server-menu-sync!)
     - Examples: energy, node-type, ssid, password, is-online, charging-in/out, tab-index

  4. NETWORK-EDITABLE (2 fields)
     - Player can edit via network messages
     - Used by: block.clj (network message handlers)
     - Examples: node-name (SSID), password

  5. DERIVED-FIELDS (6 fields)
     - Computed or queried on client
     - Used by: gui.clj (GUI display logic)
     - Examples: max-energy (from node-type), transfer-rate (from charging flags), capacity (queried)

  6. EPHEMERAL-FIELDS (3 fields)
     - Runtime state, not persisted
     - Used by: block.clj (tick logic), gui.clj (animations)
     - Examples: charging-in/out

  Fields can appear in multiple groups (e.g., :energy is both NBT-PERSISTED and BLOCKSTATE-PROPERTY).
  The unified schema merges all definitions by :key.")

(comment
  ;; Quick reference
  schema-groups
  ;; => {:nbt-persisted 8, :blockstate-properties 2, :gui-container 10,
  ;;     :network-editable 2, :derived 6, :ephemeral 3, :total-unique 18}

  ;; See usage documentation
  (println usage-doc))
