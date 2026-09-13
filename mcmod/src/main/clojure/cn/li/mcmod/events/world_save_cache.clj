(ns cn.li.mcmod.events.world-save-cache
  "Runtime component for carrying world-save payloads across
  world save/unload and the next matching world load.

  State stored in Framework [:service :world-save-cache]."
  (:require [cn.li.mcmod.events.world-owner-key :as world-owner-key]
            [cn.li.mcmod.framework :as fw]))

(defn world-key
  [world]
  (world-owner-key/world-key world))

;; ============================================================================
;; State access — Framework [:service :world-save-cache]
;; ============================================================================

(def ^:private cache-path [:service :world-save-cache])

(defn- pending-save-data-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom cache-path {})
    {}))

(defn- update-pending-save-data! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in cache-path
           (fn [current]
             (apply f (or current {}) args))))
  nil)

;; ============================================================================
;; Public API
;; ============================================================================

(defn consume-saved-data!
  [world]
  (let [wid (world-key world)]
    (when-let [fw-atom (fw/fw-atom)]
      (let [pending (get-in @fw-atom cache-path {})
            saved (get pending wid)]
        (swap! fw-atom update-in cache-path dissoc wid)
        saved))))

(defn remember-saved-data!
  [world saved-data]
  (if (seq saved-data)
    (update-pending-save-data! assoc (world-key world) saved-data)
    (update-pending-save-data! dissoc (world-key world)))
  nil)

(defn clear-world-saved-data!
  [world]
  (update-pending-save-data! dissoc (world-key world))
  nil)

(defn clear-session-saved-data!
  [owner-or-session-id]
  (let [session-id (if (map? owner-or-session-id)
                     (first (world-key owner-or-session-id))
                     owner-or-session-id)]
    (update-pending-save-data!
      (fn [pending]
        (into {}
              (remove (fn [[[entry-session-id _world-id] _saved-data]]
                        (= session-id entry-session-id))
                      pending))))
    nil))

(defn world-save-cache-snapshot
  []
  (pending-save-data-snapshot))

(defn reset-world-save-cache-for-test!
  ([]
   (reset-world-save-cache-for-test! {}))
  ([pending-save-data]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in cache-path (or pending-save-data {})))
   nil))
