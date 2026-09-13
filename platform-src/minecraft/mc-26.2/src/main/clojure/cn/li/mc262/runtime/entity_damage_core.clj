(ns cn.li.mc262.runtime.entity-damage-core
  "Thin re-export of cn.li.mcbase.runtime.entity-damage-core."
  (:require [cn.li.mcbase.runtime.entity-damage-core :as shared]))

(def resolve-damage-source shared/resolve-damage-source)
(def entity-pos-map shared/entity-pos-map)
(def candidate-map shared/candidate-map)
(def compute-aoe-damage shared/compute-aoe-damage)
(def apply-aoe-damage-flow! shared/apply-aoe-damage-flow!)
(def select-reflection-target-uuid shared/select-reflection-target-uuid)
(def compute-reflected-damage shared/compute-reflected-damage)
(def reflection-search-radius shared/reflection-search-radius)
(def apply-reflection-damage-flow! shared/apply-reflection-damage-flow!)
