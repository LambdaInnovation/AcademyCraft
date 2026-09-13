(ns cn.li.mc262.integration.event-helpers-core
  "Thin re-export of cn.li.mcbase.integration.event-helpers-core."
  (:require [cn.li.mcbase.integration.event-helpers-core :as shared]))

(def get-player-uuid shared/get-player-uuid)
(def runtime-activated? shared/runtime-activated?)
(def build-block-event-data shared/build-block-event-data)
(def validate-block-event-data shared/validate-block-event-data)
(def make-platform-adapter shared/make-platform-adapter)
