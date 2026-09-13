(ns cn.li.mcbase.runtime.teleportation-core
  "Loader-agnostic teleportation helpers.

  Teleports go through cn.li.mcver.TeleportAccess; dimension ids via McAccess."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver McAccess TeleportAccess]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel ServerPlayer]
           [net.minecraft.world.entity Entity LivingEntity]
           [net.minecraft.world.phys AABB]))

(defn get-level
  ^ServerLevel [^MinecraftServer server world-id]
  (try
    (query-core/resolve-level-strict server world-id)
    (catch Exception e
      (log/warn "Failed to get level:" world-id (ex-message e))
      nil)))

(defn- teleport-to!
  [^Entity entity ^ServerLevel target-level x y z]
  (TeleportAccess/teleportPreservingRotation entity target-level (double x) (double y) (double z)))

(defn teleport-player!
  [^MinecraftServer server player-uuid world-id x y z]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [^ServerLevel target-level (get-level server world-id)]
        (when (.isPassenger player)
          (.stopRiding player))
        (teleport-to! player target-level x y z)
        true))
    (catch Exception e
      (log/warn "Failed to teleport player:" (ex-message e))
      false)))

(defn- entity-small-enough? [^Entity entity]
  (< (* (.getBbWidth entity) (.getBbWidth entity) (.getBbHeight entity)) 80.0))

(defn- teleport-entity-relative!
  [^Entity entity ^ServerLevel target-level tx ty tz dx dy dz]
  (try
    (when (.isPassenger entity)
      (.stopRiding entity))
    (teleport-to! entity target-level (+ tx dx) (+ ty dy) (+ tz dz))
    (catch Exception e
      (log/debug "Failed to teleport entity:" (ex-message e))
      false)))

(defn teleport-with-entities!
  [^MinecraftServer server player-uuid world-id x y z radius]
  (try
    (if-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (if-let [^ServerLevel target-level (get-level server world-id)]
        (let [^ServerLevel current-level (.level player)
              player-pos (.position player)
              px (.x player-pos)
              py (.y player-pos)
              pz (.z player-pos)
              aabb (AABB. (- px radius) (- py radius) (- pz radius)
                          (+ px radius) (+ py radius) (+ pz radius))
              nearby (->> (.getEntities current-level nil aabb)
                          (filter #(instance? LivingEntity %))
                          (filter #(not= ^Entity % player))
                          (filter entity-small-enough?))
              teleported-count (long-array 1)]
          (when (.isPassenger player)
            (.stopRiding player))
          (teleport-to! player target-level x y z)
          (aset-long teleported-count 0 1)
          (doseq [^Entity entity nearby]
            (let [epos (.position entity)
                  dx (- (.x epos) px)
                  dy (- (.y epos) py)
                  dz (- (.z epos) pz)]
              (when (teleport-entity-relative! entity target-level x y z dx dy dz)
                (aset-long teleported-count 0 (unchecked-inc (aget teleported-count 0))))))
          {:success true :teleported-count (aget teleported-count 0)})
        {:success false :teleported-count 0})
      {:success false :teleported-count 0})
    (catch Exception e
      (log/warn "Failed to teleport with entities:" (ex-message e))
      {:success false :teleported-count 0})))

(defn reset-fall-damage!
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (.resetFallDistance player)
      true)
    (catch Exception e
      (log/warn "Failed to reset fall damage:" (ex-message e))
      false)))

(defn get-player-position
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (let [pos (.position player)
            world-id (McAccess/dimensionId (.level player))]
        {:world-id world-id
         :x (.x pos)
         :y (.y pos)
         :z (.z pos)}))
    (catch Exception e
      (log/warn "Failed to get player position:" (ex-message e))
      nil)))

(defn get-player-dimension
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (McAccess/dimensionId (.level player)))
    (catch Exception e
      (log/warn "Failed to get player dimension:" (ex-message e))
      nil)))

(defn create-teleportation
  "Create an ITeleportation adapter using a platform-provided server supplier."
  [get-server]
  {:teleport-player! (fn [player-uuid world-id x y z]
                       (teleport-player! (get-server) player-uuid world-id x y z))
   :teleport-with-entities! (fn [player-uuid world-id x y z radius]
                              (teleport-with-entities! (get-server) player-uuid world-id x y z radius))
   :reset-fall-damage! (fn [player-uuid]
                         (reset-fall-damage! (get-server) player-uuid))
   :get-player-position (fn [player-uuid]
                          (get-player-position (get-server) player-uuid))
   :get-player-dimension (fn [player-uuid]
                           (get-player-dimension (get-server) player-uuid))})
