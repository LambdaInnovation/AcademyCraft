(ns cn.li.mc1211.integration.saveddata.world-lifecycle
  (:require [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mc1211.integration.saveddata WorldLifecycleSavedData]
           [net.minecraft.nbt CompoundTag]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.level.storage DimensionDataStorage]))

(defn- id->key
  [id]
  (cond
    (keyword? id)
    (if-let [ns (namespace id)]
      (str ns "/" (name id))
      (name id))
    :else
    (str id)))

(defn- key->id
  [^String k]
  (try
    (keyword k)
    (catch Exception _
      k)))

(defn get-or-create-saved-data
  ^WorldLifecycleSavedData
  [^ServerLevel level]
  (let [^DimensionDataStorage storage (.getDataStorage level)]
    ;; 1.21.1: computeIfAbsent(SavedData.Factory, String) — load takes HolderLookup.Provider
    (.computeIfAbsent storage (WorldLifecycleSavedData/factory) WorldLifecycleSavedData/NAME)))

(defn load-world-lifecycle-saved-data
  "Return saved map of handler-id -> CompoundTag, or nil when empty."
  [^ServerLevel level]
  (try
    (let [^WorldLifecycleSavedData sd (get-or-create-saved-data level)
          ^CompoundTag handlers (.getHandlers sd)
          m (into {}
                  (map (fn [^String k]
                         [(key->id k) (.getCompound handlers k)])
                       (.getAllKeys handlers)))]
      (when (seq m) m))
    (catch Throwable t
      (log/stacktrace "Failed to load world lifecycle SavedData:" t)
      nil)))

(defn save-world-lifecycle-saved-data!
  "Persist handler-id -> CompoundTag map into SavedData.
  setDirty will be picked up by the next auto-save or normal save cycle."
  [^ServerLevel level saved-map]
  (try
    (let [^WorldLifecycleSavedData sd (get-or-create-saved-data level)
          ^CompoundTag handlers (CompoundTag.)]
      (doseq [[id payload] saved-map]
        (when (instance? CompoundTag payload)
          (.put handlers (id->key id) ^CompoundTag payload)))
      (.setHandlers sd handlers))
    (catch Throwable t
      (log/stacktrace "Failed to save world lifecycle SavedData:" t)))
  nil)
