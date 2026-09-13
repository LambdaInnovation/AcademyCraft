(ns cn.li.ac.block.machine.wireless-handlers
  "Shared wireless generator/receiver network handlers for block GUIs."
  (:require [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.feedback :as feedback]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.wireless.gui.sync.handler :as net-helpers]
            [cn.li.ac.wireless.link-helpers :as link-helpers]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.gui.container.sync-routing :as sync-routing]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.util.log :as log]))

(defn wireless-node->info
  "Re-export for callers that already depend on this namespace."
  [node]
  (link-helpers/wireless-node->info node))

(defn available-nodes
  "Query in-range nodes and return link-panel DTOs excluding the linked node."
  [world tile-pos linked-node]
  (let [nodes (if tile-pos (wireless-api/get-nodes-in-range world tile-pos) [])]
    (link-helpers/available-node-infos nodes linked-node)))

(defn- resolve-controller-tile
  "If tile is a multiblock part, find the controller by enumerating the
  multiblock's relative positions, computing the candidate controller position
  for each, and checking for a tile with the controller block-id."
  [tile player]
  (if-let [block-id (platform-be/get-block-id tile)]
    (if-let [block-spec (bdsl/get-block-spec block-id)]
      (let [mb (:multi-block block-spec)]
        (if (and mb (= :controller-parts (:multiblock-mode mb))
                 (:part-block-id mb) (not (:multi-block? mb)))
          (if-let [ctrl-spec (some-> (:controller-block-id mb) bdsl/get-block-spec)]
            (if-let [world (net-helpers/get-world player)]
              (let [ctrl-mb (:multi-block ctrl-spec)
                    positions (:multi-block-positions ctrl-mb)
                    px (pos/pos-x (pos/block-pos tile))
                    py (pos/pos-y (pos/block-pos tile))
                    pz (pos/pos-z (pos/block-pos tile))]
                (or (some (fn [rel-pos]
                            (let [rx (or (:relative-x rel-pos) (:x rel-pos) 0)
                                  ry (or (:relative-y rel-pos) (:y rel-pos) 0)
                                  rz (or (:relative-z rel-pos) (:z rel-pos) 0)
                                  cx (- px rx)
                                  cy (- py ry)
                                  cz (- pz rz)
                                  cpos (pos/create-block-pos cx cy cz)
                                  ctile (world/get-tile-entity world cpos)]
                              (when (and ctile
                                         (= (:controller-block-id ctrl-mb)
                                            (platform-be/get-block-id ctile)))
                                ctile)))
                          positions)
                    tile))
              tile)
            tile)
          tile))
      tile)
    tile))

(defn- tile-from-payload
  [payload player]
  (let [container (sync-routing/require-open-container! payload player)
        tile (:tile-entity container)]
    (resolve-controller-tile tile player)))

(defn- nodes-in-range
  [world tile-pos]
  (if tile-pos
    (wireless-api/get-nodes-in-range world tile-pos)
    []))

(defn- build-link-response
  [get-linked-node device world]
  (let [linked-node (get-linked-node device)
        nodes (if world
                (nodes-in-range world (pos/block-pos device))
                [])]
    (link-helpers/link-panel-state linked-node nodes)))

(defn register-link-handlers!
  "Register list/connect/disconnect handlers for a wireless-linked machine.

  spec keys:
  - :message-domain keyword (:generator or :developer)
  - :get-linked-node (fn [tile] -> node or nil)
  - :link! (fn [tile node password need-auth?] -> bool)
  - :unlink! (fn [tile] -> nil)
  - :extra-handlers optional map msg-keyword -> handler
  - :log-label optional string"
  [{:keys [message-domain get-linked-node link! unlink! extra-handlers log-label]}]
  (let [msg (fn [action] (msg-registry/msg message-domain action))
        handle-list-nodes
        (fn [payload player]
          (try
            (let [tile (tile-from-payload payload player)
                  world (net-helpers/get-world player)]
              (log/debug "[handle-list-nodes]" log-label "tile=" (some? tile))
              (if tile
                (let [linked-node (get-linked-node tile)]
                  (link-helpers/list-nodes-success-response
                    linked-node
                    (nodes-in-range world (pos/block-pos tile))))
                (link-helpers/list-nodes-empty-response)))
            (catch Exception e
              (log/stacktrace "[handle-list-nodes]" e)
              {:success false :error (ex-message e)})))
        handle-connect
        (fn [payload player]
          (try
            (let [world (net-helpers/get-world player)
                  device (tile-from-payload payload player)
                  node-pos (link-helpers/payload-node-position payload)
                  pass (:password payload "")
                  need-auth? (boolean (:need-auth? payload true))]
              (log/info "[handle-connect] device=" (some? device) "world=" (some? world)
                        "node-pos=" (pr-str node-pos))
              (let [{:keys [linked avail]} (when device (build-link-response get-linked-node device world))]
                (if (and world device (link-helpers/valid-node-position? node-pos))
                  (if-let [node (net-helpers/get-tile-at world {:pos-x (:node-x node-pos)
                                                                :pos-y (:node-y node-pos)
                                                                :pos-z (:node-z node-pos)})]
                    (let [result (link! device node pass need-auth?)
                          {:keys [linked avail]} (build-link-response get-linked-node device world)]
                      (log/debug "[handle-connect] link result:" (pr-str result))
                      (assoc result :messages (feedback/result->messages message-domain result)
                                     :linked linked :avail avail))
                    {:success false :linked linked :avail avail
                     :messages (feedback/result->messages message-domain {:success false :reason :not-found})})
                  {:success false :linked linked :avail avail
                   :messages (feedback/result->messages message-domain {:success false :reason :aborted})})))
            (catch Exception e
              (log/stacktrace "[handle-connect]" e)
              {:success false :error (ex-message e)
               :messages (feedback/result->messages message-domain {:success false :reason :aborted})})))
        handle-disconnect
        (fn [payload player]
          (try
            (let [device (tile-from-payload payload player)
                  world (net-helpers/get-world player)
                  {:keys [linked avail]} (when device (build-link-response get-linked-node device world))]
              (if device
                (do
                  (unlink! device)
                  (let [{:keys [linked avail]} (build-link-response get-linked-node device world)]
                    {:success true :linked linked :avail avail
                     :messages (feedback/result->messages message-domain {:success true})}))
                {:success false :linked linked :avail avail
                 :messages (feedback/result->messages message-domain {:success false :reason :aborted})}))
            (catch Exception e
              (log/stacktrace "[handle-disconnect]" e)
              {:success false :error (ex-message e)
               :messages (feedback/result->messages message-domain {:success false :reason :aborted})})))]
    (net-server/register-handler (msg :list-nodes) handle-list-nodes)
    (net-server/register-handler (msg :connect) handle-connect)
    (net-server/register-handler (msg :disconnect) handle-disconnect)
    (doseq [[action handler] (or extra-handlers {})]
      (net-server/register-handler (msg action) handler))))
