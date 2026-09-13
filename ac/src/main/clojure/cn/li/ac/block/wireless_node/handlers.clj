(ns cn.li.ac.block.wireless-node.handlers
  "Wireless Node network message handlers and infrastructure."
  (:require [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.block.inventory-helpers :as inv-helpers]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.wireless.config :as search-config]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.gui.sync.handler :as net-helpers]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.wireless.feedback :as feedback])
  (:import [cn.li.acapi.wireless IWirelessNode IWirelessMatrix]))

;; Stub Var for test injection (wireless_node_handlers_test relies on redefs).
(def generated-network-handlers nil)

(defn- open-tile [payload player]
  (machine-handlers/open-container-tile payload player))

(defn linked-network
  [tile]
  (try
    (wireless-api/get-wireless-net-by-node tile)
    (catch Exception _ nil)))

(defn node-range
  [tile]
  (try
    (if-let [node-cap (resolver/node-capability tile)]
      (.getRange ^IWirelessNode node-cap)
      20.0)
    (catch Exception _ 20.0)))

(defn available-networks
  [world x y z range]
  (wireless-api/get-nets-in-range world x y z range (search-config/max-results)))

(defn matrix-capability
  [world net]
  (when (:matrix net)
    (resolver/resolve-matrix-cap world (:matrix net))))

(defn matrix-capacity
  [matrix-cap]
  (if matrix-cap
    (try (.getMatrixCapacity ^IWirelessMatrix matrix-cap) (catch Exception _ 0))
    0))

(defn matrix-bandwidth
  [matrix-cap]
  (if matrix-cap
    (try (.getMatrixBandwidth ^IWirelessMatrix matrix-cap) (catch Exception _ 0))
    0))

(defn matrix-range
  [matrix-cap]
  (if matrix-cap
    (try (.getMatrixRange ^IWirelessMatrix matrix-cap) (catch Exception _ 0.0))
    0.0))

(defn connect-node!
  [world tile ssid password]
  (wireless-api/connect-node-to-ssid! world tile ssid password))

(defn disconnect-node!
  [tile]
  (wireless-api/unlink-node-from-network! tile)
  true)

(defn linked->dto
  [linked]
  (when linked
    (let [{:keys [ssid password]} (wireless-api/network-snapshot linked)]
      {:ssid ssid
       :is-encrypted? (boolean (seq (str password)))})))

(defn available-net->dto
  [net matrix-cap {:keys [matrix-capacity matrix-bandwidth matrix-range]}]
  (let [{:keys [ssid password load]} (wireless-api/network-snapshot net)]
    {:ssid ssid
     :is-encrypted? (boolean (seq (str password)))
     :load load
     :capacity (matrix-capacity matrix-cap)
     :bandwidth (matrix-bandwidth matrix-cap)
     :range (matrix-range matrix-cap)}))

(defn list-networks-response
  [{:keys [linked avail linked-ssid matrix-cap-fn matrix-capacity matrix-bandwidth matrix-range]}]
  (let [after-remove (remove (fn [net] (= (wireless-api/network-ssid net) linked-ssid)) avail)
        result {:linked (linked->dto linked)
                :avail (mapv (fn [net]
                              (let [matrix-cap (matrix-cap-fn net)]
                                (available-net->dto
                                  net
                                  matrix-cap
                                  {:matrix-capacity matrix-capacity
                                   :matrix-bandwidth matrix-bandwidth
                                   :matrix-range matrix-range})))
                            after-remove)}]
    result))

(defn- msg [action]
  (msg-registry/msg :node action))

(defn- owner-authorized?
  [tile player]
  (node-logic/owner-authorized? (platform-be/get-custom-state tile) player))

(defn- with-owner-authorization
  [payload player handler-fn]
  (let [tile (open-tile payload player)]
    (if (and tile (owner-authorized? tile player))
      (handler-fn payload player tile)
      {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})})))

