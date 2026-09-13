(ns cn.li.mc1211.datagen.block-loot-provider-shell
  (:require [cn.li.mcbase.datagen.block-loot-provider-core :as shared]))

(defn create [pack-output]
  (shared/create pack-output {:loot-path "loot_tables" :modern? true}))
