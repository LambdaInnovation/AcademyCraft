(ns cn.li.neoforge1211.integration.events.world
  "Forge world load/unload event handlers."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.event-runtime :as world-lifecycle]
            [cn.li.platform.neutral.event-runtime :as world-save-cache]
            [cn.li.platform.neutral.event-runtime :as world-state-notify]
            [cn.li.platform.neutral.event-runtime :as wok]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mc1211.integration.saveddata.world-lifecycle :as wl-saved])
	  (:import [net.minecraft.server.level ServerLevel]
           [net.neoforged.neoforge.event.level LevelEvent$Load LevelEvent$Unload LevelEvent$Save]
           [net.neoforged.neoforge.event.tick LevelTickEvent$Post]))

(declare register-level-for-state-change-hook! unregister-level-for-state-change-hook!)

(defn handle-world-load
  [^LevelEvent$Load evt]
  (try
    (let [level (.getLevel evt)]
      (when-not (.isClientSide level)
        (log/debug "World loaded, dispatching to lifecycle handlers")
        (register-level-for-state-change-hook! level)
        (let [from-storage (wl-saved/load-world-lifecycle-saved-data level)
              from-cache (world-save-cache/consume-saved-data! level)
              saved (or from-storage from-cache)]
          (world-lifecycle/dispatch-world-load level saved))))
    (catch Throwable t
      (log/stacktrace "Error handling world load event:" t)
      (.printStackTrace t))))

(defn handle-world-save
  [^LevelEvent$Save evt]
  (try
    (let [level (.getLevel evt)]
      (when-not (.isClientSide level)
        (let [saved (world-lifecycle/dispatch-world-save level)]
          (wl-saved/save-world-lifecycle-saved-data! level saved)
          (world-save-cache/remember-saved-data! level saved))))
    (catch Throwable t
      (log/stacktrace "Error handling world save event:" t)
      (.printStackTrace t))))

(defn handle-world-unload
  [^LevelEvent$Unload evt]
  (try
    (let [level (.getLevel evt)]
      (when-not (.isClientSide level)
        (log/debug "World unloading, dispatching to lifecycle handlers")
        (unregister-level-for-state-change-hook! level)
        (world-save-cache/clear-world-saved-data! level)
        (world-lifecycle/dispatch-world-unload level)))
    (catch Throwable t
      (log/stacktrace "Error handling world unload event:" t)
      (.printStackTrace t))))

(defn handle-world-tick
  [^LevelTickEvent$Post evt]
  (try
    (let [level (.getLevel evt)]
      (when (and level
                 (not (.isClientSide level)))
        (world-lifecycle/dispatch-world-tick level)))
    (catch Throwable t
      (log/debug "Error handling world tick event:" (.getMessage t))
      (.printStackTrace t))))

;; Per-world-key → Forge SavedData mapping. Populated on world load, cleared
;; on unload. Used by on-world-state-changed callback to call setDirty()
;; without reflection or MinecraftServer lookup. Lives in Framework
;; [:service :world-lifecycle :saved-data] — always injected by the time any
;; world-load event fires (mod bootstrap completes before Forge can load a level).
(def ^:private saved-data-path [:service :world-lifecycle :saved-data])

(def ^:private ^:const GLOBAL-KEY :global)

(defn- overworld? [^ServerLevel level]
  (= "minecraft:overworld" (str (.location (.dimension level)))))

(defn- register-level-for-state-change-hook! [^ServerLevel level]
  (let [fw-atom (fw/fw-atom)
        sd (wl-saved/get-or-create-saved-data level)
        wk (wok/world-key level)]
    (when (contains? (get-in @fw-atom saved-data-path) wk)
      (log/warn "[forge] Stale SavedData mapping detected for" wk
                "— previous world unload may not have fired cleanly, overwriting"))
    (swap! fw-atom assoc-in (conj saved-data-path wk) sd)
    ;; Global shared data is physically anchored to the overworld — the only
    ;; dimension that is never dynamically unloaded and the last one saved on
    ;; shutdown. Business code writes with world-key = :global; the hook routes
    ;; the dirty signal to the overworld's SavedData.
    (when (overworld? level)
      (swap! fw-atom assoc-in (conj saved-data-path GLOBAL-KEY) sd))))

(defn- unregister-level-for-state-change-hook! [^ServerLevel level]
  (let [fw-atom (fw/fw-atom)
        wk (wok/world-key level)]
    (swap! fw-atom update-in saved-data-path dissoc wk)
    ;; Global data is physically bound to the overworld. When the overworld
    ;; unloads (server shutdown), drop the :global alias so stale references
    ;; don't survive into a new server session.
    (when (overworld? level)
      (swap! fw-atom update-in saved-data-path dissoc GLOBAL-KEY))))

(defn register-on-world-state-changed!
  "Generic hook: subscribes to all world-state mutations (world-registry modules
  and connections, or any future module storing data via world-registry).
  When state changes, calls setDirty() on the WorldLifecycleSavedData
  so the next save cycle picks it up — no LevelEvent$Save timing dependency."
  []
  (world-state-notify/set-on-world-state-changed-fn!
    (fn [world-key]
      (when-let [fw-atom (fw/fw-atom)]
        (when-let [sd (get-in @fw-atom (conj saved-data-path world-key))]
          (.setDirty ^cn.li.mc1211.integration.saveddata.WorldLifecycleSavedData sd)))))
  (log/debug "[forge] on-world-state-changed → SavedData.setDirty() hook registered"))
