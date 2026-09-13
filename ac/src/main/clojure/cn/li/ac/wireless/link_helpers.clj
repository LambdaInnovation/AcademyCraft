(ns cn.li.ac.wireless.link-helpers
  "Pure helpers for generator/receiver <-> wireless-node link UI DTOs.

  Message domains and network handler registration stay in
  cn.li.ac.block.machine.wireless-handlers."
  (:require [cn.li.mcmod.platform.position :as pos])
  (:import [cn.li.acapi.wireless IWirelessNode]))

(defn wireless-node->info
  "Serialize a wireless node capability to GUI link-panel DTO."
  [^IWirelessNode node]
  (when node
    (let [p (try (.getBlockPos node) (catch Exception _ nil))
          pw (try (str (.getPassword node)) (catch Exception _ ""))]
      {:node-name (try (str (.getNodeName node)) (catch Exception _ "Node"))
       :pos-x (when p (pos/pos-x p))
       :pos-y (when p (pos/pos-y p))
       :pos-z (when p (pos/pos-z p))
       :is-encrypted? (boolean (seq pw))})))

(defn node-block-pos
  "Return the block position for a node capability, or nil."
  [^IWirelessNode node]
  (when node
    (try (.getBlockPos node) (catch Exception _ nil))))

(defn same-block-pos?
  "True when two block positions refer to the same block."
  [p linked-pos]
  (and p linked-pos
       (= (pos/pos-x p) (pos/pos-x linked-pos))
       (= (pos/pos-y p) (pos/pos-y linked-pos))
       (= (pos/pos-z p) (pos/pos-z linked-pos))))

(defn payload-node-position
  "Extract node coordinates from a connect payload."
  [payload]
  (select-keys payload [:node-x :node-y :node-z]))

(defn valid-node-position?
  "True when payload carries a complete node block position."
  [node-pos]
  (and (number? (:node-x node-pos))
       (number? (:node-y node-pos))
       (number? (:node-z node-pos))))

(defn available-node-infos
  "Map in-range nodes to link DTOs, excluding the currently linked node position."
  [nodes linked-node]
  (let [linked-pos (node-block-pos linked-node)]
    (->> nodes
         (remove (fn [^IWirelessNode n]
                   (same-block-pos? (node-block-pos n) linked-pos)))
         (mapv wireless-node->info))))

(defn link-panel-state
  "Build {:linked info-or-nil :avail [infos]} for generator/receiver link panels."
  [linked-node nodes-in-range]
  {:linked (wireless-node->info linked-node)
   :avail (available-node-infos nodes-in-range linked-node)})

(defn list-nodes-success-response
  "Success payload for list-nodes handlers."
  [linked-node nodes-in-range]
  (assoc (link-panel-state linked-node nodes-in-range) :success true))

(defn list-nodes-empty-response
  "Failure payload when the requesting tile cannot be resolved."
  []
  {:success false :linked nil :avail []})

(defn attach-link-panel
  "Merge link-panel fields into an existing handler result map."
  [result linked-node nodes-in-range]
  (let [{:keys [linked avail]} (link-panel-state linked-node nodes-in-range)]
    (assoc result :linked linked :avail avail)))
