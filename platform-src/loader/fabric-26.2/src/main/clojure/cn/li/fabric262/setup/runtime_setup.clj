(ns cn.li.fabric262.setup.runtime-setup
  "Fabric runtime setup phase extracted from mod entry."
  (:require [cn.li.fabric262.runtime.adapters.registry :as runtime-adapters-registry]
            [cn.li.fabricbase.runtime-setup :as shared-runtime-setup]
            [cn.li.fabric262.gui.init :as gui-init]))

(defn preload-platform-runtime!
  "Force the AOT runtime-adapter graph to initialize before neutral facade
   roots are installed from validated provider maps."
  []
  (shared-runtime-setup/preload-runtime-adapters!
   runtime-adapters-registry/runtime-install-steps))

(defn install-runtime!
  []
  (shared-runtime-setup/install-runtime!
   {:runtime-install-steps runtime-adapters-registry/runtime-install-steps
    :init-common-gui! gui-init/init-common!
    :init-server-gui! gui-init/init-server!}))
