(ns cn.li.mc1211.datagen.metadata-resolver
  "Thin re-export of cn.li.mcbase.datagen.metadata-resolver."
  (:require [cn.li.mcbase.datagen.metadata-resolver :as shared]))

(def resolve-item shared/resolve-item)
(def resolve-tag shared/resolve-tag)
(def ingredient-from-spec shared/ingredient-from-spec)
