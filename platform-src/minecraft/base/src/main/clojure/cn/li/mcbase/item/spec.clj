(ns cn.li.mcbase.item.spec
  "Shared item-spec value normalization for loader registration.")

(defn standalone-values
  [item-spec]
  {:energy-item? (true? (get-in item-spec [:properties :energy-item?]))
   :enchantability (int (or (:enchantability item-spec) 0))
   :tooltip-lines (mapv str (or (get-in item-spec [:properties :tooltip]) []))
   :current-key (str (or (get-in item-spec [:properties :bar-current-key]) "energy"))
   :max-key (str (or (get-in item-spec [:properties :bar-max-key]) "maxEnergy"))
   :default-max (double (or (get-in item-spec [:properties :energy-capacity]) 1.0))
   :bar-color (int (or (get-in item-spec [:properties :energy-bar-color]) 0x00E5FF))})
