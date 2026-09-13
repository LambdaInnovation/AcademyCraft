(ns cn.li.mc1201.gui.registry.common
  "Thin re-export of cn.li.mcbase.gui.registry.common."
  (:require [cn.li.mcbase.gui.registry.common :as shared]))

(def create-wrapped-container shared/create-wrapped-container)
(def read-block-pos shared/read-block-pos)
(def write-block-pos! shared/write-block-pos!)
(def read-extended-open-payload shared/read-extended-open-payload)
(def write-extended-open-payload! shared/write-extended-open-payload!)
(def create-client-menu! shared/create-client-menu!)
