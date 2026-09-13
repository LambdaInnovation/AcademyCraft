(ns cn.li.neoforgebase.setup.lifecycle-init
  "Forge lifecycle coordinator.

  Fabric-style action injection: callers pass phase fns; this namespace owns
  the phase manifest and ForgeBootstrapGuard isolation only."
  (:require [cn.li.mcmod.aot :as aot]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.lifecycle.manifest :as manifest]
            [cn.li.platform.lifecycle.orchestrator :as lifecycle-orchestrator]
            [cn.li.platform.target :as target])
  (:import [cn.li.neoforgebase.bootstrap ForgeBootstrapGuard]))

(defn- lifecycle-manifest []
  {:label (:id (target/current-target!))
   :phases [{:id :platform-init
             :actions [:init-platform!]}
            {:id :runtime-activation
             :desc "activate runtime content"
             :actions [:activate-runtime-content!]}
            {:id :resource-init
             :actions [:init-resource-definitions!]}
            {:id :content-registration
             :actions [:register-content!]}
            {:id :mod-bus-setup
             :actions [:setup-mod-bus!]}]})

(defn- guarded-run!
  "Run body once per process via ForgeBootstrapGuard. Returns nil."
  [body!]
  (if-not (ForgeBootstrapGuard/markLifecycleInitializedIfAbsent)
    (do
      (log/info "[LIFECYCLE] Forge bootstrap already initialized; skipping duplicate invocation")
      nil)
    (try
      (body!)
      (catch Exception e
        (log/stacktrace "Forge initialization lifecycle failed" e)
        (throw (Error. "Critical mod initialization failure" e))))))

(defn init-lifecycle!
  "Run Forge constructor lifecycle phases from an injected action map.

  action-map keys: :init-platform! :activate-runtime-content!
  :init-resource-definitions! :register-content! :setup-mod-bus!"
  [action-map]
  (guarded-run!
   #(lifecycle-orchestrator/run-lifecycle!
     (manifest/build-lifecycle (lifecycle-manifest) action-map)))
  nil)

(defn init-lifecycle-with-error-handling!
  "Run lifecycle with AOT/checkClojure error handling.

  Returns [state exception] for use with gen-class :init contract.
  - state: [] (empty state)
  - exception: nil on success

  Args:
    action-map: injected phase fns (see init-lifecycle!)
    compiling?: boolean from (aot/compiling?)"
  [action-map compiling?]
  (if compiling?
    (do
      (log/warn "[LIFECYCLE] Skipping bootstrap-sensitive path during compilation")
      [[] nil])
    (do
      (aot/ensure-runtime! "cn.li.neoforgebase.setup.lifecycle-init/init-lifecycle-with-error-handling!")
      (init-lifecycle! action-map)
      [[] nil])))
