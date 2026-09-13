(ns cn.li.mcbase.gui.init.orchestrator
  "Shared helper functions for platform GUI init flows."
  (:require [cn.li.mcmod.util.log :as log]))

(defn phase-start!
  [platform-label phase-label]
  (log/debug (str "=== Initializing " platform-label " GUI System (" phase-label ") ===")))

(defn phase-done!
  [platform-label phase-label]
  (log/debug (str "=== " platform-label " GUI System (" phase-label ") Initialized ===")))

(defn safe-init!
  [error-prefix init-fn]
  (try
    (init-fn)
    true
    (catch Exception e
      (log/stacktrace error-prefix e)
      (.printStackTrace e)
      false)))

(defn run-phase!
  [{:keys [platform-label phase-label steps]}]
  (phase-start! platform-label phase-label)
  (doseq [{:keys [run]} steps]
    (run))
  (phase-done! platform-label phase-label)
  true)

(defn safe-run-phase!
  [{:keys [platform-label phase-label] :as phase}]
  (safe-init! (str "Failed to initialize " platform-label " GUI system (" phase-label "):")
              #(run-phase! phase)))

(defn run-phases!
  [phases]
  (every? true? (map run-phase! phases)))

(defn safe-run-phases!
  [phases]
  (every? true? (map safe-run-phase! phases)))

(defn verify-checks!
  [title checks]
  (log/debug title)
  (doseq [[check-name result] checks]
    (log/debug "  " check-name ":" (if result "✓" "✗")))
  (let [all-passed? (every? true? (vals checks))]
    (if all-passed?
      (log/debug "All GUI system checks passed!")
      (log/error "Some GUI system checks failed!"))
    all-passed?))
