(ns cn.li.ac.block.wireless-node.capability
  "Wireless node API/capability implementations."
  (:require [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.wireless.config :as node-config]
            [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]))

(deftype WirelessNodeImpl [be]
  cn.li.acapi.wireless.IWirelessNode

  (getEnergy [_]
    (let [state (or (platform-be/get-custom-state be) node-logic/node-default-state)]
      (double (state-schema/get-field node-logic/node-state-schema state :energy))))

  (setEnergy [_ energy]
    (machine-runtime/commit-transform! be node-logic/node-default-state
                                       #(assoc % :energy (double energy))))

  (getMaxEnergy [_]
    (let [state (or (platform-be/get-custom-state be) node-logic/node-default-state)]
      (double (node-logic/node-max-energy state))))

  (getBandwidth [_]
    (let [state     (or (platform-be/get-custom-state be) node-logic/node-default-state)
          node-type (keyword (state-schema/get-field node-logic/node-state-schema state :node-type))]
      (double (node-config/bandwidth node-type))))

  (getCapacity [_]
    (let [state     (or (platform-be/get-custom-state be) node-logic/node-default-state)
          node-type (keyword (state-schema/get-field node-logic/node-state-schema state :node-type))]
      (int (node-config/capacity node-type))))

  (getRange [_]
    (let [state     (or (platform-be/get-custom-state be) node-logic/node-default-state)
          node-type (keyword (state-schema/get-field node-logic/node-state-schema state :node-type))]
      (double (node-config/range-blocks node-type))))

  (getNodeName [_]
    (let [state (or (platform-be/get-custom-state be) node-logic/node-default-state)]
      (str (state-schema/get-field node-logic/node-state-schema state :node-name))))

  (getPassword [_]
    (let [state (or (platform-be/get-custom-state be) node-logic/node-default-state)]
      (str (state-schema/get-field node-logic/node-state-schema state :password))))

  (getBlockPos [_]
    (pos/block-pos be))

  Object
  (toString [_]
    (str "WirelessNodeImpl@" (pos/block-pos be))))

(deftype ClojureEnergyImpl [be]
  cn.li.mcmod.energy.IEnergyCapable

  (receiveEnergy [_ max-receive simulate]
    (let [state    (or (platform-be/get-custom-state be) node-logic/node-default-state)
          cur      (double (state-schema/get-field node-logic/node-state-schema state :energy))
          max-e    (double (node-logic/node-max-energy state))
          can-recv (- max-e cur)
          actual   (min can-recv (double max-receive))]
      (when (and (not simulate) (pos? actual))
        (machine-runtime/commit-transform! be node-logic/node-default-state
                                         #(assoc % :energy (+ cur actual))
                                         :blockstate-updater node-logic/update-block-state!))
      (int actual)))

  (extractEnergy [_ max-extract simulate]
    (let [state  (or (platform-be/get-custom-state be) node-logic/node-default-state)
          cur    (double (state-schema/get-field node-logic/node-state-schema state :energy))
          actual (min cur (double max-extract))]
      (when (and (not simulate) (pos? actual))
        (machine-runtime/commit-transform! be node-logic/node-default-state
                                         #(assoc % :energy (- cur actual))
                                         :blockstate-updater node-logic/update-block-state!))
      (int actual)))

  (getEnergyStored [_]
    (let [state (or (platform-be/get-custom-state be) node-logic/node-default-state)]
      (int (state-schema/get-field node-logic/node-state-schema state :energy))))

  (getMaxEnergyStored [_]
    (let [state (or (platform-be/get-custom-state be) node-logic/node-default-state)]
      (int (node-logic/node-max-energy state))))

  (canExtract [_] true)
  (canReceive [_] true)

  Object
  (toString [_]
    (str "ClojureEnergyImpl@" (pos/block-pos be))))
