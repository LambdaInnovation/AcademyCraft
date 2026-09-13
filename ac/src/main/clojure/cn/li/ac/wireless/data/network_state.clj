(ns cn.li.ac.wireless.data.network-state
  "Immutable wireless network model and accessors."
  (:require [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]))

(defrecord WirelessNet
  [world-data
   matrix
   state])

(defn- default-state
  [ssid password]
  {:ssid ssid
   :password password
   :nodes []
   :buffer 0.0
   :disposed false})

(defn create-wireless-net
  "Create a new wireless network."
  [world-data matrix-vblock ssid password]
  (->WirelessNet
    world-data
    matrix-vblock
    (default-state ssid password)))

(defn get-matrix
  "Get the matrix TileEntity."
  [network world]
  (resolver/resolve-matrix-cap world (:matrix network)))

(defn state-value
  [network key]
  (get (:state network) key))

(defn- with-state
  [network state]
  (assoc network :state state))

(defn- update-state
  [network f]
  (with-state network (f (:state network))))

(defn set-state-value
  [network key value]
  (update-state network #(assoc % key value)))

(defn update-state-value
  [network key f & args]
  (update-state network
                #(if (seq args)
                     (apply update % key f args)
                     (update % key f))))

(defn- commit!
  [network updated]
  (entity-commit/commit-network! (:world-data network) updated))

(defn set-state-value!
  [network key value]
  (let [updated (set-state-value network key value)]
    (commit! network updated)
    updated))

(defn update-state-value!
  [network key f & args]
  (let [updated (apply update-state-value network key f args)]
    (commit! network updated)
    updated))

(defn is-disposed? [network] (boolean (state-value network :disposed)))
(defn get-ssid [network] (state-value network :ssid))
(defn get-password [network] (state-value network :password))
(defn get-nodes [network] (vec (or (state-value network :nodes) [])))
(defn get-load [network] (count (get-nodes network)))

(defn get-buffer [network] (state-value network :buffer))
(defn set-nodes! [network nodes]
  (set-state-value! network :nodes (vec nodes)))
(defn update-nodes! [network f & args]
  (let [updated (if (seq args)
                    (update-state network #(apply update % :nodes f args))
                    (update-state network #(update % :nodes f)))]
    (commit! network updated)
    updated))
(defn set-buffer! [network value]
  (let [updated (set-state-value! network :buffer value)]
    (state-value updated :buffer)))
(defn mark-disposed! [network]
  (set-state-value! network :disposed true))

(defn active?
  [network]
  (boolean (and network (not (is-disposed? network)))))

(defn snapshot
  "Return a read-only, plain-value view of a wireless network."
  [network]
  (when network
    {:matrix (:matrix network)
     :ssid (get-ssid network)
     :password (get-password network)
     :nodes (get-nodes network)
     :load (get-load network)
     :disposed? (is-disposed? network)}))

(defn get-capacity
  "Get network capacity from matrix."
  [network world]
  (if-let [matrix (get-matrix network world)]
    (.getMatrixCapacity ^cn.li.acapi.wireless.IWirelessMatrix matrix)
    0))
