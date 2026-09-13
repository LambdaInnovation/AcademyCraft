(ns cn.li.mcmod.content.registry
  "Neutral descriptor registries used by content modules and host layers.

  This namespace stores only host-readable descriptor envelopes. Content-owned
  values remain opaque payload/metadata/handler data behind neutral field names."
  (:require [clojure.set :as set]
            [cn.li.mcmod.schema.core :as schema]
            [cn.li.mcmod.framework :as fw]))

(def ^:private allowed-categories
  #{:content-action
    :request
    :sync
    :player-persistence
    :client-input
    :screen
    :item-decorator
    :worldgen-feature
    :integration
    :smoke-manifest})

(def ^:private allowed-field-keys
  #{:id
    :content-id
    :message-id
    :message-key
    :nbt-key
    :format
    :payload
    :payload-key
    :operations
    :handler
    :capability-key
    :metadata
    :priority
    :tags
    :checks
    :fixtures
    :description
    :context-selector
    :result-handler
    :codec
    :factory
    :key
    :platform-key
    :host-key
    :category
    :kind
    :target
    :source
    :options
    :enabled?
    :clone?
    :order})

(def ^:private category-input-schema
  [:or keyword? string?])

(def ^:private category-schema
  (into [:enum] (sort allowed-categories)))

(defn- ^:private non-empty-descriptor-id? [s]
  (seq s))

(def ^:private descriptor-id-schema
  [:or keyword? [:and string? [:fn non-empty-descriptor-id?]]])

(def ^:private descriptor-schema
  map?)

(def ^:private field-key-schema
  (into [:enum] (sort allowed-field-keys)))

(def ^:private descriptor-field-keyword-set-schema
  [:set keyword?])

(def ^:private descriptor-field-set-schema
  [:set field-key-schema])

;; Lazy validator compilation via schema/lazy-validator — avoids executing
;; Malli's schema/validator during AOT classloading/bootstrap.  Each validator
;; is compiled once on first call and cached thereafter.
;; NOTE: `delay` is deliberately NOT used here — see `schema/lazy-validator`.
(def ^:private category-input-validator (schema/lazy-validator category-input-schema))
(defn- valid-category-input? [x]
  (schema/valid? (category-input-validator) x))
(def ^:private category-validator (schema/lazy-validator category-schema))
(defn- valid-category? [x]
  (schema/valid? (category-validator) x))
(def ^:private descriptor-id-validator (schema/lazy-validator descriptor-id-schema))
(defn- valid-descriptor-id? [x]
  (schema/valid? (descriptor-id-validator) x))
(def ^:private descriptor-validator (schema/lazy-validator descriptor-schema))
(defn- valid-descriptor? [x]
  (schema/valid? (descriptor-validator) x))
(def ^:private descriptor-field-keyword-set-validator (schema/lazy-validator descriptor-field-keyword-set-schema))
(defn- valid-descriptor-field-keyword-set? [x]
  (schema/valid? (descriptor-field-keyword-set-validator) x))
(def ^:private descriptor-field-set-validator (schema/lazy-validator descriptor-field-set-schema))
(defn- valid-descriptor-field-set? [x]
  (schema/valid? (descriptor-field-set-validator) x))

;; ============================================================================
;; Runtime Container
;; ============================================================================

;; Content registry — stored in Framework [:registry :content]

(def ^:private content-path [:registry :content])

(defn- content-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom content-path {})
    {}))

(defn- update-content-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in content-path
           (fn [current] (apply f (or current {}) args))))
  nil)

(defn- normalize-category
  [category]
  (when-not (valid-category-input? category)
    (throw (ex-info "Descriptor category must be a keyword or string"
                    {:category category})))
  (let [category-key (if (keyword? category)
                       category
                       (keyword category))]
    (when-not (valid-category? category-key)
      (throw (ex-info "Descriptor category is not registered as a neutral host category"
                      {:category category-key
                       :allowed-categories allowed-categories})))
    category-key))

(defn- validate-descriptor-id!
  [descriptor-id]
  (when-not (valid-descriptor-id? descriptor-id)
    (throw (ex-info "Descriptor id must be a keyword or non-empty string"
                    {:descriptor-id descriptor-id}))))

