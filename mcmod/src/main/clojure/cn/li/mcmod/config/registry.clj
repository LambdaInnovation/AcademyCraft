(ns cn.li.mcmod.config.registry
  "Platform-neutral config descriptor and runtime value registry.

  State stored in Framework [:registry :configs]."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]))

(def ^:private config-path [:registry :configs])

;; Generation counter bumped on every config mutation — callers that cache
;; derived values (e.g. ability skill specs after apply-skill-overrides) can
;; check this instead of recomputing from get-config-values on every read.
(def ^:private generation-path [:service :config-generation])

(defn config-generation []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom generation-path 0)
    0))

(defn- bump-generation! []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in generation-path (fnil inc 0)))
  nil)

(defn- config-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom config-path)
    {:descriptor-registry {} :value-registry {}}))

(defn- update-config! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in config-path
           (fn [current] (apply f (or current {:descriptor-registry {} :value-registry {}}) args))))
  (bump-generation!)
  nil)

(defn get-descriptor-registry []
  (:descriptor-registry (config-snapshot)))

(defn get-value-registry []
  (:value-registry (config-snapshot)))

(defn set-descriptor-registry! [registry]
  (update-config! assoc :descriptor-registry (or registry {})))

(defn set-value-registry! [registry]
  (update-config! assoc :value-registry (or registry {})))

(defn- normalize-descriptors [descriptors]
  (let [descriptors (vec descriptors)
        keys (mapv #(get % :key) descriptors)]
    (when-not (every? keyword? keys)
      (throw (ex-info "Config descriptor keys must be keywords" {:descriptor-keys keys})))
    (when-not (= (count keys) (count (distinct keys)))
      (throw (ex-info "Duplicate config descriptor keys" {:descriptor-keys keys})))
    descriptors))

(defn get-config-descriptors [domain]
  (get-in (config-snapshot) [:descriptor-registry domain] []))

(defn get-all-config-domains []
  (keys (:descriptor-registry (config-snapshot))))

(defn register-config-descriptors!
  "Register config descriptors for a domain, replacing any existing.
   Each descriptor must have :key, :default, and optionally :validate."
  [domain descriptors]
  (when-not (keyword? domain)
    (throw (ex-info "Config domain must be a keyword" {:domain domain})))
  (let [descriptors (normalize-descriptors descriptors)]
    (update-config! assoc-in [:descriptor-registry domain] descriptors)
    (log/debug "Registered config descriptors for domain" domain
              "count" (count descriptors))
    nil))

(defn descriptor-default-values [domain]
  (into {} (map #(vector (get % :key) (get % :default))
                (get-config-descriptors domain))))

(defn ensure-default-values! [domain defaults]
  (update-config! update-in [:value-registry domain]
                  (fn [current] (merge (or defaults (descriptor-default-values domain))
                                      (or current {}))))
  nil)

(defn get-config-values [domain]
  (merge (descriptor-default-values domain)
         (get-in (config-snapshot) [:value-registry domain] {})))

(defn get-config-value
  ([domain key]
   (get (get-config-values domain) key))
  ([domain key default]
   (get (get-config-values domain) key default)))

(defn set-config-value! [domain key value]
  (update-config! assoc-in [:value-registry domain key] value))

(defn set-config-values!
  "Replace domain's runtime values with value-map, filling keys value-map
   omits from descriptor defaults (not from whatever was previously set)."
  [domain value-map]
  (update-config! assoc-in [:value-registry domain]
                  (merge (descriptor-default-values domain) value-map))
  nil)

(defn reset-config-for-test! []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in config-path {:descriptor-registry {} :value-registry {}}))
  (bump-generation!)
  nil)
