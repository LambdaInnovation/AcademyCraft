(ns cn.li.ac.ability.client.runtime
  "AC-side runtime bridge for client key events.

  Keeps AC independent from Minecraft classes while delegating to platform runtime
  when available."
  (:require [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as power-runtime]))

(defn on-slot-key-down!
  [player-uuid key-idx]
  (client-bridge/on-slot-key-down! player-uuid key-idx))

(defn on-slot-key-tick!
  [player-uuid key-idx]
  (client-bridge/on-slot-key-tick! player-uuid key-idx))

(defn on-slot-key-up!
  [player-uuid key-idx]
  (client-bridge/on-slot-key-up! player-uuid key-idx))

(defn on-slot-key-abort!
  [player-uuid key-idx]
  (client-bridge/on-slot-key-abort! player-uuid key-idx))

(defn on-movement-key-down!
  [player-uuid movement-key]
  (client-bridge/on-movement-key-down! player-uuid movement-key))

(defn on-movement-key-tick!
  [player-uuid movement-key]
  (client-bridge/on-movement-key-tick! player-uuid movement-key))

(defn on-movement-key-up!
  [player-uuid movement-key]
  (client-bridge/on-movement-key-up! player-uuid movement-key))

(defn railgun-charge-visual-state
  [player-uuid now-ms]
  (power-runtime/client-visual-state :ac/charge-coin {:player-uuid player-uuid :now-ms now-ms}))
