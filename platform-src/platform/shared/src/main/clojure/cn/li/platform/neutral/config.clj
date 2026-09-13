(ns cn.li.platform.neutral.config
  "Cached neutral configuration callbacks for AOT/remapped platform code.")

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Config provider is unavailable: " operation))))

(def mod-id nil)
(defn asset-path [& _] (unavailable :asset-path))
(defn namespaced-path [& _] (unavailable :namespaced-path))
(defn config-file-path [& _] (unavailable :config-file-path))
(defn get-all-config-domains [& _] (unavailable :get-all-config-domains))
(defn get-config-descriptors [& _] (unavailable :get-config-descriptors))
(defn get-config-values [& _] (unavailable :get-config-values))
(defn set-config-value! [& _] (unavailable :set-config-value!))
(defn set-config-values! [& _] (unavailable :set-config-values!))
(defn disabled-renderer-ids [& _] (unavailable :disabled-renderer-ids))
(defn init-descriptors! [& _] (unavailable :init-descriptors!))
(defn script-render-enabled? [& _] (unavailable :script-render-enabled?))

(def ^:private operation-vars
  {:mod-id #'mod-id
   :asset-path #'asset-path
   :namespaced-path #'namespaced-path
   :config-file-path #'config-file-path
   :get-all-config-domains #'get-all-config-domains
   :get-config-descriptors #'get-config-descriptors
   :get-config-values #'get-config-values
   :set-config-value! #'set-config-value!
   :set-config-values! #'set-config-values!
   :disabled-renderer-ids #'disabled-renderer-ids
   :init-descriptors! #'init-descriptors!
   :script-render-enabled? #'script-render-enabled?})

(defn install!
  [operations]
  (let [expected (set (keys operation-vars))]
    (when (or (not= expected (set (keys operations)))
              (some (complement ifn?) (vals operations)))
      (throw (ex-info "Config provider contract mismatch"
                      {:expected (sort expected) :actual (sort (keys operations))})))
    (alter-var-root #'mod-id (constantly ((:mod-id operations))))
    (doseq [[operation target-var] (dissoc operation-vars :mod-id)]
      (alter-var-root target-var (constantly (get operations operation)))))
  nil)
