(ns cn.li.mcmod.gui.tabbed-gui
  "AC 通用：多页签 TechUI 约定与辅助。

   This is shared client/server tab switching logic used by platform GUI/menu bridges."
  (:require [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.gui.container.sync-routing :as sync-routing]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.runtime.owner :as runtime-owner]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Constants & protocol (platform-agnostic)
;; ============================================================================

(def inv-tab-index
  "Tab index for the inventory (inv-window) page. Only this tab has slot interaction enabled."
  0)

(declare send-set-tab!)

(defn slots-active?
  "True when the current tab is inv-window (slot interaction and highlight should be enabled).
   Call only when container has :tab-index; returns false if :tab-index is missing."
  [container]
  (boolean
   (when (contains? container :tab-index)
     (zero? @(:tab-index container)))))

(defn tabbed-container?
  "True if container supports tab switching (has :tab-index). Used by platform to add DataSlot and conditional slots."
  [container]
  (contains? container :tab-index))

(defn- require-owner-value
  [owner label value]
  (if (some? value)
    value
    (throw (ex-info (format "Tabbed GUI owner requires %s" label)
                    {:owner owner
                     :required label}))))

(defn- player-key
  [player]
  (when player
    (some-> (entity/player-get-uuid player) str)))

(defn- server-owner-for-player
  [player]
  (let [runtime-owner runtime-hooks/player-state-owner
        server-session-id (require-owner-value runtime-owner
                                               ":server-session-id"
                                               (:server-session-id runtime-owner))
        player-id (require-owner-value {:player player}
                                       ":player-uuid"
                                       (some-> player player-key))]
    (runtime-owner/require-server-owner
     {:logical-side :server
      :server-session-id server-session-id
      :player-uuid player-id
      :player player})))

(defn- owner-client-session-id
  [owner]
  (when (runtime-owner/valid-client-owner? owner)
    (:client-session-id owner)))

(defn- send-set-tab-safe!
  [owner tab-index container-id]
  (let [owner-session-id (owner-client-session-id owner)]
    (try
      (if owner-session-id
        (send-set-tab! owner tab-index container-id)
        (log/debug "Skip set-tab sync: missing canonical client owner"
                   {:container-id container-id
                    :tab-index tab-index
                    :owner owner}))
      (catch Exception e
        (log/debug "Skip set-tab sync: send failed"
                  {:container-id container-id
                   :tab-index tab-index
                   :owner owner
                   :reason (ex-message e)})))))

(defn slots-active-for-menu?
  "True when slot clicks should be allowed. Uses container :tab-index (DataSlot authority)."
  [_menu container]
  (slots-active? container))

(defn page-id->index
  "Map page id (e.g. \"inv\", \"panel\") to 0-based tab index from pages sequence.
   pages: seq of {:id string :window widget}"
  [pages id]
  (let [idx (first (keep-indexed (fn [i p] (when (= (:id p) id) i)) pages))]
    (when (integer? idx) idx)))

(defn index->page-id
  "Map 0-based tab index to page id. Returns nil if index out of range."
  [pages index]
  (when (and (>= index 0) (< index (count pages)))
    (:id (nth pages index))))

;; ============================================================================
;; Set-tab C2S handler (server)
;; ============================================================================

(def set-tab-msg-id "set-tab")

(defn- get-player-menu
  "Get player's current open container menu. Tries field 'containerMenu' then method 'getContainerMenu' (Mojang 1.20.1)."
  [player]
  (try
    (entity/player-get-container-menu player)
    (catch Exception _ nil)))

(defn- handle-set-tab
  "Server handler for set-tab: update tab-index on the player's validated open container."
  [payload player]
  (try
    (sync-routing/with-open-container
      payload
      player
      (fn [container _payload _player]
        (when (tabbed-container? container)
          (reset! (:tab-index container) (int (or (:tab-index payload) 0)))
          (log/debug "Set tab-index to" @(:tab-index container) "for player"
                    (entity/player-get-name player)))))
    (catch Exception e
      (log/debug "set-tab rejected:" (ex-message e) {:payload payload})))
  {})

(defn register-set-tab-handler!
  "Register the set-tab C2S handler. Call once during init (e.g. from core)."
  []
  (net-server/register-handler set-tab-msg-id handle-set-tab
                               {:owner-spec :server :payload-routing :sync-routing})
  (log/debug "Registered set-tab network handler"))

(defn send-set-tab!
  "Client: send tab index and required container-id to server."
  ([tab-index container-id]
   (when-not (integer? container-id)
     (throw (ex-info "set-tab requires container-id" {:tab-index tab-index})))
   (net-client/send-to-server set-tab-msg-id
     {:tab-index (int tab-index)
      :container-id (int container-id)}))
  ([owner tab-index container-id]
   (when-not (integer? container-id)
     (throw (ex-info "set-tab requires container-id" {:tab-index tab-index})))
   (net-client/send-to-server owner set-tab-msg-id
     {:tab-index (int tab-index)
      :container-id (int container-id)}
     nil)))

(defn detach-tab-sync!
  "Remove tab-sync watch from tech-ui. Call when closing a screen if the tech-ui atom may outlive the session."
  [tech-ui]
  (when-let [current-atom (:current tech-ui)]
    (remove-watch current-atom :tab-sync)))

(defn attach-tab-sync!
  "Attach generic tab-change sync between a TechUI instance and a container.

   container-id: required integer from `container-state/get-menu-container-id`.
   tech-ui is per-screen; watch is reclaimed with the atom unless cached — use `detach-tab-sync!` on close if reused."
  [pages tech-ui container container-id]
  (when-not (integer? container-id)
    (throw (ex-info "attach-tab-sync! requires container-id" {:container-id container-id})))
  (let [current-atom (:current tech-ui)
        owner (container-state/owner-from-container container)]
    (when (some? current-atom)
      (add-watch current-atom :tab-sync
        (fn [_ _ _ new-id]
          (when-let [idx (page-id->index pages new-id)]
            (send-set-tab-safe! owner idx container-id)))))

    (when-let [cur (and current-atom @current-atom)]
      (when-let [idx (page-id->index pages cur)]
        (when (tabbed-container? container)
          (send-set-tab-safe! owner idx container-id))))))
