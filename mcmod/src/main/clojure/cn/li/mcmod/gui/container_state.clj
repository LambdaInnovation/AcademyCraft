(ns cn.li.mcmod.gui.container-state
  "Platform-neutral container lifecycle state for GUI infrastructure.

  Stores a single `menu → clj-container` map in an atom.
  Registration happens at menu creation (proxy.clj finalize-menu-registration!)
  and cleanup happens at menu removal (proxy.clj remove-menu!), so the atom
  never accumulates stale entries — no memory leak.

  No reflection, no Java field access — pure Clojure."
  (:require [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Atom-based menu → container map
;; ============================================================================

;; Menu containers stored in Framework [:service :container-state :menu-containers]

(def ^:private menu-path [:service :container-state :menu-containers])

(defn- menu-containers-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom menu-path {})
    {}))

(defn get-container-for-menu
  "Return the Clojure container backing a Minecraft menu instance."
  [menu]
  (get (menu-containers-snapshot) menu))

(defn resolve-container-for-menu
  "Resolve a Clojure container for a menu."
  [menu]
  (get-container-for-menu menu))

(defn register-menu-container!
  "Register the Clojure container backing a Minecraft menu instance."
  [menu container]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in menu-path
           (fn [current] (assoc (or current {}) menu container))))
  (log/debug "Registered GUI container for menu" (type menu))
  nil)

(defn unregister-menu-container!
  "Remove menu → Clojure container mapping."
  [menu]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in menu-path
           (fn [current] (dissoc (or current {}) menu))))
  (log/debug "Unregistered GUI container for menu" (type menu))
  nil)

;; ============================================================================
;; Menu → container-id (platform protocol — not stateful)
;; ============================================================================

(defn get-menu-container-id
  "Get a Minecraft menu/container window id via platform protocol."
  [menu]
  (when menu
    (try
      (entity/menu-get-container-id menu)
      (catch Exception _ nil))))

;; ============================================================================
;; owner-from-container (used by panel.clj and content GUIs)
;; ============================================================================

(defn- player-key
  [player]
  (when player
    (some-> (entity/player-get-uuid player) str)))

(defn owner-from-container
  "Resolve an explicit owner map from a Clojure container."
  [container]
  (let [owner (or (:owner container)
                  (select-keys container [:server-session-id :client-session-id :player-uuid :player :owner :logical-side]))
        player (:player owner)
        player-id (or (:player-uuid owner)
                      (:player-uuid container)
                      (some-> player player-key))]
    (cond-> owner
      player (assoc :player player)
      player-id (assoc :player-uuid player-id))))

;; ============================================================================
;; Runtime (kept for test binding compat — no longer used in production)
;; ============================================================================

(defn installed-runtime
  "Return a marker runtime map (state now lives in Framework)."
  []
  {::runtime ::container-state-runtime})

(defn container-state-snapshot
  "Return snapshot for tests/diagnostics."
  []
  {:menu-containers (menu-containers-snapshot)})

;; ============================================================================
;; Cleanup (test teardown)
;; ============================================================================

(defn clear-all!
  "Clear all GUI runtime state. Intended for tests/reloads."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in menu-path {}))
  nil)
