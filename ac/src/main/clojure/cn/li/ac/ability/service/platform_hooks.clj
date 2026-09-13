(ns cn.li.ac.ability.service.platform-hooks
  "Platform function registry for dependency injection.
   Replaces requiring-resolve anti-pattern by maintaining a registry
   of platform-specific functions registered at bootstrap time.")

(def ^:private platform-fn-registry (atom {}))

(defn register-platform-fn!
  "Register a platform function implementation."
  [fn-ref impl-fn]
  (swap! platform-fn-registry assoc fn-ref impl-fn) nil)

(defn get-platform-fn
  "Retrieve a registered platform function. Throws if not found."
  [fn-ref]
  (if-let [impl-fn (get @platform-fn-registry fn-ref)]
    impl-fn
    (throw (ex-info "Platform function not registered" {:fn-ref fn-ref}))))

(defn platform-fn-registered?
  "Check if a platform function is registered."
  [fn-ref]
  (contains? @platform-fn-registry fn-ref))

(defn list-platform-fns
  "List all registered platform functions."
  []
  (keys @platform-fn-registry))

(defn reset-platform-fns!
  "Clear all registered platform functions (mainly for testing)."
  []
  (reset! platform-fn-registry {}) nil)
