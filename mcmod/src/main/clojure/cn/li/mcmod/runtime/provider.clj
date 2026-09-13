(ns cn.li.mcmod.runtime.provider
  "Runtime loader for platform-neutral provider factories.

   A descriptor is data supplied by target metadata. Its factory is resolved
   only during bootstrap and must return a map of concrete IFn values. This
   namespace intentionally has no dependency on platform namespaces: a
   provider loaded through it must be safe to compile from source in a
   remapped production jar."
  (:require [clojure.set :as set]
            [cn.li.mcmod.framework :as fw]))

(def ^:private provider-path [:service :runtime-providers])

(defn- keywordize [value label]
  (cond
    (keyword? value) value
    (string? value) (keyword value)
    :else (throw (ex-info "Provider descriptor value must be a keyword or string"
                          {:field label :value value}))))

(defn- symbol-value [value label]
  (cond
    (symbol? value) value
    (string? value) (symbol value)
    :else (throw (ex-info "Provider descriptor value must be a symbol or string"
                          {:field label :value value}))))

(defn- normalize-operation-set [operations label]
  (let [result (into #{} (map #(keywordize % label)) (or operations []))]
    (when (empty? result)
      (throw (ex-info "Provider descriptor must declare operations"
                      {:field label :value operations})))
    result))

(defn- neutral-target-value? [value]
  (or (string? value) (keyword? value) (number? value)))

(defn- provider-target! [target]
  (when-not (map? target)
    (throw (ex-info "Provider target must be a neutral data map" {:target target})))
  (let [result (select-keys target [:id :loader :minecraft-version])]
    (when (or (not= 3 (count result))
              (some (complement neutral-target-value?) (vals result)))
      (throw (ex-info "Provider target may contain only id/loader/minecraft-version neutral scalars"
                      {:target target})))
    result))

(defn- resolve-factory! [{:keys [namespace function] :as descriptor}]
  (let [namespace-symbol (symbol-value namespace :namespace)
        function-symbol (symbol-value function :function)
        factory-symbol (symbol (str namespace-symbol) (str function-symbol))]
    ;; `requiring-resolve` is deliberately limited to generated provider
    ;; descriptors. verifyNeutralProviderClosure proves this source tree has
    ;; no Minecraft/loader/platform dependency before packaging.
    ;;
    ;; This helper is intentionally private: no caller receives an arbitrary
    ;; symbol-to-Var resolver as an SPI capability.
    (or (requiring-resolve factory-symbol)
        (throw (ex-info "Neutral provider factory is missing"
                        {:provider (:id descriptor)
                         :namespace namespace
                         :function function})))))

(defn- validate-provider-map! [provider-id expected-operations operations]
  (when-not (map? operations)
    (throw (ex-info "Neutral provider factory must return an operation map"
                    {:provider provider-id :value operations})))
  (let [actual-operations (set (keys operations))
        missing (sort (set/difference expected-operations actual-operations))
        unexpected (sort (set/difference actual-operations expected-operations))
        non-functions (->> operations
                           (remove (fn [[_ operation]] (ifn? operation)))
                           (map first)
                           sort)]
    (when (or (seq missing) (seq unexpected) (seq non-functions))
      (throw (ex-info "Neutral provider operation contract mismatch"
                      {:provider provider-id
                       :missing missing
                       :unexpected unexpected
                       :non-functions non-functions}))))
  operations)

(defn- host-op [host-ports [domain operation]]
  (get-in host-ports [(keywordize domain :required-host-ports)
                      (keywordize operation :required-host-ports)]))

(defn- validate-host-ports! [provider-id host-ports requirements]
  (doseq [requirement (or requirements [])]
    (when-not (and (vector? requirement) (= 2 (count requirement)))
      (throw (ex-info "Provider host-port requirement must be [domain operation]"
                      {:provider provider-id :requirement requirement})))
    (when-not (ifn? (host-op host-ports requirement))
      (throw (ex-info "Required platform host-port operation is missing"
                      {:provider provider-id
                       :domain (first requirement)
                       :operation (second requirement)}))))
  nil)

(defn install-provider!
  "Install one immutable neutral provider map in the current Framework.

   Duplicate installation is a lifecycle error: silently replacing a cached
   callback map would leave already-registered listeners pointing at stale
   functions."
  [fw-atom provider-id operations]
  (when-not (instance? clojure.lang.IAtom fw-atom)
    (throw (ex-info "Provider installation requires a Framework atom"
                    {:provider provider-id :framework fw-atom})))
  (let [provider-id (keywordize provider-id :id)]
    (swap! fw-atom
           (fn [state]
             (when (get-in state (conj provider-path provider-id))
               (throw (ex-info "Neutral provider is already installed"
                               {:provider provider-id})))
             (assoc-in state (conj provider-path provider-id) operations))))
  nil)

(defn provider-op!
  "Return a concrete provider IFn. Call this during listener construction and
   retain the returned value; it is not a hot-path dispatch API."
  [fw-atom provider-id operation]
  (let [provider-id (keywordize provider-id :id)
        operation (keywordize operation :operation)
        operations (get-in @fw-atom (conj provider-path provider-id))
        implementation (get operations operation)]
    (when-not (ifn? implementation)
      (throw (ex-info "Neutral provider operation is not installed"
                      {:provider provider-id
                       :operation operation
                       :installed (sort (keys operations))})))
    implementation))

(defn load-provider!
  "Resolve, validate and install one neutral provider factory.

   `context` is passed unchanged to the factory except that :framework and
   :host-ports are injected from the live Framework. The descriptor is data,
   never a platform Var or Class."
  [fw-atom {:keys [id side provides required-host-ports] :as descriptor} context]
  (let [provider-id (keywordize id :id)
        side (keywordize side :side)
        expected-operations (normalize-operation-set provides :provides)
        target (provider-target! (:target context))
        host-ports (:platform @fw-atom)
        _ (when-not (contains? #{:common :server :client :datagen} side)
            (throw (ex-info "Unsupported provider side" {:provider provider-id :side side})))
        _ (validate-host-ports! provider-id host-ports required-host-ports)
        factory (resolve-factory! descriptor)
        operations (factory {:framework fw-atom
                             :target target
                             :host-ports host-ports
                             :side side})]
    (validate-provider-map! provider-id expected-operations operations)
    (install-provider! fw-atom provider-id operations)
    nil))

(defn load-current-provider!
  "Load a provider after Framework injection without requiring target or loader
   code from this neutral namespace."
  [descriptor context]
  (let [fw-atom (or (fw/fw-atom)
                    (throw (ex-info "Provider loaded before Framework injection"
                                    {:provider (:id descriptor)})))]
    (load-provider! fw-atom descriptor context)))
