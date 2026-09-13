(ns cn.li.mcmod.capability.registry
  "Capability declaration and lookup registry.

  Content code calls declare-capability! to register a capability type and its
  handler factory. Handler factories are resolved at bundle compile time via
  get-handler-factory."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Registry
;; ============================================================================

; Map of keyword → {:java-type Class :handler-factory-fn (fn [be side] handler)}
; Populated by declare-capability! calls from content namespaces at load time.

(defn capability-type-registry-snapshot
  []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom [:registry :capability ::capability-types])
    {}))

(defn update-capability-type-registry!
  [f & args]
  (registry/register! (fw/fw-atom) :capability ::capability-types
    (apply f (capability-type-registry-snapshot) args))
  nil)

(defn reset-capability-type-registry!
  "Test-only reset of the static capability type registry."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in [:registry :capability] dissoc ::capability-types))
  nil)

(defn get-capability-entry
  "Look up a capability entry by key. Returns nil if not registered."
  [key]
  (get (capability-type-registry-snapshot) key))

(defn get-handler-factory
  "Return the handler-factory-fn for key, or nil."
  [key]
  (:handler-factory-fn (get-capability-entry key)))

;; ============================================================================
;; Public API
;; ============================================================================

(defn declare-capability!
  "Register a capability type with this mod."
  [key java-type handler-factory-fn]
  (when-not (keyword? key)
    (throw (ex-info "declare-capability!: key must be keyword" {:key key})))
  (let [entry {:java-type java-type :handler-factory-fn handler-factory-fn}]
    (when-let [prev (get-capability-entry key)]
      (when (not= prev entry)
        (throw (ex-info "Duplicate capability registration with different entry"
                        {:key key :previous prev :incoming entry}))))
    (update-capability-type-registry! assoc key entry))
  (log/debug "Declared capability" key "->" (.getName ^Class java-type))
  nil)

;; ============================================================================
;; Tile Fluid Spec Registry
;; Maps tile-id (string) → {:mod-id string :path string} for fluid capability.
;; Content registers which fluid a tile holds; platform reads at capability setup.
;; ============================================================================

(defn register-tile-fluid-spec!
  "Register the fluid a tile holds for IFluidHandler capability binding.
  tile-id is the DSL tile id string; mod-id and fluid-path form the ResourceLocation."
  [tile-id fluid-mod-id fluid-path]
  (registry/register! (fw/fw-atom) :capability [::tile-fluid tile-id]
    {:mod-id fluid-mod-id :path fluid-path})
  nil)

(defn get-tile-fluid-spec
  "Return {:mod-id ... :path ...} for tile-id, or nil if not registered."
  [tile-id]
  (when-let [fw-atom (fw/fw-atom)]
    (registry/get-spec fw-atom :capability [::tile-fluid tile-id])))

(defn reset-tile-fluid-specs-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in [:registry :capability]
      (fn [m] (into {} (remove (fn [[k _]] (and (vector? k) (= ::tile-fluid (first k))))) m))))
  nil)

;; ============================================================================
;; Capability Access — Map-based API
;; ============================================================================

;; Expected map keys for ICapabilityProvider:
;; - :get-capability (fn [cap side] -> capability or nil)

;; Expected map keys for ILazyOptional:
;; - :is-present? (fn [] -> boolean)
;; - :or-else (fn [default] -> value)

;; Wrapper functions for capability providers

(defn get-capability
  "Get a capability from a provider. Returns LazyOptional or equivalent."
  [provider cap side]
  ((:get-capability provider) cap side))

;; Wrapper functions for lazy optionals

(defn is-present?
  "Check if the capability is present. Returns boolean."
  [lazy-opt]
  ((:is-present? lazy-opt)))

(defn or-else
  "Return contained value or `default` if not present."
  [lazy-opt default]
  ((:or-else lazy-opt) default))