(defn handle-change-name
  [payload player]
  (log/debug "[NodeGUI-Server] handle-change-name called:" {:payload payload
                                                            :player (str player)})
  (let [tile (open-tile payload player)
        authorized? (and tile (owner-authorized? tile player))]
    (log/debug "[NodeGUI-Server] change-name auth:" {:tile (boolean tile)
                                                     :authorized? (boolean authorized?)})
    (if authorized?
      (let [new-value (:node-name payload)]
        (log/debug "[NodeGUI-Server] change-name updating:" {:new-value new-value})
        (if (and tile new-value)
          (do (inv-helpers/update-be-field! tile :node-name new-value)
              (log/debug "[NodeGUI-Server] change-name success")
              {:success true :messages (feedback/result->messages :node {:success true})})
          (do (log/debug "[NodeGUI-Server] change-name aborted: missing tile or new-value")
              {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})})))
      (do (log/warn "[NodeGUI-Server] change-name unauthorized")
          {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})}))))

(defn handle-change-password
  [payload player]
  (log/debug "[NodeGUI-Server] handle-change-password called:" {:has-password (boolean (:password payload))
                                                                :player (str player)})
  (let [tile (open-tile payload player)
        authorized? (and tile (owner-authorized? tile player))]
    (log/debug "[NodeGUI-Server] change-password auth:" {:tile (boolean tile)
                                                         :authorized? (boolean authorized?)})
    (if authorized?
      (let [new-value (:password payload)]
        (log/debug "[NodeGUI-Server] change-password updating:" {:new-value-len (count (str new-value))})
        (if (and tile new-value)
          (do (inv-helpers/update-be-field! tile :password new-value)
              (log/debug "[NodeGUI-Server] change-password success")
              {:success true :messages (feedback/result->messages :node {:success true})})
          (do (log/debug "[NodeGUI-Server] change-password aborted: missing tile or new-value")
              {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})})))
      (do (log/warn "[NodeGUI-Server] change-password unauthorized")
          {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})}))))

(defn handle-list-networks
  [payload player]
  (let [tile (open-tile payload player)
        world (net-helpers/get-world player)]
    (if tile
      (let [linked (linked-network tile)
            linked-ssid (when linked (wireless-api/network-ssid linked))
            block-pos (pos/block-pos tile)
            x (double (pos/pos-x block-pos))
            y (double (pos/pos-y block-pos))
            z (double (pos/pos-z block-pos))
            range (node-range tile)
            avail (available-networks world x y z range)]
        (list-networks-response
          {:linked linked
           :avail avail
           :linked-ssid linked-ssid
           :matrix-cap-fn (fn [net] (matrix-capability world net))
           :matrix-capacity matrix-capacity
           :matrix-bandwidth matrix-bandwidth
           :matrix-range matrix-range}))
      {:linked nil :avail []})))

(defn handle-connect
  [payload player]
  (let [world (net-helpers/get-world player)
        tile (open-tile payload player)
        ssid (:ssid payload)
        password (:password payload)]
    (if (and world tile ssid)
      (let [result (connect-node! world tile ssid password)]
        (assoc result :messages (feedback/result->messages :node result)))
      {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})})))

(defn handle-disconnect
  [payload player]
  (let [tile (open-tile payload player)]
    (if tile
      (let [result {:success (disconnect-node! tile)}]
        (assoc result :messages (feedback/result->messages :node result)))
      {:success false :messages (feedback/result->messages :node {:success false :reason :aborted})})))

(defn handle-query-link
  "Handle periodic link-status query from client (matching original
  GuiNode MSG_QUERY_LINK: WirelessHelper.isNodeLinked(node))."
  [payload player]
  (let [tile (open-tile payload player)]
    {:linked (boolean (linked-network tile))}))

(defn register-network-handlers!
  []
  (net-server/register-handler (msg :change-name) handle-change-name)
  (net-server/register-handler (msg :change-password) handle-change-password)
  (net-server/register-handler (msg :list-networks) handle-list-networks)
  (net-server/register-handler (msg :connect) handle-connect)
  (net-server/register-handler (msg :disconnect) handle-disconnect)
  (net-server/register-handler (msg :query-link) handle-query-link)
  (log/debug "Node GUI network handlers registered"))
