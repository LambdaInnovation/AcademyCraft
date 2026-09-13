(ns cn.li.ac.block.imag-fusor.schema
  "Imaginary Fusor state schema"
  (:require [cn.li.mcmod.block.inventory-helpers :as inv-helpers]))

(def imag-fusor-schema
  "Schema for imaginary fusor block"
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
    :default 2000.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :crafting-progress
    :nbt-key "CraftingProgress"
    :type :int
    :default 0
    :persist? true
    :gui-sync? true
    :gui-coerce int}

   {:key :max-progress
    :nbt-key "MaxProgress"
    :type :int
    :default 120
    :persist? true
    :gui-sync? true
    :gui-coerce int}

     {:key :work-progress
    :nbt-key "WorkProgress"
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

   {:key :working
    :nbt-key "Working"
    :type :boolean
    :default false
    :persist? true
    :gui-sync? true
    :gui-coerce boolean}

  {:key :frame
   :nbt-key "Frame"
   :type :int
   :default 0
   :persist? true
   :gui-sync? false
   :block-state {:prop "frame"
            :type :integer
            :min 0
            :max 4
            :default 0}}

   {:key :current-recipe-id
    :nbt-key "CurrentRecipeId"
    :type :string
    :default ""
    :persist? true}

  {:key :current-recipe-liquid
   :nbt-key "CurrentRecipeLiquid"
   :type :int
   :default 0
   :persist? true
   :gui-sync? true
   :gui-coerce int}

   {:key :inventory
    :nbt-key "Inventory"
    :type :inventory
    :default [nil nil nil nil nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory}

  {:key :check-cooldown
   :nbt-key "CheckCooldown"
   :type :int
   :default 10
   :persist? true
   :gui-sync? false
   :gui-coerce int}

   {:key :sound-cooldown
    :type :int
    :default 0
    :persist? false
    :gui-sync? false}

   {:key :facing
    :nbt-key "Facing"
    :type :string
    :default "north"
    :persist? true
    :block-state {:prop "facing"
                 :type :horizontal-facing
                 :default "north"}}])
