(ns cn.li.ac.block.machine.render-runtime
  "Shared client render runtime: lazy resource baking, per-tile render caches,
  renderer init registration."
  (:require [cn.li.mcmod.client.render.init :as render-init]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap]))

;; ============================================================================
;; Lazy resource baking — one holder var per renderer namespace
;; ============================================================================

(def ^:private resource-bake-lock
  "Render layer's sole remaining lock: guards the double-checked-locking bake
  in lazy-resources below. One shared lock is fine — each holder-var bakes
  exactly once per JVM lifetime, so contention only exists at first-render."
  (Object.))

(defn lazy-resources
  "Return a thunk that bakes every {key loader-fn} pair in `loaders` exactly
  once (under resource-bake-lock, double-checked), stores the resolved map in
  `holder-var`, and returns that map on every call thereafter (plain root
  deref — no thread-binding frame, no per-key locking after first bake)."
  [holder-var loaders]
  (fn []
    (or (var-get holder-var)
        (locking resource-bake-lock
          (or (var-get holder-var)
              (let [m (reduce-kv (fn [acc k loader] (assoc acc k (loader))) {} loaders)]
                (alter-var-root holder-var (constantly m))
                m))))))

;; ============================================================================
;; Per-tile render caches — Framework-backed, replaces create-cache-runtime +
;; ^:dynamic pointer + with-bound-runtime. Render only ever runs client-side,
;; post-framework-injection, so this is intentionally fail-fast (no framework
;; -> ex-info) rather than falling back to a fresh atom every call, which
;; would silently drop cached state instead of preserving it.
;; ============================================================================

(defonce ^:private ^HashMap render-caches (HashMap.))

(defn render-cache
  [cache-key initial-value]
  (if (.containsKey render-caches cache-key)
    (.get render-caches cache-key)
    (do (.put render-caches cache-key initial-value) initial-value)))

(defn render-cache-snapshot
  [cache-key initial-value]
  (render-cache cache-key initial-value))

(defn put-render-cache!
  [cache-key value]
  (.put render-caches cache-key value)
  value)

(defn clear-render-cache!
  [cache-key initial-value]
  (.put render-caches cache-key initial-value)
  nil)

(defn reset-render-cache-for-test!
  [cache-key initial-value value]
  (.put render-caches cache-key value)
  nil)

(defn register-client-renderer-init!
  [init-ref]
  (let [init-fn (cond
                  (fn? init-ref) init-ref
                  (var? init-ref) init-ref
                  (symbol? init-ref)
                  (or (try (requiring-resolve init-ref)
                           (catch Exception _ nil))
                      (fn []
                        (throw (ex-info "Failed to resolve renderer init symbol"
                                        {:symbol init-ref}))))
                  :else
                  (fn []
                    (throw (ex-info "Unsupported renderer init ref"
                                    {:value init-ref
                                     :type (type init-ref)}))))]
    (render-init/register-renderer-init-fn! init-fn)
    (log/debug "Registered client renderer init" init-ref)))
