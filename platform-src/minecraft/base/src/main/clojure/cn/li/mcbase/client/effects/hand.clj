(ns cn.li.mcbase.client.effects.hand
  "Shared client hand-effect helpers for Minecraft 1.20.1."
  (:require [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as power-runtime])
  (:import [net.minecraft.client.player LocalPlayer]))

(defn tick-hand-effects!
  []
  (client-session/with-current-client-session
    #(power-runtime/client-tick-hand-effects!)))

(defn apply-camera-pitch-deltas!
  [^LocalPlayer player]
  (when (and player (client-session/current-local-player-owner))
    (doseq [delta (power-runtime/client-drain-camera-pitch-deltas!
                    (client-session/current-local-player-owner))]
      (.setXRot player (+ (.getXRot player) (float delta))))))

(defn tick-and-apply-camera!
  [^LocalPlayer player]
  (tick-hand-effects!)
  (apply-camera-pitch-deltas! player))

(defn current-hand-transform
  []
  (client-session/with-current-client-session
    #(power-runtime/client-current-hand-transform)))
