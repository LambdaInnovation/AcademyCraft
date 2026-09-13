(ns cn.li.fabric1201.runtime.player-motion
  "Fabric implementation of IPlayerMotion protocol."
  (:require [cn.li.fabric1201.adapter.server-context :as server-context]
            [cn.li.mcbase.runtime.player-motion-core :as core])
  (:import [net.minecraft.server MinecraftServer]))

(defn- get-server ^MinecraftServer []
  (server-context/get-server))

(defn fabric-player-motion []
  (core/create-player-motion get-server))
