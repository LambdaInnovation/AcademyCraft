(ns cn.li.mcbase.client.runtime.input-mode-core
  "Shared input-mode runtime core facade.

  Provides a stable runtime namespace while delegating to the existing
  mode-switch state machine implementation."
  (:require [cn.li.mcbase.client.input.mode-switch :as mode-switch]))

(defn initial-state
  []
  (mode-switch/initial-state))

(defn handle-button-state!
  [state-atom is-down opts]
  (mode-switch/handle-button-state! state-atom is-down opts))
