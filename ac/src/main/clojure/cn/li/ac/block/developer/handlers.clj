(ns cn.li.ac.block.developer.handlers
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.developer.logic :as dev-logic]
            [cn.li.ac.block.developer.session :as dev-session]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.machine.wireless-handlers :as wireless-handlers]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.wireless.gui.sync.handler :as net-helpers]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessNode]))

(defn- msg [action] (msg-registry/msg :developer action))

(defn- open-tile [payload player]
  (let [tile (machine-handlers/open-container-tile payload player)
        block-id (some-> tile platform-be/get-block-id)
        block-spec (some-> block-id bdsl/get-block-spec)
        mb (:multi-block block-spec)]
    ;; If the container gave us a multiblock part, resolve to the controller
    ;; by enumerating relative positions (controller = part - rel-offset).
    (if (and mb (= :controller-parts (:multiblock-mode mb))
             (:part-block-id mb) (not (:multi-block? mb)))
      (if-let [ctrl-spec (some-> (:controller-block-id mb) bdsl/get-block-spec)]
        (if-let [world (net-helpers/get-world player)]
          (let [ctrl-mb (:multi-block ctrl-spec)
                positions (:multi-block-positions ctrl-mb)
                px (pos/pos-x (pos/block-pos tile))
                py (pos/pos-y (pos/block-pos tile))
                pz (pos/pos-z (pos/block-pos tile))
                ctrl-tile (some (fn [rel-pos]
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
                                positions)]
            (or ctrl-tile tile))
          tile)
        tile)
      tile)))

(defn- get-linked-node-for-receiver [tile]
  (when-let [conn (try (wireless-api/get-node-conn-by-receiver tile) (catch Exception _ nil))]
    (try (node-conn/get-node conn (platform-be/be-get-world-safe tile)) (catch Exception _ nil))))

(defn handle-start-development [payload player]
  (log/debug "[handle-start-development] received payload=" (pr-str payload)
            "player=" (pr-str (some-> player entity/player-get-name)))
  (let [tile (open-tile payload player)
        world (net-helpers/get-world player)]
    (log/debug "[handle-start-development] tile=" (some? tile) "world=" (some? world))
    (if-not tile
      (do (log/warn "[handle-start-development] no tile found")
          {:success false :reason "no-tile"})
      (let [state (machine-runtime/state-or-default tile dev-logic/dev-default-state)
            ;; Force immediate structure validation so the player doesn't have
            ;; to wait for the periodic tick to set :structure-valid.
            structure-valid? (boolean (dev-logic/check-structure-valid? world tile))
            state (assoc state :structure-valid structure-valid?)
            result (dev-session/validate-and-start state player payload)]
        (log/debug "[handle-start-development] result ok?=" (:ok? result)
                  "reason=" (:reason result "none")
                  "structure-valid-now=" structure-valid?)
        (if (:ok? result)
          (let [session-id (runtime-hooks/player-state-session-id)
                new-state (-> (:state result)
                              (assoc :user-uuid (uuid/player-uuid player)
                                     :user-name (entity/player-get-name player)
                                     :player-state-session-id session-id))]
            (machine-runtime/commit-state! tile world nil state new-state)
            (log/debug "[handle-start-development] committed, is-developing=" (:is-developing new-state)
                      "progress=" (:development-progress new-state))
            {:success true})
          {:success false :reason (:reason result "rejected")})))))

(defn handle-stop-development [payload player]
  (let [tile (open-tile payload player)
        world (net-helpers/get-world player)]
    (if tile
      (let [state (machine-runtime/state-or-default tile dev-logic/dev-default-state)
            state' (dev-session/clear-session state)]
        (machine-runtime/commit-state! tile world nil state state')
        {:success true})
      {:success false})))

(defn register-network-handlers! []
  (wireless-handlers/register-link-handlers!
    {:message-domain :developer
     :get-linked-node get-linked-node-for-receiver
     :link! wireless-api/link-receiver-to-node!
     :unlink! wireless-api/unlink-receiver-from-node!
     :extra-handlers {:start-development handle-start-development
                      :stop-development handle-stop-development}
     :log-label "Developer network handlers registered"}))
