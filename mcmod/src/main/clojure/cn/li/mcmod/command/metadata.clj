(ns cn.li.mcmod.command.metadata
  "Command metadata system - single source of truth for command registration.

  This module provides metadata-driven command registration, ensuring platform
  code does not contain hardcoded command names. All registration information
  is dynamically retrieved from the command DSL system.

  State stored in Framework [:registry :commands :metadata]."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]))

(def ^:private cmd-path [:registry :commands :metadata])

(defn- command-registry-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom cmd-path {})
    {}))

(defn- resolve-command-registry []
  (command-registry-snapshot))

(defn register-command-registry!
  "Register/replace the full command registry map provided by game content."
  [registry]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in cmd-path (or registry {})))
  nil)

;; ============================================================================
;; Command Metadata Queries
;; ============================================================================

(defn get-all-command-ids []
  (keys (resolve-command-registry)))

(defn get-command-spec [command-id]
  (get (resolve-command-registry) command-id))

(defn get-command-name [command-id]
  (when-let [spec (get-command-spec command-id)]
    (:name spec)))

(defn get-command-permission-level [command-id]
  (when-let [spec (get-command-spec command-id)]
    (:permission-level spec)))

(defn get-command-arguments [command-id]
  (when-let [spec (get-command-spec command-id)]
    (:arguments spec)))

(defn get-command-executor [command-id]
  (when-let [spec (get-command-spec command-id)]
    (:executor-fn spec)))

(defn get-command-description [command-id]
  (when-let [spec (get-command-spec command-id)]
    (:description spec)))

(defn get-subcommands [command-id]
  (when-let [spec (get-command-spec command-id)]
    (:subcommands spec)))

(defn has-subcommands? [command-id]
  (boolean (get-subcommands command-id)))

(defn get-all-subcommand-ids [command-id]
  (keys (or (get-subcommands command-id) {})))

(defn get-subcommand-spec [command-id subcommand-name]
  (when-let [subcommands (get-subcommands command-id)]
    (get subcommands subcommand-name)))

(defn clear-command-registry!
  "Clear command registry. Intended for tests."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in cmd-path {}))
  nil)
