(ns cn.li.mcbase.runtime.player-motion-core
  "Shared Minecraft-side player motion helpers (no loader API imports)."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server MinecraftServer]
           [net.minecraft.world.entity Entity]
           [net.minecraft.server.level ServerPlayer]))

(defn set-velocity-for-player!
  [^ServerPlayer player x y z]
  (when player
    (.setDeltaMovement player x y z)
    (set! (.-hurtMarked ^Entity player) true)
    true))

(defn add-velocity-for-player!
  [^ServerPlayer player x y z]
  (when player
    (let [current (.getDeltaMovement player)]
      (.setDeltaMovement player
                         (+ (.x current) x)
                         (+ (.y current) y)
                         (+ (.z current) z))
      (set! (.-hurtMarked ^Entity player) true)
      true)))

(defn get-velocity-for-player
  [^ServerPlayer player]
  (when player
    (let [vel (.getDeltaMovement player)]
      {:x (.x vel)
       :y (.y vel)
       :z (.z vel)})))

(defn set-on-ground-for-player!
  [^ServerPlayer player on-ground?]
  (when player
    (.setOnGround player (boolean on-ground?))
    true))

(defn is-on-ground-for-player?
  [^ServerPlayer player]
  (boolean (and player (.onGround player))))

(defn dismount-riding-for-player!
  [^ServerPlayer player]
  (when player
    (when (.isPassenger player)
      (.stopRiding player))
    true))

(defn can-fly-for-player?
  [^ServerPlayer player]
  (boolean
   (and player
        (.-mayfly (.getAbilities player)))))

(defn set-can-fly-for-player!
  [^ServerPlayer player can-fly?]
  (when player
    (set! (.-mayfly (.getAbilities player)) (boolean can-fly?))
    (.onUpdateAbilities player)
    true))

(defn create-player-motion
  "Create an IPlayerMotion adapter using a platform-provided server supplier."
  [get-server]
  {:set-velocity! (fn [player-id x y z]
                    (boolean (set-velocity-for-player! (query-core/get-player-by-uuid (get-server) player-id) x y z)))
   :add-velocity! (fn [player-id x y z]
                    (boolean (add-velocity-for-player! (query-core/get-player-by-uuid (get-server) player-id) x y z)))
   :get-velocity (fn [player-id]
                   (get-velocity-for-player (query-core/get-player-by-uuid (get-server) player-id)))
   :set-on-ground! (fn [player-id on-ground?]
                     (boolean (set-on-ground-for-player! (query-core/get-player-by-uuid (get-server) player-id) on-ground?)))
   :is-on-ground? (fn [player-id]
                    (is-on-ground-for-player? (query-core/get-player-by-uuid (get-server) player-id)))
   :dismount-riding! (fn [player-id]
                       (boolean (dismount-riding-for-player! (query-core/get-player-by-uuid (get-server) player-id))))
   :can-fly? (fn [player-id]
               (can-fly-for-player? (query-core/get-player-by-uuid (get-server) player-id)))
   :set-can-fly! (fn [player-id can-fly?]
                   (boolean
                    (set-can-fly-for-player!
                     (query-core/get-player-by-uuid (get-server) player-id)
                     can-fly?)))})
