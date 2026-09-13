(ns cn.li.ac.block.ability-interferer.schema
  "Ability Interferer state schema"
  (:require [cn.li.mcmod.block.inventory-helpers :as inv-helpers]))

(defonce ability-interferer-schema
  [{:key :energy
    :nbt-key "Energy"
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :max-energy
    :nbt-key "MaxEnergy"
    :type :double
    :default 10000.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :range
    :nbt-key "Range"
    :type :double
    :default 10.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100
    :network-editable? true
    :network-msg :change-range}

   {:key :enabled
    :nbt-key "Enabled"
    :type :boolean
    :default false
    :persist? true
    :gui-sync? true
    :gui-coerce boolean
    :block-state {:prop "on"
            :type :boolean
            :default false}
    :network-editable? true
    :network-msg :toggle-enabled}

     {:key :placer-name
    :nbt-key "Placer"
    :type :string
    :default ""
    :persist? true
    :gui-sync? true
    :gui-data-slot? false}

   {:key :whitelist
    :nbt-key "Whitelist"
    :type :string-list
    :default []
    :persist? true
    ;; :client-sync? required for the commit :sync-client? gate — without it
    ;; the field's flag mask lacks client-sync-mask and the manual update-tag
    ;; push is skipped, so a reopened GUI reads a stale client BE state.
    :client-sync? true
    :gui-sync? true
    :gui-data-slot? false}

   {:key :affected-player-count
    :nbt-key "AffectedPlayerCount"
    :type :int
    :default 0
    :persist? false
    :gui-sync? true
    :gui-coerce int}

   {:key :inventory
    :nbt-key "Inventory"
    :type :inventory
    :default [nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory}

     {:key :affected-player-uuids
    :type :string-list
    :default []
    :persist? false}

   ])
