(ns cn.li.ac.registry.discovery
  "Discovery layer for AC content providers.

  This is a lightweight dynamic discovery mechanism built on top of the
  existing phase-plugin registration flow. Providers register phase specs,
  and consumers materialize a merged load plan from those providers."
  (:require [cn.li.ac.registry.core :as core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Registry — Framework [:registry :providers :content]

(def ^:private disc-path [:registry :providers :content])

(defn- content-provider-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom disc-path {:providers {} :frozen? false})
    {:providers {} :frozen? false}))

(defn- update-content-provider-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in disc-path
           (fn [current] (apply f (or current {:providers {} :frozen? false}) args))))
  nil)

(defn- assert-registry-open!
  []
  (when (:frozen? (content-provider-registry-state-snapshot))
    (throw (ex-info "Content discovery provider registry is frozen" {}))))

(defn provider-registry-snapshot
  []
  (content-provider-registry-state-snapshot))

(defn reset-provider-registry-for-test!
  ([] (reset-provider-registry-for-test! {}))
  ([{:keys [providers frozen?]
     :or {providers {} frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in disc-path {:providers providers :frozen? frozen?}))
   nil))

(defn freeze-provider-registry!
  []
  (update-content-provider-registry-state! assoc :frozen? true)
  nil)

(defn register-provider!
  "Register or replace a content provider by id."
  [provider]
  (let [provider* (core/normalize-provider provider)
  id (core/provider-id* provider*)]
    (assert-registry-open!)
    (update-content-provider-registry-state! assoc-in [:providers id] provider*)
    (log/debug "Registered content provider" id)
    provider*))

(defn unregister-provider!
  [provider-id]
  (assert-registry-open!)
  (update-content-provider-registry-state! update :providers dissoc provider-id)
  nil)

(defn registered-providers
  []
  (vals (:providers (content-provider-registry-state-snapshot))))

(defn discover-providers
  "Return providers in load order."
  []
  (->> (registered-providers)
  (sort-by (juxt core/provider-priority* core/provider-id*))
       vec))

(defn discovered-content-phases
  "Materialize all discovered provider phases into a single ordered load plan."
  []
  (->> (discover-providers)
  (mapcat core/provider-phases*)
       vec))

(defn register-phase-provider!
  "Compatibility helper for code that only knows about a single phase spec."
  [phase-spec]
  (register-provider!
    {:id (:phase phase-spec)
     :priority (or (:priority phase-spec) 100)
     :phases [phase-spec]}))

(defn bootstrap-default-providers!
  "Ensure the given default phases are registered exactly once.

  Safe to call repeatedly."
  [phase-specs]
  (doseq [phase-spec phase-specs]
    (register-phase-provider! phase-spec))
  (discover-providers))
