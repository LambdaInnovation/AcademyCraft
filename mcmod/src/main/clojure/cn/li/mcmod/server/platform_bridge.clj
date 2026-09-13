(ns cn.li.mcmod.server.platform-bridge
  "Server bridge operations via Framework function map.

   Bridge ops stored at [:platform :server-bridge]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn- bridge-op-optional [k & args]
  (when-let [f (get-in @(fw/fw-atom) [:platform :server-bridge k])]
    (apply f args)))

(defn- bridge-op [k & args]
  (let [ops (get-in @(fw/fw-atom) [:platform :server-bridge])
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required server bridge operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

(defn install-server-bridge!
  "Install server bridge callbacks from a map of handler functions."
  [ops-map]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :server-bridge] ops-map)) nil)

(defn server-bridge-available? []
  (boolean (get-in @(fw/fw-atom) [:platform :server-bridge])))

(defn reset-server-bridge-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :server-bridge] nil)) nil)

(defn send-to-client!
  [player-uuid message-key payload]
  (or (bridge-op-optional :send-to-client! player-uuid message-key payload)
      (log/debug "Server bridge send-to-client! not available")))

(defn spawn-item-stack-at!
  [world-id x y z item-id count]
  (bridge-op :spawn-item-stack-at! world-id x y z item-id count))
