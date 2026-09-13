(ns cn.li.mc262.block.blockstate-properties
  "Thin re-export of cn.li.mcbase.block.blockstate-properties."
  (:require [cn.li.mcbase.block.blockstate-properties :as shared]))

(def create-adapter-registry shared/create-adapter-registry)
(def register-block-properties! shared/register-block-properties!)
(def register-default-block-properties! shared/register-default-block-properties!)
(def get-property shared/get-property)
(def get-all-properties shared/get-all-properties)
(def init-all-properties! shared/init-all-properties!)
