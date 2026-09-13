(ns cn.li.fabricbase.lifecycle
  "Loader-neutral Fabric lifecycle coordinator.

  Version adapters only provide action functions; phase ordering lives here so
  Fabric targets cannot silently diverge as new Minecraft versions are added."
  (:require [cn.li.platform.lifecycle.manifest :as manifest]
            [cn.li.platform.lifecycle.orchestrator :as orchestrator]
            [cn.li.platform.target :as target]))

(defn- lifecycle-manifest []
  {:label (:id (target/current-target!))
   :phases [{:id :platform-init
             ;; AOT platform namespaces that reference neutral facades must
             ;; finish their namespace initialization before the facade roots
             ;; are replaced with provider IFns in :init-from-java!.
             :actions [:init-platform! :preload-platform-runtime! :init-from-java!]}
            {:id :runtime-activation
             :actions [:load-config! :activate-runtime-content!]}
            {:id :resource-init
             :actions [:init-blockstate-properties!]}
            {:id :content-registration
             :actions [:register-content!]}
            {:id :common-setup
             :actions [:install-runtime!]}
            {:id :mod-bus-setup
             :actions [:register-events!]}]})

(defn init!
  "Run the shared Fabric lifecycle with a version adapter action map."
  [action-map]
  (orchestrator/run-lifecycle!
   (manifest/build-lifecycle (lifecycle-manifest) action-map))
  nil)
