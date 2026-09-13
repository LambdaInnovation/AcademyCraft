(ns cn.li.mcmod.framework.platform
  "Facade API for platform adapter function maps (:platform/* sub-namespace).

   Replaces the defprotocol + ^:dynamic *runtime* + alter-var-root pattern
   with plain function maps stored in the framework atom.

   Each platform domain (e.g. :world-ops, :structured-data-ops, :entity-damage) is a
   plain Clojure map of {:operation-key (fn [args] ...), ...}.

   Benefits over defprotocol + dynamic var:
     - No protocol bytecode class generation (AOT class-loading race)
     - No ThreadLocal dependency (async thread safety)
     - Function references follow the framework instance — valid on any thread"
  )

(defn install-adapter!
  "Install a platform adapter function map.

   Called once at bootstrap by platform-specific code (Forge/Fabric).
   Overwrites any existing adapter for the same key.

   Args:
     fw          — framework atom instance
     adapter-key — keyword identifying the adapter, e.g. :world-ops, :structured-data-ops
     impl-map    — plain map of {:operation-key (fn [args] ...), ...}"
  [fw adapter-key impl-map]
  (swap! fw assoc-in [:platform adapter-key] impl-map)
  nil)

(defn get-adapter
  "Read a platform adapter function map.

   Returns nil if the adapter has not been installed yet.

   Args:
     fw          — framework atom instance
     adapter-key — keyword identifying the adapter"
  [fw adapter-key]
  (get-in @fw [:platform adapter-key]))

(defn adapter-installed?
  "Check if a platform adapter has been installed."
  [fw adapter-key]
  (boolean (get-in @fw [:platform adapter-key])))

(defn call-adapter-optional
  "Call an optional adapter operation, returning nil when it is unavailable."
  [fw adapter-key fn-key & args]
  (when-let [f (get-in @fw [:platform adapter-key fn-key])]
    (apply f args)))

(defn call-adapter
  "Call a required adapter operation and fail when it is unavailable."
  [fw adapter-key fn-key & args]
  (let [adapter (get-in @fw [:platform adapter-key])
        f (get adapter fn-key)]
    (when-not f
      (throw (ex-info "Required platform adapter operation is not installed"
                      {:adapter adapter-key
                       :operation fn-key
                       :installed (keys adapter)})))
    (apply f args)))
