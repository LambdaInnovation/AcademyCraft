(ns cn.li.mc1211.runtime.interop-core
  "Thin re-export of cn.li.mcbase.runtime.interop-core."
  (:require [cn.li.mcbase.runtime.interop-core :as shared]))

(def get-level-by-id shared/get-level-by-id)
(def get-player-view shared/get-player-view)
(def get-player-main-hand-item shared/get-player-main-hand-item)
(def get-player-entity shared/get-player-entity)
(def get-block-entity-at shared/get-block-entity-at)
(def runtime-interop-impl shared/runtime-interop-impl)
(def install-runtime-interop! shared/install-runtime-interop!)
