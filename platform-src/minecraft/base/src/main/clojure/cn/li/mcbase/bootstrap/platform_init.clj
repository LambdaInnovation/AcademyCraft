(ns cn.li.mcbase.bootstrap.platform-init
  "Shared platform bootstrap install wrappers.

  Version modules install installer-core / accessor-registry hooks, then call
  their menu-bridge-install. Loaders call the shared install entrypoints.")

(defonce ^:private hooks-atom (atom nil))

(defn install-platform-init-hooks!
  "Install map with :install-platform-core! :install-platform-services!
   :init-default-accessors!."
  [m]
  (reset! hooks-atom m)
  m)

(defn- hooks []
  (let [m @hooks-atom]
    (when (nil? m)
      (throw (IllegalStateException. "platform-init hooks not installed")))
    m))

(defn install-platform-core!
  "Install the full shared platform core for adapters that can provide
  all required world/block/entity/item operations through PlatformAdapter."
  [adapter]
  ((:install-platform-core! (hooks)) adapter)
  ((:init-default-accessors! (hooks))))

(defn install-platform-services!
  [adapter world-fns-map be-fns-map]
  ((:install-platform-services! (hooks)) adapter world-fns-map be-fns-map)
  ((:init-default-accessors! (hooks))))
