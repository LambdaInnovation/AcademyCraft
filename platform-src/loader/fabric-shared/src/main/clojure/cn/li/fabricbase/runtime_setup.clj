(ns cn.li.fabricbase.runtime-setup
  "Shared Fabric runtime installation sequence.

  Minecraft-version adapters supply their runtime-install-step factory and GUI
  initializers.  Keeping the ordering here prevents Fabric targets from
  drifting while preserving version-specific API bindings at their boundary."
  (:require [cn.li.fabricbase.runtime :as fabric-runtime]
            [cn.li.mcbase.runtime.adapter-registry :as adapter-registry]
            [cn.li.platform.target :as target]))

(defn- resolve-runtime-install-steps
  [runtime-install-steps]
  (let [steps (if (fn? runtime-install-steps)
                (runtime-install-steps)
                runtime-install-steps)]
    (when-not (sequential? steps)
      (throw (ex-info "Fabric runtime adapter steps must be sequential or supplied by a callable factory"
                      {:runtime-install-steps runtime-install-steps})))
    steps))

(defn preload-runtime-adapters!
  "Validate the adapter plan while the platform runtime is still being
  initialized.  Targets may provide a static plan or a side-effect-free
  factory; executing the plan remains in install-runtime!."
  [runtime-install-steps]
  (resolve-runtime-install-steps runtime-install-steps)
  nil)

(defn install-runtime!
  "Install one version adapter's runtime callbacks in the shared Fabric order."
  [{:keys [runtime-install-steps init-common-gui! init-server-gui!]}]
  (let [runtime-adapters (resolve-runtime-install-steps runtime-install-steps)
        target-model (target/current-target!)]
    (adapter-registry/run-install-steps! (:id target-model) runtime-adapters)
    (init-common-gui!)
    (init-server-gui!)
    (fabric-runtime/install! {:target target-model
                               :runtime-adapters runtime-adapters
                               :gui-init true}))
  nil)
