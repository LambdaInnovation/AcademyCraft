(ns cn.li.fabric262.integration.achievement-bridge
  "Fabric achievement-trigger bridge for Minecraft 26.2."
  (:require [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]
            [cn.li.fabric262.adapter.server-context :as server-context])
  (:import [cn.li.mc262.trigger ModTriggers]
           [java.util UUID]
           [net.minecraft.server.level ServerPlayer]))

(defn- resolve-player [uuid-str]
  (when-let [server (server-context/get-server)]
    (some-> server .getPlayerList (.getPlayer (UUID/fromString (str uuid-str))))))

(defn init! []
  (install/process-once! ::installed
    #(do
       (power-runtime/subscribe-achievement-trigger!
        (fn [{:keys [uuid achievement-id]}]
          (try
            (when-let [^ServerPlayer player (resolve-player uuid)]
              (.trigger ModTriggers/CUSTOM player (str achievement-id)))
            (catch Exception e
              (log/warn "Failed to dispatch Fabric achievement trigger" achievement-id (ex-message e))))))
       (log/debug "Fabric achievement bridge initialized")))
  nil)
