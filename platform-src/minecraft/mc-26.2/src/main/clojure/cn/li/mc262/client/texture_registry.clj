(ns cn.li.mc262.client.texture-registry
  "Thin re-export of cn.li.mcbase.client.texture-registry."
  (:require [cn.li.mcbase.client.texture-registry :as shared]))

(def register-texture! shared/register-texture!)
(def resolve-texture shared/resolve-texture)
(def reset-texture-registry-for-test! shared/reset-texture-registry-for-test!)
