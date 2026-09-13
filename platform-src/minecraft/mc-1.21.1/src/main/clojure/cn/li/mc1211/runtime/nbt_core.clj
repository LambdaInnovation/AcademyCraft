(ns cn.li.mc1211.runtime.nbt-core
  "Thin re-export of cn.li.mcbase.runtime.nbt-core."
  (:require [cn.li.mcbase.runtime.nbt-core :as shared]))

(def load-player-state! shared/load-player-state!)
(def save-player-state! shared/save-player-state!)
(def clone-player-state! shared/clone-player-state!)
