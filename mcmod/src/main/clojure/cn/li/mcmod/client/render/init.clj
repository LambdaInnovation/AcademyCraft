(ns cn.li.mcmod.client.render.init
  "CLIENT-ONLY: Renderer registration system.

  This namespace must be loaded via side-checked requiring-resolve from the
  platform layer. It provides platform-agnostic renderer registration that
  delegates to platform-specific implementations.

  Single entrypoint for registering all core renderers. Platform client init
  should call `register-all-renderers!` once; individual renderer namespaces
  keep their own `register!` implementations."
  (:require [cn.li.mcmod.lifecycle :as lifecycle]
            [cn.li.mcmod.util.log :as log]))

(defn create-render-init-runtime
  []
  {:renderer-init-fns []})

(def ^:private ^:dynamic *renderer-init-fns*
  [])

(defn renderer-init-fns-snapshot
  []
  *renderer-init-fns*)

(defn register-renderer-init-fn!
  "Register a single renderer init callback. Called by renderer namespaces at load time."
  [f]
  (alter-var-root #'*renderer-init-fns* conj f)
  nil)

(defn register-renderer-init-fns!
  "Register renderer init callbacks to be invoked by `register-all-renderers!`.
   Each callback should be a (fn [] ...) or a Var referencing such a function."
  [fns]
  (alter-var-root #'*renderer-init-fns* (constantly (vec fns)))
  nil)

(defn register-default-renderer-init-fns!
  "Trigger client-side initialization callbacks that load renderer namespaces.

  This function is called by platform layer during client setup.
  Content modules register their callbacks via lifecycle/register-client-init!."
  []
  (lifecycle/run-client-init!)
  nil)

(defn register-all-renderers!
  "Run every queued `register!` callback (typically once from platform client setup).

  Intentionally not gated by a \"ran once\" flag: if this ever ran while the
  callback queue was still empty, a compare-and-swap gate would permanently
  skip real registrations (matrix/solar TESRs would never attach)."
  []
  (let [fns (renderer-init-fns-snapshot)]
    (log/info "Registering all core renderers..." (count fns) "callbacks")
    (when (empty? fns)
      (log/warn "No renderer init callbacks registered; skipping."))
    (doseq [f fns]
      (try
        (f)
        (catch Throwable t
          (log/stacktrace "Renderer init callback failed" t)
          (log/stacktrace "Renderer init callback" t)))))
  (log/info "Core renderers registered."))

