(ns cn.li.fabric1201.integration.events.world
  "Fabric world load/unload SavedData wiring (parity with Forge world events)."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.event-runtime :as world-lifecycle]
            [cn.li.platform.neutral.event-runtime :as world-save-cache]
            [cn.li.platform.neutral.event-runtime :as world-state-notify]
            [cn.li.platform.neutral.event-runtime :as wok]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mc1201.integration.saveddata.world-lifecycle :as wl-saved])
  (:import [net.minecraft.server.level ServerLevel]
           [cn.li.mc1201.integration.saveddata WorldLifecycleSavedData]))

(def ^:private saved-data-path [:service :world-lifecycle :saved-data])
(def ^:private ^:const GLOBAL-KEY :global)

(defn- overworld? [^ServerLevel level]
  (= "minecraft:overworld" (str (.location (.dimension level)))))

(defn- register-level-for-state-change-hook! [^ServerLevel level]
  (let [fw-atom (fw/fw-atom)
        sd (wl-saved/get-or-create-saved-data level)
        wk (wok/world-key level)]
    (when (contains? (get-in @fw-atom saved-data-path) wk)
      (log/warn "[fabric] Stale SavedData mapping detected for" wk
                "— previous world unload may not have fired cleanly, overwriting"))
    (swap! fw-atom assoc-in (conj saved-data-path wk) sd)
    (when (overworld? level)
      (swap! fw-atom assoc-in (conj saved-data-path GLOBAL-KEY) sd))))

(defn- unregister-level-for-state-change-hook! [^ServerLevel level]
  (let [fw-atom (fw/fw-atom)
        wk (wok/world-key level)]
    (swap! fw-atom update-in saved-data-path dissoc wk)
    (when (overworld? level)
      (swap! fw-atom update-in saved-data-path dissoc GLOBAL-KEY))))

(defn handle-world-load
  [^ServerLevel world]
  (try
    (register-level-for-state-change-hook! world)
    (let [from-storage (wl-saved/load-world-lifecycle-saved-data world)
          from-cache (world-save-cache/consume-saved-data! world)
          saved (or from-storage from-cache)]
      (world-lifecycle/dispatch-world-load world saved))
    (catch Throwable t
      (log/stacktrace "Error handling Fabric world load:" t)
      (.printStackTrace t))))

(defn handle-world-unload
  [^ServerLevel world]
  (try
    (let [saved (world-lifecycle/dispatch-world-save world)]
      (wl-saved/save-world-lifecycle-saved-data! world saved)
      (world-save-cache/remember-saved-data! world saved))
    (unregister-level-for-state-change-hook! world)
    (world-save-cache/clear-world-saved-data! world)
    (world-lifecycle/dispatch-world-unload world)
    (catch Throwable t
      (log/stacktrace "Error handling Fabric world unload:" t)
      (.printStackTrace t))))

(defn register-on-world-state-changed!
  "When world-registry state mutates, mark SavedData dirty for the next save."
  []
  (world-state-notify/set-on-world-state-changed-fn!
    (fn [world-key]
      (when-let [fw-atom (fw/fw-atom)]
        (when-let [sd (get-in @fw-atom (conj saved-data-path world-key))]
          (.setDirty ^WorldLifecycleSavedData sd)))))
  (log/debug "[fabric] on-world-state-changed → SavedData.setDirty() hook registered"))
