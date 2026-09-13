(ns cn.li.ac.energy.energy-type-interface
  "Energy type abstraction and registry.

  This introduces a small, platform-neutral registry so different item energy
  semantics can coexist without hard-coding all callers to one implementation.

  Energy types are plain function maps with keys:
    :energy-type-id        — stable keyword identifier
    :energy-type-name      — display name
    :supports-item?        — (fn [item-stack])  -> boolean
    :get-energy*           — (fn [item-stack])  -> double
    :get-capacity*         — (fn [item-stack])  -> double
    :get-bandwidth*        — (fn [item-stack])  -> double
    :set-energy*!          — (fn [item-stack amount])  -> nil
    :charge-item*!         — (fn [item-stack amount ignore-bandwidth]) -> leftover
    :discharge-item*!      — (fn [item-stack amount ignore-bandwidth]) -> extracted

  Registry stored in Framework [:registry :energy]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Energy type key set documentation
;; ============================================================================

(def energy-type-keys
  "Keys required by an energy type function map."
  [:energy-type-id :energy-type-name :supports-item?
   :get-energy* :get-capacity* :get-bandwidth*
   :set-energy*! :charge-item*! :discharge-item*!])

;; ============================================================================
;; Wrapper functions — keep calling convention for existing callers
;; ============================================================================

(defn energy-type-id
  "Get the stable keyword identifier from an energy type."
  [etype]
  (:energy-type-id etype))

(defn energy-type-name
  "Get the display name from an energy type."
  [etype]
  (:energy-type-name etype))

(defn supports-item?
  "Return true when this type can operate on the item."
  [etype item-stack]
  ((:supports-item? etype) item-stack))

(defn get-energy*
  "Read current energy from item-stack via energy type.
  Returns double."
  [etype item-stack]
  ((:get-energy* etype) item-stack))

(defn get-capacity*
  "Read capacity from item-stack via energy type.
  Returns double."
  [etype item-stack]
  ((:get-capacity* etype) item-stack))

(defn get-bandwidth*
  "Read transfer rate from item-stack via energy type.
  Returns double."
  [etype item-stack]
  ((:get-bandwidth* etype) item-stack))

(defn set-energy*!
  "Set exact energy on item-stack via energy type."
  [etype item-stack amount]
  ((:set-energy*! etype) item-stack amount))

(defn charge-item*!
  "Charge item and return leftover energy.
  ignore-bandwidth: if true, bypass bandwidth limit."
  [etype item-stack amount ignore-bandwidth]
  ((:charge-item*! etype) item-stack amount ignore-bandwidth))

(defn discharge-item*!
  "Discharge item and return extracted energy.
  ignore-bandwidth: if true, bypass bandwidth limit."
  [etype item-stack amount ignore-bandwidth]
  ((:discharge-item*! etype) item-stack amount ignore-bandwidth))

;; ============================================================================
;; Registry — Framework [:registry :energy]
;; ============================================================================

(def ^:private et-path [:registry :energy])

(defn- energy-type-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom et-path {:types {} :frozen? false})
    {:types {} :frozen? false}))

(defn- update-energy-type-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in et-path
           (fn [current] (apply f (or current {:types {} :frozen? false}) args))))
  nil)

(defn- assert-not-frozen! []
  (when (:frozen? (energy-type-state-snapshot))
    (throw (ex-info "Energy type registry is frozen" {}))))

(defn register-energy-type! [energy-type]
  (assert-not-frozen!)
  (let [type-id (:energy-type-id energy-type)]
    (when-not (keyword? type-id)
      (throw (ex-info "Energy type id must be a keyword" {:type-id type-id})))
    (update-energy-type-state! update :types
      (fn [registry]
        (if-let [existing (get registry type-id)]
          (if (= existing energy-type) registry
            (throw (ex-info "Conflicting energy type id" {:type-id type-id})))
          (assoc registry type-id energy-type))))
    (log/debug "Registered energy type" type-id) energy-type))

(defn unregister-energy-type! [type-id]
  (assert-not-frozen!)
  (update-energy-type-state! update :types dissoc type-id) nil)

(defn freeze-energy-types! []
  (update-energy-type-state! assoc :frozen? true) nil)

(defn energy-types-snapshot [] (energy-type-state-snapshot))

(defn reset-energy-types-for-test! []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in et-path {:types {} :frozen? false}))
  nil)

(defn get-energy-type [type-id]
  (get (:types (energy-type-state-snapshot)) type-id))

(defn list-energy-types []
  (->> (:types (energy-type-state-snapshot)) vals (sort-by #(get % :energy-type-id)) vec))

(defn resolve-energy-type
  "The energy type owning `type-or-item` — a type id, or an item stack matched
  against every registered type's :supports-item?. Returns the type itself, not
  the predicate's result: callers feed it straight to get-energy* and friends."
  [type-or-item]
  (cond
    (keyword? type-or-item) (get-energy-type type-or-item)
    :else (some (fn [energy-type]
                  (when ((:supports-item? energy-type) type-or-item)
                    energy-type))
                (vals (:types (energy-type-state-snapshot))))))

;; ============================================================================
;; Runtime helpers for test isolation
;; ============================================================================

(defn create-energy-type-runtime
  "Create an isolated energy type runtime for testing."
  []
  (atom {:types {} :frozen? false}))

(defn call-with-energy-type-runtime
  "Temporarily replace the energy type state for the duration of `f`."
  [runtime f]
  (let [fw-atom (fw/fw-atom)]
    (if fw-atom
      (let [prev (get-in @fw-atom et-path)]
        (try
          (swap! fw-atom assoc-in et-path @runtime)
          (f)
          (finally
            (swap! fw-atom assoc-in et-path prev))))
      (f))))
