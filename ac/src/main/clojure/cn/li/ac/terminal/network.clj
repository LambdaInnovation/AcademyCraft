(ns cn.li.ac.terminal.network
  "Server-side terminal network handlers."
  (:require [cn.li.ac.terminal.messages :as terminal-messages]
            [cn.li.ac.terminal.model :as model]
            [cn.li.ac.terminal.player :as player]
            [cn.li.ac.terminal.catalog :as catalog]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.achievement.dispatcher :as achievement-dispatcher]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.server.platform-bridge :as server-bridge]))

(defn handle-install-terminal
  [_payload player]
  (if (player/terminal-installed? player)
    {:success false :error :already-installed}
    (try
      (player/install-terminal! player)
      (let [uid (uuid/player-uuid player)]
        (try
          (achievement-dispatcher/trigger-custom-event! uid "terminal_installed")
          (catch Throwable e
            (log/stacktrace "Failed to trigger terminal_installed achievement" e)))
        (try
          (server-bridge/send-to-client! uid (terminal-messages/msg-id :terminal-install-effect) {})
          (catch Throwable e
            (log/stacktrace "Failed to send terminal install effect" e))))
      {:success true}
      (catch Exception e
        (log/stacktrace "Error installing terminal" e)
        {:success false :error (ex-message e)}))))

(defn handle-install-app
  [payload player]
  (try
    (let [app-id (keyword (:app-id payload))]
      (if (catalog/app-exists? app-id)
        (do
          (player/install-app! player app-id)
          {:success true :app-id app-id})
        {:success false :error "App not found"}))
    (catch Exception e
      (log/stacktrace "Error installing app" e)
      {:success false :error (ex-message e)})))

(defn handle-uninstall-app
  [payload player]
  (try
    (let [app-id (keyword (:app-id payload))]
      (player/uninstall-app! player app-id)
      {:success true :app-id app-id})
    (catch Exception e
      (log/stacktrace "Error uninstalling app" e)
      {:success false :error (ex-message e)})))

(defn handle-get-state
  [_payload player]
  (try
    (let [terminal-data (player/state player)
          normalized (model/normalize-state terminal-data)
          explicit-installed (:installed-apps normalized)
          ;; Include pre-installed apps (matching original isPreInstalled)
          pre-installed (set (filter #(:pre-installed? (catalog/app-by-id %))
                                     (catalog/app-ids)))
          all-installed (into pre-installed explicit-installed)]
      {:terminal-installed? (:terminal-installed? normalized)
       ;; The wire response is stable and follows the terminal's AppRegistry
       ;; order. Consumers may still store membership as a set.
       :installed-apps (mapv :id
                             (catalog/installed-apps-in-display-order all-installed))
       :available-apps (catalog/app-ids)
       :app-count (catalog/app-count)})
    (catch Exception e
      (log/stacktrace "Error getting terminal state" e)
      {:error (ex-message e)})))

(defn register-handlers!
  []
  ;; Terminal is a reactive overlay, not an open container GUI — payloads are
  ;; self-contained (app-id, empty get-state, etc.) and must not require
  ;; :container-id sync-routing validation.
  (let [contract {:owner-spec :server :payload-routing :none}]
    (net-server/register-handler (terminal-messages/msg-id :install-terminal) handle-install-terminal contract)
    (net-server/register-handler (terminal-messages/msg-id :install-app) handle-install-app contract)
    (net-server/register-handler (terminal-messages/msg-id :uninstall-app) handle-uninstall-app contract)
    (net-server/register-handler (terminal-messages/msg-id :get-state) handle-get-state contract))
  (log/debug "Terminal network handlers registered"))
