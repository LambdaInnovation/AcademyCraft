(ns cn.li.fabric1211.runtime.entity-motion
  "Fabric implementation of IEntityMotion protocol."
  (:require [cn.li.fabric1211.adapter.server-context :as server-context]
            [cn.li.mc1211.runtime.entity-motion-core :as core])
  (:import [net.minecraft.server MinecraftServer]))

(defn- get-server ^MinecraftServer []
  (server-context/get-server))

(defn fabric-entity-motion []
  (core/create-entity-motion get-server))
