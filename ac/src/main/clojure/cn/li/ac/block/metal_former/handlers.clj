(ns cn.li.ac.block.metal-former.handlers
  "Metal Former network handlers."
  (:require [cn.li.mcmod.network.server :as net-server]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.metal-former.logic :as former-logic]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.util.log :as log]))

(defn- msg [action] (msg-registry/msg :metal-former action))

(defn- open-tile [payload player]
  (machine-handlers/open-container-tile payload player))

(defn handle-alternate [payload player]
  (let [tile (open-tile payload player)
        delta (int (or (:dir payload) 0))]
    (if-not tile
      {:success false}
      (let [state (or (platform-be/get-custom-state tile) former-logic/former-default-state)
            next-state (former-logic/cycle-mode state delta)]
        (machine-runtime/commit-from-tile! tile former-logic/former-default-state next-state)
        {:success true :mode (:mode next-state)}))))

(defn register-network-handlers! []
  (net-server/register-handler (msg :alternate) handle-alternate)
  (log/debug "Metal Former network handlers registered"))
