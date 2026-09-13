(ns cn.li.ac.ability.discovery
	"Discovery/registration layer for AC ability content.

	This namespace now builds provider lists from a classpath scanner,
	while preserving the existing provider registry API surface."
	(:require [cn.li.ac.discovery.registry :as discovery-registry]
						[cn.li.ac.ability.discovery-descriptor :as discovery-descriptor]
						[cn.li.ac.discovery.scanner :as scanner]
						[cn.li.mcmod.util.log :as log]))

(declare register-provider! registered-providers merge-provider!)

(def ^:private attempts*
  "Retry-if-empty counter, not a strict exactly-once guard (bootstrap-default-
  providers! deliberately re-attempts when the registry looks empty even
  after the count threshold) — install/framework-once! doesn't model retry,
  so this stays a plain top-level atom rather than migrating to it."
  (atom 0))

(defn bootstrap-attempts-snapshot
  "Return current bootstrap attempt count. For test diagnostics."
  []
  @attempts*)

(defn reset-bootstrap-attempts-for-test!
  "Reset bootstrap attempt counter. For tests only."
  ([]
   (reset-bootstrap-attempts-for-test! 0))
  ([attempts]
   (reset! attempts* (long (or attempts 0)))
   nil))

(defn bootstrap-default-providers!
  "Discover and register built-in ability providers from classpath.

  Safe to call repeatedly. Tracks attempts internally — runs discovery
  at most once per JVM session (or until reset for tests)."
  []
  (when (or (zero? (count (registered-providers)))
            (< @attempts* 1))
    (swap! attempts* inc)
    (let [scanned-providers (scanner/discover-ability-providers)
          descriptor-providers (discovery-descriptor/bundled-provider-descriptors)]
      (when (and (empty? scanned-providers) (empty? descriptor-providers))
        (log/warn "Ability discovery returned no providers; check classpath/resource layout"))
      (doseq [provider scanned-providers]
        (register-provider! provider))
      (doseq [provider descriptor-providers]
        (merge-provider! provider))))
  (registered-providers))

(defn- provider-by-id
	[provider-id]
	(some #(when (= (:id %) provider-id) %) (registered-providers)))

(defn- merge-provider!
	[provider]
	(let [existing (provider-by-id (:id provider))
				merged (if existing
						 (-> existing
								 (assoc :priority (min (long (:priority existing)) (long (:priority provider))))
								 (update :skill-namespaces #(vec (distinct (concat % (:skill-namespaces provider)))))
								 (update :fx-namespaces #(vec (distinct (concat % (:fx-namespaces provider))))))
						 provider)]
		(register-provider! merged)))

(defn register-provider!
	"Register or replace an ability content provider."
	[provider]
	(discovery-registry/register-provider! provider))

(defn unregister-provider!
	[provider-id]
	(discovery-registry/unregister-provider! provider-id))

(defn registered-providers
	[]
	(discovery-registry/registered-providers))

(defn freeze-provider-discovery!
	[]
	(discovery-registry/freeze-provider-registry!))

(defn discovered-skill-namespaces
	"Return server-side skill namespaces in deterministic load order."
	[]
	(bootstrap-default-providers!)
	(->> (registered-providers)
			 (mapcat #(get % :skill-namespaces))
			 distinct
			 vec))

(defn discovered-fx-namespaces
	"Return client-side FX namespaces in deterministic load order."
	[]
	(bootstrap-default-providers!)
	(->> (registered-providers)
			 (mapcat #(get % :fx-namespaces))
			 distinct
			 vec))
