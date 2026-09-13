(ns cn.li.mc1211.runtime.potion-effects-core
  "Thin re-export of cn.li.mcbase.runtime.potion-effects-core."
  (:require [cn.li.mcbase.runtime.potion-effects-core :as shared]))

(def apply-potion-effect! shared/apply-potion-effect!)
(def remove-potion-effect! shared/remove-potion-effect!)
(def has-potion-effect? shared/has-potion-effect?)
(def clear-all-effects! shared/clear-all-effects!)
(def create-potion-effects shared/create-potion-effects)
