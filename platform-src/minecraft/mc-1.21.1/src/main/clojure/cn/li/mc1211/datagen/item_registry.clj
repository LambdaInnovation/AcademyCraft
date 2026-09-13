(ns cn.li.mc1211.datagen.item-registry
  "Thin re-export of cn.li.mcbase.datagen.item-registry."
  (:require [cn.li.mcbase.datagen.item-registry :as shared]))

(def known-item-ids shared/known-item-ids)
(def item-exists? shared/item-exists?)
(def safe-item-id shared/safe-item-id)
(def with-safe-items shared/with-safe-items)
