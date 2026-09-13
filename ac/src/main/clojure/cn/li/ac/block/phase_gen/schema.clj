(ns cn.li.ac.block.phase-gen.schema
  "Phase Generator state schema"
  (:require [cn.li.mcmod.block.inventory-helpers :as inv-helpers]))

(def phase-gen-schema
  "Schema for phase generator block"
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
    :default 6000.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :gen-speed
    :nbt-key "GenSpeed"
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :liquid-amount
    :nbt-key "LiquidAmount"
    :type :int
    :default 0
    :persist? true
    :gui-sync? true
    :gui-coerce int}

   {:key :tank-size
    :nbt-key "TankSize"
    :type :int
    :default 8000
    :persist? true
    :gui-sync? true
    :gui-coerce int}

   {:key :inventory
    :nbt-key "Inventory"
    :type :inventory
    :default [nil nil nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory}

   {:key :status
    :nbt-key "Status"
    :type :string
    :default "IDLE"
    :persist? true
    :gui-sync? true
    :gui-data-slot? false}

   ])
