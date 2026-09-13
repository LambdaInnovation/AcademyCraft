(ns cn.li.mcbase.runtime.accessor-registry-core
  "Core accessor registry implementation with schema enforcement.
   
   Accessors are registered functions/protocols that provide platform-independent
   access to Minecraft runtime data and operations. This registry:
   - Enforces accessor signature schema
   - Prevents duplicate registrations
   - Provides clear documentation for each accessor
   - Organizes accessors by domain (world, entity, render, lifecycle)"
  (:require [clojure.string :as str]
            [cn.li.mcmod.util.log :as log]))

(def ^:private accessor-registry-lock
  (Object.))

(def ^:private registries*
  (atom {:world {}
         :entity {}
         :render {}
         :lifecycle {}}))

(defn- accessor-registries-snapshot
  []
  @registries*)

(defn- update-accessor-registry!
  [domain update-fn]
  (locking accessor-registry-lock
    (let [registries (accessor-registries-snapshot)
          current (or (get registries domain)
                      (throw (ex-info "Unknown accessor domain" {:domain domain})))
          updated (update-fn current)]
  (reset! registries* (assoc registries domain updated))
      updated)))

(defn- get-registry
  [domain]
  (or (get (accessor-registries-snapshot) domain)
      (throw (ex-info "Unknown accessor domain" {:domain domain}))))

(defn- validate-accessor-key
  [key]
  (when-not (keyword? key)
    (throw (ex-info "Accessor key must be a keyword" {:key key :type (type key)})))
  key)

(defn- validate-accessor-fn
  [accessor-fn]
  (when-not (or (fn? accessor-fn)
                (symbol? accessor-fn)
                (var? accessor-fn)
                (ifn? accessor-fn))
    (throw (ex-info "Accessor must be a function or callable"
                    {:value accessor-fn :type (type accessor-fn)})))
  accessor-fn)

(defn- validate-accessor-doc
  [doc]
  (when-not (and (string? doc) (not (str/blank? doc)))
    (throw (ex-info "Accessor must have non-empty string documentation" {:doc doc})))
  doc)

(defn register-accessor!
  [domain key accessor-fn doc]
  (validate-accessor-key key)
  (validate-accessor-fn accessor-fn)
  (validate-accessor-doc doc)
  (log/debug "Registering accessor"
            {:domain domain :key key :doc (subs doc 0 (min 60 (count doc)))})
  (update-accessor-registry!
    domain
    (fn [current]
      (when (contains? current key)
        (throw (ex-info "Accessor already registered in domain"
                        {:domain domain :key key})))
      (assoc current key {:fn accessor-fn :doc doc}))))

(defn- register-accessor-group!
  [domain accessors-list]
  (doseq [[key fn doc] accessors-list]
    (register-accessor! domain key fn doc)))

(defn- get-accessor
  [domain key]
  (when-let [registry (get (accessor-registries-snapshot) domain)]
    (when-let [entry (get registry key)]
      (:fn entry))))

(defn- get-accessor-meta
  [domain key]
  (when-let [registry (get (accessor-registries-snapshot) domain)]
    (get registry key)))

(defn- get-accessor-or-throw
  [domain key]
  (or (get-accessor domain key)
      (throw (ex-info "Accessor not found"
                      {:domain domain :key key}))))

(defn- list-accessors
  [domain]
  (keys (get-registry domain)))

(defn- list-accessors-detailed
  [domain]
  (get-registry domain))

(defn- accessor-exists?
  [domain key]
  (when-let [registry (get (accessor-registries-snapshot) domain)]
    (contains? registry key)))

(defn- get-all-accessors
  []
  (accessor-registries-snapshot))

(defn- validate-registry-integrity
  []
    (let [all-accessors (get-all-accessors)
          errors (transient [])]
      (doseq [[domain domain-accessors] all-accessors]
        (doseq [[key {:keys [fn]}] domain-accessors]
          (when-not (ifn? fn)
            (conj! errors {:domain domain
                           :key key
                           :error "Accessor is not callable"}))))
    (let [errors* (persistent! errors)]
      (if (empty? errors*)
        {:valid true}
        {:valid false :errors errors*}))))

(defn- registry-status
  []
  {:total-domains (count (accessor-registries-snapshot))
   :domains (into {}
                  (for [[domain registry-map] (accessor-registries-snapshot)]
                    [domain {:count (count registry-map)
                             :keys (keys registry-map)}]))
   :integrity (validate-registry-integrity)})

(defn- clear-domain!
  [domain]
  (update-accessor-registry! domain (constantly {})))

(defn- unregister-accessor!
  [domain key]
  (update-accessor-registry! domain #(dissoc % key)))

(defn- snapshot-registry
  []
  (into {}
        (for [[domain registry-map] (accessor-registries-snapshot)]
          [domain (into {} registry-map)])))
