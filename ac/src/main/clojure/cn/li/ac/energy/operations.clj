(ns cn.li.ac.energy.operations
  "Energy operations - 物品/节点/接收器的充放电与无线传输

  实现并统一对外提供：
  - IFItemManager：物品能量（电池充放电、容量、带宽）
  - IFNodeManager：节点能量（Wireless Node 充放电）
  - IFReceiverManager：接收器能量（inject/pull）
  - 无线网络：获取网络、连接状态"
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.ac.energy.api.impl :as energy-api]
            [cn.li.ac.energy.service.item-manager :as item-manager]
            [cn.li.ac.energy.service.node-manager :as node-manager]
            [cn.li.ac.wireless.api :as wireless-api]))

(defn energy-system
  "Return the Phase C energy system implementation for an explicit owner."
  [owner]
  (energy-api/energy-system owner))

;; ============================================================================
;; IFItemManager Implementation
;; ============================================================================

(defn is-energy-item-supported?
  "Check if an item supports energy storage"
  [item-stack]
  (item-manager/is-energy-item-supported? item-stack))

(defn get-item-energy
  "Get energy stored in item"
  [item-stack]
  (item-manager/get-item-energy item-stack))

(defn get-item-max-energy
  "Get maximum energy capacity of item"
  [item-stack]
  (item-manager/get-item-capacity item-stack))

(defn get-item-bandwidth
  "Get energy transfer bandwidth of item"
  [item-stack]
  (item-manager/get-item-bandwidth item-stack))

(defn set-item-energy!
  "Set energy in item and update durability display"
  [item-stack energy]
  (item-manager/set-item-energy! item-stack energy))

(defn charge-energy-to-item
  "Charge energy to item
  Returns the amount that couldn't be charged (leftover)"
  [item-stack amount ignore-bandwidth]
  (item-manager/charge-energy-to-item item-stack amount ignore-bandwidth))

(defn pull-energy-from-item
  "Pull energy from item
  Returns the amount actually pulled"
  [item-stack amount ignore-bandwidth]
  (item-manager/pull-energy-from-item item-stack amount ignore-bandwidth))

;; ============================================================================
;; IFNodeManager Implementation
;; ============================================================================

(defn is-node-supported?
  "Check if TileEntity is an IWirelessNode"
  [tile-entity]
  (node-manager/is-node-supported? tile-entity))

(defn get-node-energy
  "Get energy from node"
  [tile-entity]
  (node-manager/get-node-energy tile-entity))

(defn set-node-energy!
  "Set energy in node"
  [tile-entity energy]
  (node-manager/set-node-energy! tile-entity energy))

(defn charge-node
  "Charge energy to node
  Returns the amount that couldn't be charged (leftover)"
  [tile-entity amount ignore-bandwidth]
  (node-manager/charge-node tile-entity amount ignore-bandwidth))

(defn pull-from-node
  "Pull energy from node
  Returns the amount actually pulled"
  [tile-entity amount ignore-bandwidth]
  (node-manager/pull-from-node tile-entity amount ignore-bandwidth))

;; ============================================================================
;; IFReceiverManager Implementation
;; ============================================================================

(defn is-receiver-supported?
  "Check if TileEntity is an IWirelessReceiver"
  [tile-entity]
  (node-manager/is-receiver-supported? tile-entity))

(defn charge-receiver
  "Charge energy to receiver (calls inject-energy)
  Returns the amount that couldn't be charged"
  [tile-entity amount]
  (node-manager/charge-receiver tile-entity amount))

(defn pull-from-receiver
  "Pull energy from receiver (calls pull-energy)
  Returns the amount actually pulled"
  [tile-entity amount]
  (node-manager/pull-from-receiver tile-entity amount))

;; ============================================================================
;; Wireless Network (delegates to cn.li.ac.wireless.api)
;; ============================================================================

(defn get-wireless-network
  "Get wireless network for a node via `wireless.api`.
  Returns nil if no network is found or an error occurs."
  [node _password]
  (try
    (wireless-api/get-wireless-net-by-node node)
    (catch Exception _
      (log/warn "get-wireless-network failed; returning nil for safety")
      nil)))

(defn is-node-connected?
  "Check if node is connected to wireless network via `wireless.api`"
  [node _password]
  (try
    (boolean (wireless-api/is-node-linked? node))
    (catch Exception _
      (log/warn "is-node-connected? failed; returning false for safety")
      false)))

