(ns cn.li.ac.block.cat-engine.schema
  "Cat Engine state schema")

(def cat-engine-schema
  "Schema for cat engine block"
  [{:key :energy
    :nbt-key "Energy"
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? false}

   {:key :max-energy
    :nbt-key "MaxEnergy"
    :type :double
    :default 2000.0
    :persist? true
    :gui-sync? false}

   {:key :this-tick-gen
    :nbt-key "TickGen"
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? false}

   {:key :gen-speed
    :type :double
    :default 0.0
    :persist? false}

   {:key :linked-node-name
    :nbt-key "LinkedNodeName"
    :type :string
    :default ""
    :persist? true
    :gui-sync? false
    :gui-data-slot? false}

   {:key :linked-node-x
    :nbt-key "LinkedNodeX"
    :type :int
    :default 0
    :persist? true}

   {:key :linked-node-y
    :nbt-key "LinkedNodeY"
    :type :int
    :default 0
    :persist? true}

   {:key :linked-node-z
    :nbt-key "LinkedNodeZ"
    :type :int
    :default 0
    :persist? true}

   {:key :has-link
    :nbt-key "HasLink"
    :type :boolean
    :default false
    :persist? true
    :gui-sync? false
    :gui-coerce boolean}

   ])
