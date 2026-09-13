(ns cn.li.ac.discovery.registry
  "Generic provider registry used by discovery-based bootstrap flows.

  Registry stored in Framework [:registry :providers :discovery]."
  (:require [cn.li.ac.discovery.core :as core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def ^:private prov-path [:registry :providers :discovery])

(defn- provider-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom prov-path {:providers {} :frozen? false})
    {:providers {} :frozen? false}))

(defn- update-provider-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in prov-path
           (fn [current] (apply f (or current {:providers {} :frozen? false}) args))))
  nil)

(defn- assert-registry-open! []
  (when (:frozen? (provider-registry-state-snapshot))
    (throw (ex-info "Discovery provider registry is frozen" {}))))

(defn provider-registry-snapshot []
  (provider-registry-state-snapshot))

(defn reset-provider-registry-for-test!
  ([] (reset-provider-registry-for-test! {}))
  ([{:keys [providers frozen?] :or {providers {} frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in prov-path {:providers providers :frozen? frozen?}))
   nil))

(defn freeze-provider-registry! []
  (update-provider-registry-state! assoc :frozen? true) nil)

(defn register-provider!
  [provider]
  (let [provider* (core/normalize-provider provider)]
    (assert-registry-open!)
    (update-provider-registry-state! assoc-in [:providers (:id provider*)] provider*)
    (log/debug "Registered discovery provider" (:id provider*))
    provider*))

(defn unregister-provider!
  [provider-id]
  (assert-registry-open!)
  (update-provider-registry-state! update :providers dissoc provider-id)
  nil)

(defn clear-providers! []
  (reset-provider-registry-for-test!))

(defn registered-providers []
  (->> (:providers (provider-registry-state-snapshot))
       vals
       (sort-by core/provider-sort-key)
       vec))
