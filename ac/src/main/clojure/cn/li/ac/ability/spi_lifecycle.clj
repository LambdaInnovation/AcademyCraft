(ns cn.li.ac.ability.spi-lifecycle
  "Ability lifecycle SPI.

  This namespace provides a lifecycle layer so ability definitions can declare
  activate/tick/deactivate hooks without coupling execution logic to the registry
  or player-state implementation.

  Lifecycle objects are plain function maps with keys:
    :on-activate   (fn [player context])
    :on-tick       (fn [player context])
    :on-deactivate (fn [player context])
    :can-execute?  (fn [player context])

  Registry stored in Framework [:service :ability-lifecycle]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Key set documentation
;; ============================================================================

(def ability-lifecycle-keys
  "Keys required by an ability lifecycle function map."
  [:on-activate :on-tick :on-deactivate :can-execute?])

;; ============================================================================
;; Wrapper functions — dispatch via map key lookup
;; ============================================================================

(defn on-activate
  "Call the on-activate hook."
  [lifecycle player context]
  ((:on-activate lifecycle) player context))

(defn on-tick
  "Call the on-tick hook."
  [lifecycle player context]
  ((:on-tick lifecycle) player context))

(defn on-deactivate
  "Call the on-deactivate hook."
  [lifecycle player context]
  ((:on-deactivate lifecycle) player context))

(defn can-execute?
  "Check if the ability can execute."
  [lifecycle player context]
  ((:can-execute? lifecycle) player context))

;; ============================================================================
;; Lifecycle Registry — Framework [:service :ability-lifecycle]
;; ============================================================================

(def ^:private lifecycle-path [:service :ability-lifecycle])

(defn- lifecycle-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom lifecycle-path {:lifecycles {} :frozen? false})
    {:lifecycles {} :frozen? false}))

(defn- update-lifecycle-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in lifecycle-path
           (fn [current] (apply f (or current {:lifecycles {} :frozen? false}) args))))
  nil)

(defn- assert-registry-open! []
  (when (:frozen? (lifecycle-registry-state-snapshot))
    (throw (ex-info "Ability lifecycle registry is frozen" {}))))

;; Runtime container API.
(defn create-lifecycle-registry-runtime
  ([]
   {::lifecycle-registry-runtime true
    :state* (atom {:lifecycles {} :frozen? false})})
  ([{:keys [state*] :or {state* (atom {:lifecycles {} :frozen? false})}}]
   {::lifecycle-registry-runtime true :state* state*}))

(defn install-lifecycle-registry-runtime! [runtime]
  (when-let [fw-atom (fw/fw-atom)]
    (when-let [state* (:state* runtime)]
      (swap! fw-atom assoc-in lifecycle-path @state*)))
  runtime)

(defn lifecycle-registry-snapshot []
  (lifecycle-registry-state-snapshot))

(defn reset-lifecycle-registry-for-test!
  ([]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in lifecycle-path {:lifecycles {} :frozen? false}))
   nil)
  ([{:keys [lifecycles frozen?] :or {lifecycles {} frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in lifecycle-path {:lifecycles lifecycles :frozen? frozen?}))
   nil))

(defn freeze-lifecycle-registry! []
  (update-lifecycle-registry-state! assoc :frozen? true)
  nil)

(defn register-lifecycle! [ability-id lifecycle]
  (assert-registry-open!)
  (if-let [existing (get (:lifecycles (lifecycle-registry-state-snapshot)) ability-id)]
    (throw (ex-info "Duplicate lifecycle registration" {:ability-id ability-id}))
    (update-lifecycle-registry-state! assoc-in [:lifecycles ability-id] lifecycle))
  nil)

(defn unregister-lifecycle! [ability-id]
  (update-lifecycle-registry-state! update :lifecycles dissoc ability-id)
  nil)

(defn get-lifecycle [ability-id]
  (get (:lifecycles (lifecycle-registry-state-snapshot)) ability-id))

(defn lifecycle-registered? [ability-id]
  (contains? (:lifecycles (lifecycle-registry-state-snapshot)) ability-id))

(defn call-with-lifecycle-registry-runtime
  "Temporarily replace the lifecycle registry state with the runtime's state
  for the duration of `f`."
  [runtime f]
  (let [fw-atom (fw/fw-atom)]
    (if fw-atom
      (let [prev (get-in @fw-atom lifecycle-path)]
        (try
          (swap! fw-atom assoc-in lifecycle-path @(:state* runtime))
          (f)
          (finally
            (swap! fw-atom assoc-in lifecycle-path prev))))
      (f))))
