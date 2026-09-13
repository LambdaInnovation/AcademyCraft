(ns cn.li.mcbase.runtime.named-position-store-core
  "Loader-agnostic named world-position NBT helpers.

  Reads go through cn.li.mcver.NbtAccess. Server is passed by the platform adapter."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver NbtAccess]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.nbt CompoundTag]))

(defn- nbt-store-key
  []
  (->> (power-runtime/list-player-persistence-descriptors)
       (filter #(and (= :named-world-position-store (:host-key %))
                     (= :compound-tag (:format %))
                     (:nbt-key %)))
       (sort-by (fn [x] [(long (or (:order x) 0)) (str (:id x))]))
       first
       :nbt-key))

(defn- get-positions-tag
  ^CompoundTag [^ServerPlayer player]
  (when-let [store-key (nbt-store-key)]
    (let [^CompoundTag persistent-data (when-let [fw-atom (fw/fw-atom)]
                                         (platform/call-adapter fw-atom :player-persistent-data :get! player))]
      (if (NbtAccess/contains persistent-data store-key)
        (NbtAccess/getCompound persistent-data store-key)
        (let [new-tag (CompoundTag.)]
          (.put persistent-data store-key new-tag)
          new-tag)))))

(defn- position-to-nbt [world-id x y z]
  (let [tag (CompoundTag.)]
    (.putString tag "world" world-id)
    (.putDouble tag "x" x)
    (.putDouble tag "y" y)
    (.putDouble tag "z" z)
    tag))

(defn- nbt-to-position [location-name ^CompoundTag tag]
  {:name location-name
   :world-id (NbtAccess/getString tag "world")
   :x (NbtAccess/getDouble tag "x")
   :y (NbtAccess/getDouble tag "y")
   :z (NbtAccess/getDouble tag "z")})

(defn save-location!
  [^MinecraftServer server player-uuid location-name world-id x y z]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [positions-tag (get-positions-tag player)]
        (.put positions-tag location-name (position-to-nbt world-id x y z))
        (log/debug "Named world position: saved" location-name "for player" player-uuid)
        true))
    (catch Exception e
      (log/warn "Failed to save named world position:" (ex-message e))
      false)))

(defn delete-location!
  [^MinecraftServer server player-uuid location-name]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [positions-tag (get-positions-tag player)]
        (if (NbtAccess/contains positions-tag location-name)
          (do
            (.remove positions-tag location-name)
            (log/debug "Named world position: deleted" location-name)
            true)
          false)))
    (catch Exception e
      (log/warn "Failed to delete named world position:" (ex-message e))
      false)))

(defn get-location
  [^MinecraftServer server player-uuid location-name]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [positions-tag (get-positions-tag player)]
        (when (NbtAccess/contains positions-tag location-name)
          (nbt-to-position location-name (NbtAccess/getCompound positions-tag location-name)))))
    (catch Exception e
      (log/warn "Failed to get named world position:" (ex-message e))
      nil)))

(defn list-locations
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [positions-tag (get-positions-tag player)]
        (let [keys (NbtAccess/keySet positions-tag)]
          (mapv (fn [key]
                  (nbt-to-position key (NbtAccess/getCompound positions-tag key)))
                keys))))
    (catch Exception e
      (log/warn "Failed to list named world positions:" (ex-message e))
      [])))

(defn get-location-count
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [positions-tag (get-positions-tag player)]
        (.size (NbtAccess/keySet positions-tag))))
    (catch Exception e
      (log/warn "Failed to get named world position count:" (ex-message e))
      0)))

(defn has-location?
  [^MinecraftServer server player-uuid location-name]
  (try
    (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
      (when-let [positions-tag (get-positions-tag player)]
        (NbtAccess/contains positions-tag location-name)))
    (catch Exception e
      (log/warn "Failed to check named world position:" (ex-message e))
      false)))

(defn create-named-position-store
  "Create an INamedPositionStore adapter using a platform-provided server supplier."
  [get-server]
  {:save-location! (fn [player-uuid location-name world-id x y z]
                     (save-location! (get-server) player-uuid location-name world-id x y z))
   :delete-location! (fn [player-uuid location-name]
                       (delete-location! (get-server) player-uuid location-name))
   :get-location (fn [player-uuid location-name]
                   (get-location (get-server) player-uuid location-name))
   :list-locations (fn [player-uuid]
                     (list-locations (get-server) player-uuid))
   :get-location-count (fn [player-uuid]
                         (get-location-count (get-server) player-uuid))
   :has-location? (fn [player-uuid location-name]
                    (has-location? (get-server) player-uuid location-name))})
