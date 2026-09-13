(ns cn.li.mc1201.runtime.accessor-registry
  "Thin re-export of cn.li.mcbase.runtime.accessor-registry."
  (:require [cn.li.mcbase.runtime.accessor-registry :as shared]))

(def init-default-accessors! shared/init-default-accessors!)
