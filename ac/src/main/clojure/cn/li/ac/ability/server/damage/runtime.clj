(ns cn.li.ac.ability.server.damage.runtime
  "AC-owned damage interception runtime.

  Owns handler registration order and damage result normalization so forge only
  forwards platform events and writes the final amount back to the event.

  State stored in Framework [:service :damage-handler-registry]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn default-damage-handler-registry-runtime-state []
  {:handlers {} :frozen? false})

(def ^:private dmg-path [:service :damage-handler-registry])

(defn- damage-handler-registry-state-atom []
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom dmg-path)
        (let [a (atom (default-damage-handler-registry-runtime-state))]
          (swap! fw-atom assoc-in dmg-path a) a))
    (atom (default-damage-handler-registry-runtime-state))))

(defn- damage-handler-registry-state-snapshot [] @(damage-handler-registry-state-atom))
(defn- update-damage-handler-registry-state! [f & args] (apply swap! (damage-handler-registry-state-atom) f args))
(defn- assert-registry-open! [] (when (:frozen? (damage-handler-registry-state-snapshot)) (throw (ex-info "Damage handler registry is frozen" {}))))

(defn damage-handler-registry-snapshot [] (damage-handler-registry-state-snapshot))

(defn reset-damage-handler-registry-for-test!
  ([] (reset-damage-handler-registry-for-test! {}))
  ([{:keys [handlers frozen?] :or {handlers {} frozen? false}}]
   (reset! (damage-handler-registry-state-atom) {:handlers handlers :frozen? frozen?}) nil))

(defn freeze-damage-handler-registry! [] (update-damage-handler-registry-state! assoc :frozen? true) nil)

(defn- get-sorted-handlers []
  (->> (:handlers (damage-handler-registry-state-snapshot))
       (sort-by (fn [[_handler-id data]] (:priority data)))
       (map (fn [[handler-id data]] [handler-id (:fn data)]))))

(defn register-damage-handler! [handler-id handler-fn priority]
  (try
    (if-let [existing (get (:handlers (damage-handler-registry-state-snapshot)) handler-id)]
      (when-not (= (:priority existing) priority)
        (throw (ex-info "Conflicting damage handler id" {:id handler-id :existing-priority (:priority existing) :new-priority priority})))
      (do (assert-registry-open!)
          (update-damage-handler-registry-state! assoc-in [:handlers handler-id] {:fn handler-fn :priority priority})
          (log/debug "Registered damage handler:" handler-id "priority:" priority))) true
    (catch Exception e (log/warn "Failed to register damage handler:" (ex-message e)) false)))

(defn unregister-damage-handler! [handler-id]
  (try (assert-registry-open!)
       (update-damage-handler-registry-state! update :handlers dissoc handler-id)
       (log/debug "Unregistered damage handler:" handler-id) true
       (catch Exception e (log/warn "Failed to unregister damage handler:" (ex-message e)) false)))

(defn get-active-handlers [] (keys (:handlers (damage-handler-registry-state-snapshot))))

(defn process-damage! [player-id attacker-id original-damage damage-source]
  (try
    (loop [remaining-handlers (get-sorted-handlers) current-damage (double original-damage)]
      (if (empty? remaining-handlers) current-damage
        (let [[handler-id handler-fn] (first remaining-handlers)
              next-damage (try (let [result (handler-fn player-id attacker-id current-damage damage-source)]
                                (cond (vector? result) (double (first result))
                                      (number? result) (double result)
                                      :else current-damage))
                              (catch Exception e (log/warn "Damage handler" handler-id "threw:" (ex-message e)) current-damage))]
          (recur (rest remaining-handlers) (double next-damage)))))
    (catch Exception e (log/warn "process-damage! failed:" (ex-message e)) (double original-damage))))
