(ns cn.li.mc1201.command.brigadier-util
  "Thin re-export of cn.li.mcbase.command.brigadier-util."
  (:require [cn.li.mcbase.command.brigadier-util :as shared]))

(def entity-arg-player-type shared/entity-arg-player-type)
(def entity-arg-get-player shared/entity-arg-get-player)
(def map-argument-type shared/map-argument-type)
(def brigadier-arg-present? shared/brigadier-arg-present?)
(def extract-argument-value shared/extract-argument-value)
(def extract-all-arguments shared/extract-all-arguments)
