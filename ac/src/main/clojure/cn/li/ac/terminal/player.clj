(ns cn.li.ac.terminal.player
  "Server-side terminal player state — persisted directly to player NBT.

  Mirrors upstream AcademyCraft TerminalData: independent NBT key per domain,
  no runtime-store dependency.  Uses player.getPersistentData() — Minecraft
  handles save/load automatically."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.terminal.model :as model]
            [cn.li.ac.persistence.nbt-collections :as nbt-coll]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.util.log :as log]))

(def ^:private nbt-key "ac_terminal_v2")
(def ^:private schema-version 2)

(defn- player-persistent-data
  [player]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :player-persistent-data :get! player)))

;; --- NBT helpers ---

(defn- load-state
  [tag]
  (when (sd/has-key-safe? tag nbt-key)
    (let [root (sd/get-structured tag nbt-key)]
      (when (= schema-version (sd/get-int root "schema"))
        {:terminal-installed? (sd/get-boolean root "installed")
         :installed-apps (nbt-coll/read-keyword-set root "apps")}))))

(defn- save-state!
  [tag state]
  (let [state (model/normalize-state state)
        root (sd/create-structured)]
    (sd/set-int! root "schema" schema-version)
    (sd/set-boolean! root "installed" (:terminal-installed? state))
    (nbt-coll/write-keyword-set! root "apps" (:installed-apps state))
    (sd/set-entry! tag nbt-key root)))

;; --- Public API ---

(defn state
  [player]
  (or (load-state (player-persistent-data player))
      (model/fresh-state)))

(defn update-state!
  [player f & args]
  (let [tag (player-persistent-data player)
        current (or (load-state tag) (model/fresh-state))
        new-state (apply f current args)]
    (save-state! tag new-state)
    new-state))

;; --- Queries ---

(defn terminal-installed?
  [player]
  (model/terminal-installed? (state player)))

(defn installed-apps
  [player]
  (:installed-apps (model/normalize-state (state player))))

(defn app-installed?
  [player app-id]
  (model/app-installed? (state player) app-id))

;; --- Mutations ---

(defn install-terminal!
  [player]
  (log/info "Installing terminal for player:" (str (uuid/player-uuid player)))
  (update-state! player model/install-terminal))

(defn uninstall-terminal!
  [player]
  (log/info "Uninstalling terminal for player:" (str (uuid/player-uuid player)))
  (update-state! player model/uninstall-terminal))

(defn install-app!
  [player app-id]
  (log/info "Installing app" app-id "for player:" (str (uuid/player-uuid player)))
  (update-state! player model/install-app app-id))

(defn uninstall-app!
  [player app-id]
  (log/info "Uninstalling app" app-id "for player:" (str (uuid/player-uuid player)))
  (update-state! player model/uninstall-app app-id))

(defn install-apps!
  [player app-ids]
  (log/info "Installing apps" app-ids "for player:" (str (uuid/player-uuid player)))
  (update-state! player model/install-apps app-ids))
