(ns cn.li.mc1211.key-scheme-provider-core
  "Thin re-export of cn.li.mcbase.key-scheme-provider-core."
  (:require [cn.li.mcbase.key-scheme-provider-core :as shared]))

(def key-display-name shared/key-display-name)
(def get-spi-implementation shared/get-spi-implementation)
