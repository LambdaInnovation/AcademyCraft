(ns cn.li.mc1211.runtime.item-handler-core
  "Thin re-export of cn.li.mcbase.runtime.item-handler-core."
  (:require [cn.li.mcbase.runtime.item-handler-core :as shared]))

(def get-item-id shared/get-item-id)
(def resolve-dsl-item-spec shared/resolve-dsl-item-spec)
(def dispatch-dsl-item-use! shared/dispatch-dsl-item-use!)
(def dispatch-dsl-item-right-click-consume? shared/dispatch-dsl-item-right-click-consume?)
(def dispatch-dsl-item-finish-using! shared/dispatch-dsl-item-finish-using!)
(def process-item-use! shared/process-item-use!)
