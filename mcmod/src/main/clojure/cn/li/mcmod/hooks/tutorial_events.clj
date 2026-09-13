(ns cn.li.mcmod.hooks.tutorial-events
  "Tutorial item-event and activation hook registry.

  State stored in Framework [:service :tutorial-events].

  Registered handlers start empty; readers apply noop fallbacks when content has
  not claimed a slot yet."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private default-on-item-event! (fn [_ _ _] nil))
(def ^:private default-process-pending-activations! (fn [_] nil))
(def ^:private default-tutorial-activated-hook (fn [_ _] nil))

;; ============================================================================
;; State access — Framework [:service :tutorial-events]
;; ============================================================================

(def ^:private hooks-path [:service :tutorial-events])

(defn- registered-handlers
  []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom hooks-path {})
    {}))

(defn register-tutorial-handlers!
  "Register tutorial event handler functions.

  Throws only when a slot was already claimed by content with a different fn."
  [handlers]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in hooks-path
           (fn [current]
             (let [base (or current {})]
               (reduce-kv (fn [m k v]
                            (if (and (contains? m k) (not= (get m k) v))
                              (throw (ex-info "Conflicting tutorial handler"
                                              {:key k :existing (get m k) :new v}))
                              (assoc m k v)))
                          base handlers)))))
  nil)

(defn register-tutorial-activated-hook!
  [hook-fn]
  (register-tutorial-handlers! {:tutorial-activated-hook hook-fn}))

(defn reset-tutorial-events-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in hooks-path {}))
  nil)

(defn on-item-event!
  [player item-id event-type]
  ((or (:on-item-event! (registered-handlers)) default-on-item-event!)
   player item-id event-type))

(defn process-pending-activations!
  [player]
  ((or (:process-pending-activations! (registered-handlers))
       default-process-pending-activations!)
   player))

(defn notify-tutorial-activated!
  [player-uuid tut-id]
  ((or (:tutorial-activated-hook (registered-handlers))
       default-tutorial-activated-hook)
   player-uuid tut-id))
