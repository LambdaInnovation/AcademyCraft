(ns cn.li.platform.registry.metadata
  "Platform-side slots for neutral registry metadata callbacks.

   `install!` is called exactly once during common bootstrap. It replaces each
   placeholder Var root with the provider's concrete IFn, so subsequent calls
   do not resolve a namespace, dereference Framework state, or dispatch
   through a provider map."
  )

(defn- unavailable [operation]
  (throw (IllegalStateException.
          (str "Registry metadata provider is unavailable: " operation))))

(defn get-all-block-ids [& _] (unavailable :get-all-block-ids))
(defn get-all-creative-tab-entries [& _] (unavailable :get-all-creative-tab-entries))
(defn get-all-effect-ids [& _] (unavailable :get-all-effect-ids))
(defn get-all-fluid-ids [& _] (unavailable :get-all-fluid-ids))
(defn get-all-item-ids [& _] (unavailable :get-all-item-ids))
(defn get-all-particle-ids [& _] (unavailable :get-all-particle-ids))
(defn get-all-sound-ids [& _] (unavailable :get-all-sound-ids))
(defn get-all-tile-ids [& _] (unavailable :get-all-tile-ids))
(defn get-block-registry-name [& _] (unavailable :get-block-registry-name))
(defn get-block-spec [& _] (unavailable :get-block-spec))
(defn get-block-tile-id [& _] (unavailable :get-block-tile-id))
(defn get-effect-registry-name [& _] (unavailable :get-effect-registry-name))
(defn get-effect-spec [& _] (unavailable :get-effect-spec))
(defn get-fluid-id-for-block [& _] (unavailable :get-fluid-id-for-block))
(defn get-fluid-registry-name [& _] (unavailable :get-fluid-registry-name))
(defn get-fluid-spec [& _] (unavailable :get-fluid-spec))
(defn get-item-registry-name [& _] (unavailable :get-item-registry-name))
(defn get-item-spec [& _] (unavailable :get-item-spec))
(defn get-loot-injections-for-table [& _] (unavailable :get-loot-injections-for-table))
(defn get-particle-registry-name [& _] (unavailable :get-particle-registry-name))
(defn get-particle-spec [& _] (unavailable :get-particle-spec))
(defn get-sound-registry-name [& _] (unavailable :get-sound-registry-name))
(defn get-tile-block-ids [& _] (unavailable :get-tile-block-ids))
(defn get-tile-registry-name [& _] (unavailable :get-tile-registry-name))
(defn has-block-entity? [& _] (unavailable :has-block-entity?))
(defn has-block-state-properties? [& _] (unavailable :has-block-state-properties?))
(defn fluid-block? [& _] (unavailable :fluid-block?))
(defn should-create-block-item? [& _] (unavailable :should-create-block-item?))
(defn get-all-definitions [& _] (unavailable :get-all-blockstate-definitions))
(defn get-block-state-definition [& _] (unavailable :get-block-state-definition))
(defn is-multipart-block? [& _] (unavailable :is-multipart-block?))
(defn get-model-cube-texture-config [& _] (unavailable :get-model-cube-texture-config))
(defn get-model-texture-config [& _] (unavailable :get-model-texture-config))
(defn get-item-model-id [& _] (unavailable :get-item-model-id))

(def ^:private operation-vars
  {:get-all-block-ids #'get-all-block-ids
   :get-all-creative-tab-entries #'get-all-creative-tab-entries
   :get-all-effect-ids #'get-all-effect-ids
   :get-all-fluid-ids #'get-all-fluid-ids
   :get-all-item-ids #'get-all-item-ids
   :get-all-particle-ids #'get-all-particle-ids
   :get-all-sound-ids #'get-all-sound-ids
   :get-all-tile-ids #'get-all-tile-ids
   :get-block-registry-name #'get-block-registry-name
   :get-block-spec #'get-block-spec
   :get-block-tile-id #'get-block-tile-id
   :get-effect-registry-name #'get-effect-registry-name
   :get-effect-spec #'get-effect-spec
   :get-fluid-id-for-block #'get-fluid-id-for-block
   :get-fluid-registry-name #'get-fluid-registry-name
   :get-fluid-spec #'get-fluid-spec
   :get-item-registry-name #'get-item-registry-name
   :get-item-spec #'get-item-spec
   :get-loot-injections-for-table #'get-loot-injections-for-table
   :get-particle-registry-name #'get-particle-registry-name
   :get-particle-spec #'get-particle-spec
   :get-sound-registry-name #'get-sound-registry-name
   :get-tile-block-ids #'get-tile-block-ids
   :get-tile-registry-name #'get-tile-registry-name
   :has-block-entity? #'has-block-entity?
   :has-block-state-properties? #'has-block-state-properties?
   :fluid-block? #'fluid-block?
   :should-create-block-item? #'should-create-block-item?
   :get-all-blockstate-definitions #'get-all-definitions
   :get-block-state-definition #'get-block-state-definition
   :is-multipart-block? #'is-multipart-block?
   :get-model-cube-texture-config #'get-model-cube-texture-config
   :get-model-texture-config #'get-model-texture-config
   :get-item-model-id #'get-item-model-id})

(defn install!
  "Install the complete, validated provider callback map during bootstrap."
  [operations]
  (let [expected (set (keys operation-vars))]
    (when (or (not= expected (set (keys operations)))
              (some (complement ifn?) (vals operations)))
      (throw (ex-info "Registry metadata provider contract mismatch"
                      {:expected (sort expected)
                       :actual (sort (keys operations))})))
    (doseq [[operation target-var] operation-vars]
      (alter-var-root target-var (constantly (get operations operation)))))
  nil)
