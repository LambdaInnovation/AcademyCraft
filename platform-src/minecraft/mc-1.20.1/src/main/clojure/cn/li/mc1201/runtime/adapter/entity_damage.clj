(ns cn.li.mc1201.runtime.adapter.entity-damage
  "Thin re-export of cn.li.mcbase.runtime.adapter.entity-damage."
  (:require [cn.li.mcbase.runtime.adapter.entity-damage :as shared]))

(def create-entity-damage shared/create-entity-damage)
(def install-entity-damage! shared/install-entity-damage!)
