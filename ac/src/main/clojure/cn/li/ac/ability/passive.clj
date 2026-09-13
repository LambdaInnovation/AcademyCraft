(ns cn.li.ac.ability.passive
  "Helpers for passive skill calc-event wiring.

  Registry stored in Framework [:registry :handlers :passive]."
  (:require [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.framework :as fw]))

(defn- resolve-session-id []
  (runtime-hooks/require-player-state-session-id "passive"))

(defn- runtime-player-state [uuid]
  (store/get-player-state (resolve-session-id) uuid))

(defn- runtime-player-state-in-session [session-id uuid]
  (store/get-player-state session-id uuid))

;; Registry — Framework [:registry :handlers :passive]

(def ^:private passive-path [:registry :handlers :passive])

(defn- passive-handler-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom passive-path {:registered-handlers #{} :frozen? false})
    {:registered-handlers #{} :frozen? false}))

(defn- update-passive-handler-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in passive-path
           (fn [current] (apply f (or current {:registered-handlers #{} :frozen? false}) args))))
  nil)

(defn- assert-registry-open! []
  (when (:frozen? (passive-handler-state-snapshot))
    (throw (ex-info "Passive handler registry is frozen" {}))))

(defn passive-handler-registry-snapshot []
  (:registered-handlers (passive-handler-state-snapshot)))

(defn reset-passive-handler-registry-for-test!
  ([]
   (reset-passive-handler-registry-for-test! #{}))
  ([snapshot]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in passive-path {:registered-handlers (or snapshot #{}) :frozen? false}))
   nil))

(defn freeze-passive-handler-registry! []
  (update-passive-handler-state! assoc :frozen? true)
  nil)

(declare learned-skill-in-session?)

(defn learned-skill? [uuid skill-id]
  (learned-skill-in-session? (resolve-session-id) uuid skill-id))

(defn learned-skill-in-session? [session-id uuid skill-id]
  (boolean
    (when-let [state (runtime-player-state-in-session session-id uuid)]
      (adata/is-learned? (:ability-data state) skill-id))))

(defn register-passive-calc-handler!
  "Register a calc-event transform for one passive skill."
  [handler-id event-type skill-id transform-fn]
  (when (keyword? handler-id)
    (if (contains? (:registered-handlers (passive-handler-state-snapshot)) handler-id)
      nil
      (do
        (assert-registry-open!)
        (update-passive-handler-state! update :registered-handlers conj handler-id)
        (evt/subscribe-ability-event!
          event-type
          (fn [event]
            (let [uuid (:uuid event)
                  value (double (or (:value event) 0.0))]
              (if (and uuid (learned-skill? uuid skill-id))
                (double (transform-fn value event))
                value))))
        true))))
