(ns cn.li.fabric1211.runtime.entity-query
  "Fabric-side entity query adapters for platform-neutral API."
  (:require [cn.li.fabric1211.adapter.server-context :as server-context]
            [cn.li.mcbase.runtime.entity-query-core :as core]
            [cn.li.mcmod.platform.entity :as pentity]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server MinecraftServer]))

(defn- get-server ^MinecraftServer []
  (server-context/get-server))

(defn install-entity-query!
  []
  (install/framework-once! ::installed
    #(do
       (server-context/install-server-context!)
       (pentity/install-entity-type-id-fn!
         (core/create-entity-type-id-fn get-server)
         "Fabric entity query")
       (log/debug "Fabric entity query installed")))
  nil)
