(ns cn.li.mc1211.runtime.entity-motion-core
  "Thin re-export of cn.li.mcbase.runtime.entity-motion-core."
  (:require [cn.li.mcbase.runtime.entity-motion-core :as shared]))

(def set-velocity-for-entity! shared/set-velocity-for-entity!)
(def add-velocity-for-entity! shared/add-velocity-for-entity!)
(def set-position-for-entity! shared/set-position-for-entity!)
(def set-block-id-for-entity! shared/set-block-id-for-entity!)
(def set-place-when-collide-for-entity! shared/set-place-when-collide-for-entity!)
(def discard-entity! shared/discard-entity!)
(def set-projectile-damage-for-entity! shared/set-projectile-damage-for-entity!)
(def add-tag-for-entity! shared/add-tag-for-entity!)
(def get-velocity-for-entity shared/get-velocity-for-entity)
(def get-position-for-entity shared/get-position-for-entity)
(def power-creeper-for-entity! shared/power-creeper-for-entity!)
(def resolve-entity shared/resolve-entity)
(def resolve-level-and-entity shared/resolve-level-and-entity)
(def create-entity-motion shared/create-entity-motion)
