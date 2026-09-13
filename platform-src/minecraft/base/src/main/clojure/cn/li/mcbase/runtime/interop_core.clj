(ns cn.li.mcbase.runtime.interop-core
  "Loader-agnostic runtime interop helpers for world/player queries.

  All operations use only vanilla MC APIs (ServerPlayer, ServerLevel)
  so this works identically on both Forge and Fabric."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver McAccess]
           [net.minecraft.core BlockPos]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerPlayer ServerLevel]))

(defn get-level-by-id
  ^ServerLevel [^MinecraftServer server world-id]
  (try
    (query-core/resolve-level-strict server world-id)
    (catch Exception _
      nil)))

(defn get-player-view
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (let [eye (.getEyePosition player)
            look (.getLookAngle player)
            world-id (some-> (.level player) McAccess/dimensionId)]
        {:world-id (or world-id "minecraft:overworld")
         :x (.x eye)
         :y (.y eye)
         :z (.z eye)
         :look-x (.x look)
         :look-y (.y look)
         :look-z (.z look)}))
    (catch Exception e
      (log/warn "Failed to get player view:" (ex-message e))
      nil)))

(defn get-player-main-hand-item
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (let [stack (.getMainHandItem player)]
        (when (and stack (not (.isEmpty stack)))
          stack)))
    (catch Exception e
      (log/warn "Failed to get player main hand item:" (ex-message e))
      nil)))

(defn get-player-entity
  "Resolve the live ServerPlayer for player-uuid, for callers (e.g. tick-driven
  entity-spawn visuals) that only have a player-id and no player-ref -- the
  generic skill-callback dispatch's positional player-ref argument is never
  populated for server-tick-driven contexts (see context-manager's
  tick-context-entry!, which omits :player from the tick payload)."
  [^MinecraftServer server player-uuid]
  (try
    (query-core/get-player-by-uuid server player-uuid)
    (catch Exception e
      (log/warn "Failed to get player entity:" (ex-message e))
      nil)))

(defn get-block-entity-at
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (.getBlockEntity level (BlockPos. (int x) (int y) (int z))))
    (catch Exception e
      (log/warn "Failed to get block entity:" (ex-message e))
      nil)))

(defn runtime-interop-impl
  "Create an IRuntimeInterop implementation backed by a server supplier."
  [server-fn]
  {:get-player-view (fn [player-uuid]
                      (get-player-view (server-fn) player-uuid))
   :get-player-main-hand-item (fn [player-uuid]
                                (get-player-main-hand-item (server-fn) player-uuid))
   :get-block-entity-at (fn [world-id x y z]
                          (get-block-entity-at (server-fn) world-id x y z))
   :get-player-entity (fn [player-uuid]
                        (get-player-entity (server-fn) player-uuid))})

(defn install-runtime-interop!
  "Install canonical runtime interop using a shared implementation."
  [label server-fn]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter! fw-atom :runtime-interop (runtime-interop-impl server-fn)))
  nil)
