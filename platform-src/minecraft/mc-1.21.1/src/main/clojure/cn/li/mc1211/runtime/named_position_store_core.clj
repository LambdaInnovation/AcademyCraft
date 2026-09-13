(ns cn.li.mc1211.runtime.named-position-store-core
  "Thin re-export of cn.li.mcbase.runtime.named-position-store-core."
  (:require [cn.li.mcbase.runtime.named-position-store-core :as shared]))

(def save-location! shared/save-location!)
(def delete-location! shared/delete-location!)
(def get-location shared/get-location)
(def list-locations shared/list-locations)
(def get-location-count shared/get-location-count)
(def has-location? shared/has-location?)
(def create-named-position-store shared/create-named-position-store)
