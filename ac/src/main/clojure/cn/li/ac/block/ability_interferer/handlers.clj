(ns cn.li.ac.block.ability-interferer.handlers
  "Ability Interferer network handlers."
  (:require [clojure.string :as str]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.machine.wireless-handlers :as wireless-handlers]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.ac.block.ability-interferer.logic :as interferer-logic]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.mcmod.util.log :as log]))

(defn- msg [action] (msg-registry/msg :ability-interferer action))

(defn- normalize-whitelist
  [names]
  (->> names
       (map #(str/trim (str %)))
       (remove str/blank?)
       distinct
       sort
       vec))

(defn- open-tile [payload player]
  (machine-handlers/open-container-tile payload player))

(defn- handle-change-range [payload player]
  (let [tile (open-tile payload player)
        requested (:range payload)]
    (if (and tile (number? requested))
      (do
        (machine-runtime/commit-transform!
          tile interferer-logic/interferer-default-state
          (fn [state]
            (assoc state :range (interferer-logic/clamp-range requested)))
          :blockstate-updater interferer-logic/interferer-blockstate-updater)
        {:success true})
      {:success false})))

(defn- handle-toggle-enabled [payload player]
  (let [tile (open-tile payload player)
        new-enabled (boolean (:enabled payload))]
    (if tile
      (do
        (machine-runtime/commit-transform!
          tile interferer-logic/interferer-default-state
          (fn [state]
            (if new-enabled
              (assoc state :enabled true)
              (assoc state :enabled false :affected-player-count 0 :affected-player-uuids [])))
          :blockstate-updater interferer-logic/interferer-blockstate-updater
          :after-commit! interferer-logic/interferer-after-commit!)
        {:success true})
      {:success false})))

(defn- handle-set-whitelist [payload player]
  (let [tile (open-tile payload player)
        names (:whitelist payload)]
    (if (and tile (sequential? names))
      (do
        (machine-runtime/commit-transform!
          tile interferer-logic/interferer-default-state
          (fn [state] (assoc state :whitelist (normalize-whitelist names)))
          :blockstate-updater interferer-logic/interferer-blockstate-updater
          ;; :sync-client? true — the whitelist is not DataSlot-encodable and
          ;; the vanilla dirty-BE sync may lag a GUI reopen; push the update
          ;; tag so a reopened GUI reads the fresh list from the client BE.
          :sync-client? true)
        {:success true})
      {:success false})))

(defn- handle-add-to-whitelist [payload player]
  (let [tile (open-tile payload player)
        player-name (:player-name payload)]
    (if (and tile (not (str/blank? (str player-name))))
      (let [new-whitelist (normalize-whitelist
                            (conj (vec (or (:whitelist (or (platform-be/get-custom-state tile)
                                                           interferer-logic/interferer-default-state))
                                           []))
                                  player-name))]
        (machine-runtime/commit-transform!
          tile interferer-logic/interferer-default-state
          (fn [state] (assoc state :whitelist new-whitelist))
          :blockstate-updater interferer-logic/interferer-blockstate-updater
          :sync-client? true)
        {:success true})
      {:success false})))

(defn- handle-remove-from-whitelist [payload player]
  (let [tile (open-tile payload player)
        player-name (:player-name payload)]
    (if (and tile (not (str/blank? (str player-name))))
      (do
        (machine-runtime/commit-transform!
          tile interferer-logic/interferer-default-state
          (fn [state]
            (assoc state :whitelist
                   (normalize-whitelist
                     (remove #(= % player-name) (:whitelist state [])))))
          :blockstate-updater interferer-logic/interferer-blockstate-updater
          :sync-client? true)
        {:success true})
      {:success false})))

(defn- get-linked-node-for-interferer [tile]
  (when-let [conn (try (wireless-api/get-node-conn-by-receiver tile) (catch Exception _ nil))]
    (try (node-conn/get-node conn (platform-be/be-get-world-safe tile)) (catch Exception _ nil))))

(defn register-network-handlers! []
  (net-server/register-handler (msg :change-range) handle-change-range)
  (net-server/register-handler (msg :toggle-enabled) handle-toggle-enabled)
  (net-server/register-handler (msg :set-whitelist) handle-set-whitelist)
  (net-server/register-handler (msg :add-to-whitelist) handle-add-to-whitelist)
  (net-server/register-handler (msg :remove-from-whitelist) handle-remove-from-whitelist)
  (wireless-handlers/register-link-handlers!
    {:message-domain :ability-interferer
     :get-linked-node get-linked-node-for-interferer
     :link! wireless-api/link-receiver-to-node!
     :unlink! wireless-api/unlink-receiver-from-node!
     :log-label "Ability Interferer wireless"})
  (log/debug "Ability Interferer network handlers registered"))
