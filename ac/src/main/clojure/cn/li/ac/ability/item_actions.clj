(ns cn.li.ac.ability.item-actions
  "Registry-based item-action dispatch.

  Content skills register item-action handlers at load time.
  Platform code queries this registry to resolve and execute item actions.

  No Minecraft imports."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Item → action resolution
;; ============================================================================

;; Registry — Framework [:registry :handlers :item-actions]

(def ^:private ia-path [:registry :handlers :item-actions])

(defn- default-state []
  {:item-actions {} :action-handlers {} :item-entity-spawns {} :frozen? false})

(defn- item-action-registries-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom ia-path (default-state))
    (default-state)))

(defn- update-item-action-registries-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in ia-path
           (fn [current] (apply f (or current (default-state)) args))))
  nil)

(defn- assert-registries-open! []
  (when (:frozen? (item-action-registries-state-snapshot))
    (throw (ex-info "Item action registries are frozen" {}))))

(defn item-action-registries-snapshot []
  (item-action-registries-state-snapshot))

(defn reset-item-action-registries-for-test!
  ([]
   (reset-item-action-registries-for-test! {}))
  ([{:keys [item-actions action-handlers item-entity-spawns frozen?]
     :or {item-actions {} action-handlers {} item-entity-spawns {} frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in ia-path {:item-actions item-actions
                                      :action-handlers action-handlers
                                      :item-entity-spawns item-entity-spawns
                                      :frozen? frozen?}))
   nil))

(defn freeze-item-action-registries! []
  (update-item-action-registries-state! assoc :frozen? true))

(defn register-item-action!
  "Map an item id (e.g. \"ac:coin\") to an action keyword (e.g. :railgun-coin-throw)."
  [item-id action-keyword]
  (if-let [existing (get (:item-actions (item-action-registries-state-snapshot)) item-id)]
    (when-not (= existing action-keyword)
      (throw (ex-info "Conflicting item action id"
                      {:item-id item-id :existing existing :new action-keyword})))
    (do
      (assert-registries-open!)
      (update-item-action-registries-state! assoc-in [:item-actions item-id] action-keyword)))
  nil)

(defn resolve-item-action
  "Return the action keyword for `item-id`, or nil."
  [item-id]
  (get (:item-actions (item-action-registries-state-snapshot)) item-id))

;; ============================================================================
;; Action → handler dispatch
;; ============================================================================

(defn register-action-handler!
  "Register a handler fn for an action keyword."
  [action-keyword handler-fn]
  (when-not (contains? (:action-handlers (item-action-registries-state-snapshot)) action-keyword)
    (assert-registries-open!)
    (update-item-action-registries-state! assoc-in [:action-handlers action-keyword] handler-fn))
  nil)

;; ============================================================================
;; Item → entity-spawn spec
;; ============================================================================

(defn register-item-entity-spawn!
  "Register a scripted-effect entity to spawn when `item-id` is used.
  `entity-spec` is a map with :entity-id (String) and optional :speed (double)."
  [item-id entity-spec]
  (if-let [existing (get (:item-entity-spawns (item-action-registries-state-snapshot)) item-id)]
    (when-not (= existing entity-spec)
      (throw (ex-info "Conflicting item entity spawn id"
                      {:item-id item-id :existing existing :new entity-spec})))
    (do
      (assert-registries-open!)
      (update-item-action-registries-state! assoc-in [:item-entity-spawns item-id] entity-spec)))
  nil)

(defn get-item-entity-spawn
  "Return the entity-spawn spec for `item-id`, or nil."
  [item-id]
  (get (:item-entity-spawns (item-action-registries-state-snapshot)) item-id))

(defn on-item-action!
  "Dispatch `action` to its registered handler.  Returns nil if no handler."
  [action player-uuid payload]
  (when-let [handler (get (:action-handlers (item-action-registries-state-snapshot)) action)]
    (try
      (handler player-uuid payload)
      (catch Exception e
        (log/warn "Item action handler" action "failed:" (ex-message e))
        nil))))

(defn reset-item-action-registries!
  "Clear all item-action registries. For REPL and clojure.test isolation only."
  []
  (reset-item-action-registries-for-test!))