(defn- validate-field-keys!
  [descriptor]
  (let [field-keys (set (keys descriptor))]
    (when-not (valid-descriptor-field-keyword-set? field-keys)
      (let [non-keyword-fields (->> field-keys (remove keyword?) vec)]
        (throw (ex-info "Descriptor field names must be keywords"
                        {:fields non-keyword-fields}))))
    (when-not (valid-descriptor-field-set? field-keys)
      (let [unknown-fields (vec (set/difference field-keys allowed-field-keys))]
        (throw (ex-info "Descriptor field names must use the neutral host envelope"
                        {:fields unknown-fields
                         :allowed-fields allowed-field-keys}))))))

(defn- normalize-descriptor
  [descriptor-id descriptor]
  (when-not (valid-descriptor? descriptor)
    (throw (ex-info "Descriptor must be a map"
                    {:descriptor descriptor})))
  (validate-descriptor-id! descriptor-id)
  (validate-field-keys! descriptor)
  (let [descriptor (assoc descriptor :id descriptor-id)]
    (validate-field-keys! descriptor)
    descriptor))

(defn register-descriptor!
  "Register a descriptor under a neutral category.

  Re-registering the same descriptor is idempotent. Reusing an id for a
  different descriptor is rejected to keep content bootstrap repeatability safe."
  ([category descriptor]
   (register-descriptor! category (:id descriptor) descriptor))
  ([category descriptor-id descriptor]
   (let [category-key (normalize-category category)
         normalized (normalize-descriptor descriptor-id descriptor)]
     (update-content-registry-state!
            (fn [state]
              (let [existing (get-in state [category-key descriptor-id])]
                (cond
                  (nil? existing)
                  (assoc-in state [category-key descriptor-id] normalized)

                  (= existing normalized)
                  state

                  :else
                  (throw (ex-info "Descriptor id is already registered with different data"
                                  {:category category-key
                                   :descriptor-id descriptor-id}))))))
     nil)))

(defn register-descriptors!
  [category descriptors]
  (doseq [descriptor descriptors]
    (register-descriptor! category descriptor))
  nil)

(defn get-descriptor
  [category descriptor-id]
  (get-in (content-registry-state-snapshot) [(normalize-category category) descriptor-id]))

(defn require-descriptor
  [category descriptor-id]
  (or (get-descriptor category descriptor-id)
      (throw (ex-info "Descriptor is not registered"
                      {:category (normalize-category category)
                       :descriptor-id descriptor-id}))))

(defn list-descriptors
  [category]
  (->> (vals (get (content-registry-state-snapshot) (normalize-category category) {}))
       (sort-by #(str (get % :id)))
       vec))

(defn registry-snapshot
  []
  (content-registry-state-snapshot))

(defn clear-category!
  [category]
  (update-content-registry-state! dissoc (normalize-category category))
  nil)

(defn clear-registry!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in content-path {}))
  nil)

(defn- invoke-descriptor-handler!
  [category descriptor-id args]
  (let [{:keys [handler]} (require-descriptor category descriptor-id)]
    (when-not (fn? handler)
      (throw (ex-info "Descriptor handler is not installed"
                      {:category (normalize-category category)
                       :descriptor-id descriptor-id})))
    (apply handler args)))

(defn register-action!
  [descriptor]
  (register-descriptor! :content-action descriptor))

(defn dispatch-action!
  [action-id context payload]
  (invoke-descriptor-handler! :content-action action-id [context payload]))

(defn register-sync-descriptor!
  [descriptor]
  (register-descriptor! :sync descriptor))

(defn list-sync-descriptors
  []
  (list-descriptors :sync))

(defn apply-sync!
  [sync-id context payload]
  (invoke-descriptor-handler! :sync sync-id [context payload]))

(defn register-player-persistence-descriptor!
  [descriptor]
  (register-descriptor! :player-persistence descriptor))

(defn list-player-persistence-descriptors
  []
  (list-descriptors :player-persistence))

;; Client input is now dispatched solely through mcmod.protocol.keyboard-input.
;; Former System B (client-input descriptors) was dead code — emit-client-input! was
;; never called from any platform event loop. Removed to consolidate into one system.

(defn register-smoke-manifest!
  [descriptor]
  (register-descriptor! :smoke-manifest descriptor))

(defn list-smoke-manifests
  []
  (list-descriptors :smoke-manifest))
