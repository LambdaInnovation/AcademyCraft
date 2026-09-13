(ns cn.li.mc1211.runtime.teleportation-core
  "Thin re-export of cn.li.mcbase.runtime.teleportation-core."
  (:require [cn.li.mcbase.runtime.teleportation-core :as shared]))

(def get-level shared/get-level)
(def teleport-player! shared/teleport-player!)
(def teleport-with-entities! shared/teleport-with-entities!)
(def reset-fall-damage! shared/reset-fall-damage!)
(def get-player-position shared/get-player-position)
(def get-player-dimension shared/get-player-dimension)
(def create-teleportation shared/create-teleportation)
