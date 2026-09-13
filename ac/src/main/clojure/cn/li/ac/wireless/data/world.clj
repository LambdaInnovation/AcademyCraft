(ns cn.li.ac.wireless.data.world
  "Wireless world lifecycle and saved-data integration.

  Owns `WiWorldData` load/save/tick/unload only. Reads use `wireless.data.network-lookup`
  or `wireless.service.queries`; writes use `wireless.service.commands`."
  (:require [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.data.persistence :as persistence]
            [cn.li.ac.wireless.data.world-runtime :as runtime]
            [cn.li.mcmod.events.world-lifecycle :as world-lifecycle]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Saved-data wrapper (plain map)
;; ============================================================================

(defn create-wi-saved-data
  "Create a saved data wrapper map containing WiWorldData and optional NBT payload."
  [wi-data wi-nbt]
  {:wi-data wi-data :wi-nbt wi-nbt})

;; ============================================================================
;; World data lifecycle
;; ============================================================================

(defn create-world-data
  "Create new world data for a world."
  [world]
  (world-registry/create-world-data world))

(defn get-world-data
  "Get world data for a world, creating it if missing."
  [world]
  (world-registry/get-world-data world))

(defn get-world-data-non-create
  "Get world data without creating."
  [world]
  (world-registry/get-world-data-non-create world))

(defn register-world-data!
  "Register a world -> WiWorldData mapping in the registry."
  [world wi-data]
  (world-registry/register-world-data! world wi-data))

(defn remove-world-data!
  "Remove world data (called on world unload)."
  [world]
  (world-registry/remove-world-data! world))

(defn create-saved-data
  "Create a saved data wrapper for a world."
  [world]
  (create-wi-saved-data (create-world-data world) nil))

(defn get-saved-data-world-data
  "Extract WiWorldData from SavedData wrapper."
  ([saved-data]
   (get-saved-data-world-data nil saved-data))
  ([world saved-data]
   (when saved-data
     (cond
       ;; Wrapper map — detected by its keys, not by map?, so a bare NBT
       ;; payload that happens to be a Clojure map is not mistaken for it.
       (and (map? saved-data)
            (or (contains? saved-data :wi-data) (contains? saved-data :wi-nbt)))
       (let [payload (:wi-nbt saved-data)]
         (if payload
           (when world (persistence/world-data-from-nbt world payload))
           (:wi-data saved-data)))

       world
       (persistence/world-data-from-nbt world saved-data)

       :else
       nil))))

(defn on-world-load
  "Called when world loads - restore from saved data."
  [world saved-data]
  (if saved-data
    (if-let [wi-data (get-saved-data-world-data world saved-data)]
      (let [registered (register-world-data! world wi-data)
            net-count (count (world-registry/transient-value
                               registered world-registry/network-rebuild-queue-key))
            conn-count (count (world-registry/transient-value
                                 registered world-registry/connection-rebuild-queue-key))]
        (log/debug "[on-world-load] Queued from save for budgeted rebuild:"
                   net-count "networks," conn-count "connections")
        registered)
      (let [fresh (create-world-data world)]
        (log/debug "No saved WiWorldData found, created fresh world data")
        (register-world-data! world fresh)))
    (let [fresh (create-world-data world)]
      (log/debug "No saved-data provided, created fresh world data")
      (register-world-data! world fresh))))

(defn on-world-save
  "Called before world save - prepare data for serialization."
  [world]
  (when-let [wi-data (get-world-data-non-create world)]
    (runtime/network-impl-validator wi-data world)
    (runtime/node-connection-impl-validator wi-data world)
    (let [net-count (count (world-registry/networks wi-data))
          conn-count (count (world-registry/connections wi-data))
          nbt-data (persistence/world-data-to-nbt wi-data world)]
      (log/debug "[on-world-save] Saving" net-count "networks," conn-count "connections")
      nbt-data)))

(defn on-world-tick
  "Called each server world tick - advance wireless runtime state."
  [world]
  (when-let [wi-data (get-world-data-non-create world)]
    (runtime/tick-world-data! wi-data world)
    nil))

(defn on-world-unload
  "Called when world unloads - cleanup."
  [world]
  (remove-world-data! world)
  (log/debug "Cleaned up WiWorldData for unloaded world"))

(defn init-world-data! []
  (log/debug "Registering wireless world data lifecycle handlers...")
  (world-lifecycle/register-world-lifecycle-handler!
    {:id :ac/wireless-world-data
     :on-load on-world-load
     :on-unload on-world-unload
     :on-save on-world-save})
  (log/debug "World data system initialized"))
