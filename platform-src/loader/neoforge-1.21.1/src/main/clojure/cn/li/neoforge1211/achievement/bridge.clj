(ns cn.li.neoforge1211.achievement.bridge
  "Forge bridge for AC achievement-trigger events.

  This bridge is intentionally generic:
  - input: {uuid achievement-id}
  - output: ModCustomTrigger.trigger(ServerPlayer, String)"
  (:require [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mc1211.trigger ModTriggers]
           [java.util UUID]
           [net.neoforged.neoforge.server ServerLifecycleHooks]))

(defn- resolve-player
  [uuid-str]
  (when-let [server (ServerLifecycleHooks/getCurrentServer)]
    (let [player-list (.getPlayerList server)]
      (.getPlayer player-list (UUID/fromString (str uuid-str))))))

(defn init!
  []
  (install/process-once! ::installed
    #(do
       (power-runtime/subscribe-achievement-trigger!
         (fn [{:keys [uuid achievement-id]}]
           (try
             (when-let [player (resolve-player uuid)]
               (.trigger ModTriggers/CUSTOM player (str achievement-id)))
             (catch Exception e
               (log/warn "Failed to dispatch achievement trigger" achievement-id (ex-message e))))))
       (log/debug "Forge achievement bridge initialized")))
  nil)
