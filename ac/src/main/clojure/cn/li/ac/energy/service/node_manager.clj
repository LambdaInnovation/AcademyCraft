(ns cn.li.ac.energy.service.node-manager
  "Wireless node and receiver energy management service.

  Provides a focused service layer over the existing
  IWirelessNode / IWirelessReceiver contracts.

  Block entities are the shared, generic ScriptedBlockEntity (no per-block
  Java class implements these interfaces directly) — the real IWirelessNode/
  IWirelessReceiver implementations are Clojure deftypes resolved on demand
  via the capability registry (cn.li.ac.block.wireless-node.capability,
  wired through capability-lookup/tile-capability), matching the pattern
  already used by cn.li.ac.wireless.core.vblock-resolver."
  (:require [cn.li.ac.energy.domain.container :as container]
            [cn.li.ac.wireless.core.capability-lookup :as cap-lookup])
  (:import [cn.li.acapi.wireless IWirelessNode IWirelessReceiver WirelessCapabilityKeys]))

(defn- resolve-node
  ^IWirelessNode [tile-entity]
  (cap-lookup/tile-capability tile-entity WirelessCapabilityKeys/NODE))

(defn- resolve-receiver
  ^IWirelessReceiver [tile-entity]
  (cap-lookup/tile-capability tile-entity WirelessCapabilityKeys/RECEIVER))

(defn is-node-supported?
  "Return true when the tile entity supports the wireless node API."
  [tile-entity]
  (some? (resolve-node tile-entity)))

(defn is-receiver-supported?
  "Return true when the tile entity supports the wireless receiver API."
  [tile-entity]
  (some? (resolve-receiver tile-entity)))

(defn get-node-energy
  "Read node energy, or nil when unsupported."
  [tile-entity]
  (when-let [node (resolve-node tile-entity)]
    (double (.getEnergy node))))

(defn get-node-capacity
  "Read node max energy, or nil when unsupported."
  [tile-entity]
  (when-let [node (resolve-node tile-entity)]
    (double (.getMaxEnergy node))))

(defn get-node-bandwidth
  "Read node transfer bandwidth, or nil when unsupported."
  [tile-entity]
  (when-let [node (resolve-node tile-entity)]
    (double (.getBandwidth node))))

(defn set-node-energy!
  "Set node energy in place and return the applied value."
  [tile-entity amount]
  (when-let [node (resolve-node tile-entity)]
    (.setEnergy node (double amount))
    (double (.getEnergy node))))

(defn charge-node
  "Insert energy into a node.

  Returns leftover energy that could not be inserted."
  [tile-entity amount ignore-bandwidth]
  (if-let [node (resolve-node tile-entity)]
    (let [current (.getEnergy node)
          max-energy (.getMaxEnergy node)
          bandwidth (.getBandwidth node)
          space (- max-energy current)
          limit (if ignore-bandwidth Double/MAX_VALUE bandwidth)
          to-charge (min amount space limit)
          leftover (- amount to-charge)]
      (.setEnergy node (+ current to-charge))
      (double leftover))
    (double amount)))

(defn pull-from-node
  "Extract energy from a node.

  Returns the amount successfully extracted."
  [tile-entity amount ignore-bandwidth]
  (if-let [node (resolve-node tile-entity)]
    (let [current (.getEnergy node)
          bandwidth (.getBandwidth node)
          limit (if ignore-bandwidth Double/MAX_VALUE bandwidth)
          to-pull (min amount current limit)]
      (.setEnergy node (- current to-pull))
      (double to-pull))
    0.0))

(defn charge-receiver
  "Inject energy into a receiver and return leftover energy."
  [tile-entity amount]
  (if-let [receiver (resolve-receiver tile-entity)]
    (double (.injectEnergy receiver amount))
    (double amount)))

(defn pull-from-receiver
  "Pull energy from a receiver and return the amount extracted."
  [tile-entity amount]
  (if-let [receiver (resolve-receiver tile-entity)]
    (double (.pullEnergy receiver amount))
    0.0))

(defn node->container
  "Project a node into the immutable EnergyContainer model."
  [tile-entity]
  (when (is-node-supported? tile-entity)
    (container/energy-container
      (max 1.0 (or (get-node-capacity tile-entity) 1.0))
      (double (or (get-node-energy tile-entity) 0.0))
      (max 1.0 (or (get-node-bandwidth tile-entity) 1.0))
      1.0)))
