(ns cn.li.ac.energy.service.provider-registry
  "Provider inference and registry helpers for the energy API implementation."
  (:require [cn.li.ac.energy.domain.container :as container]
            [cn.li.ac.energy.service.item-manager :as item-manager]
            [cn.li.ac.energy.service.node-manager :as node-manager]))

(defn- supported?
  [pred value]
  (try
    (boolean (pred value))
    (catch Exception _
      false)))

(defn infer-provider
  "Infer a provider descriptor from a raw value."
  [value]
  (cond
    (nil? value) nil
    (and (map? value) (:kind value)) value
    (container/energy-container? value) {:kind :container :value value}
    (supported? item-manager/is-energy-item-supported? value) {:kind :item :value value}
    (supported? node-manager/is-node-supported? value) {:kind :node :value value}
    :else nil))

(defn resolve-provider
  "Resolve a provider by registered id or infer one directly from a raw value."
  [providers id-or-value]
  (or (get @providers id-or-value)
      (when (keyword? id-or-value) (get @providers (name id-or-value)))
      (infer-provider id-or-value)))

(defn provider-energy
  "Read current energy from a provider descriptor."
  [{:keys [kind value ref]}]
  (case kind
    :item (item-manager/get-item-energy value)
    :node (or (node-manager/get-node-energy value) 0.0)
    :container (container/get-current (if ref @ref value))
    nil))

(defn provider-capacity
  "Read max capacity from a provider descriptor."
  [{:keys [kind value ref]}]
  (case kind
    :item (item-manager/get-item-capacity value)
    :node (or (node-manager/get-node-capacity value) 0.0)
    :container (container/get-capacity (if ref @ref value))
    nil))

(defn update-container-ref!
  "Apply a pure container update to an atom-backed provider.

  Returns `[old new]` when the provider is mutable, otherwise nil."
  [{:keys [ref value]} f & args]
  (let [target (or ref value)]
    (when (instance? clojure.lang.IAtom target)
      (let [old @target
            new-value (apply f old args)]
        (reset! target new-value)
        [old new-value]))))

(defn provider-from-registration-value
  "Build a provider descriptor for a value passed to register-provider!."
  [value]
  (if (instance? clojure.lang.IAtom value)
    (if-let [nested (infer-provider @value)]
      (assoc nested :ref value)
      {:kind :container :ref value :value @value})
    (infer-provider value)))

(defn register-provider!
  "Register a provider descriptor under a stable id in `providers`."
  [providers id value]
  (when-let [provider (provider-from-registration-value value)]
    (swap! providers assoc id provider)
    id))

(defn unregister-provider!
  "Unregister a provider id from `providers`."
  [providers id]
  (swap! providers dissoc id)
  nil)

(defn provider-ids
  "Return registered provider ids."
  [providers]
  (vec (keys @providers)))

(defn dump-state
  "Return an admin-friendly snapshot of provider energy/capacity."
  [providers]
  (into {}
        (map (fn [[id provider]]
               [id {:kind (:kind provider)
                    :energy (provider-energy provider)
                    :capacity (provider-capacity provider)}]))
        @providers))

(defn clear!
  "Clear all providers."
  [providers]
  (reset! providers {})
  nil)
