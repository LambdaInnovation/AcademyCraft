(ns cn.li.platform.lifecycle.manifest
  "Shared lifecycle phase builder for platform loaders.

  Loaders own concrete phase manifests and inject action fns. This namespace
  validates the manifest and turns it plus an action map into an executable
  lifecycle spec for cn.li.platform.lifecycle.orchestrator/run-lifecycle!.")

(def ^:private phase-descriptions
  {:platform-init "platform bootstrap + init-from-java"
   :runtime-activation "activate runtime/config foundations"
   :resource-init "initialize blockstate/resource definitions"
   :content-registration "register content"
   :mod-bus-setup "wire deferred registers and lifecycle listeners"
   :common-setup "run common setup side effects"
   :event-wiring "register loader events"})

(defn- phase-description
  [phase-id fallback]
  (or (get phase-descriptions phase-id) fallback))

(defn- phase
  [id desc f]
  {:id id
   :desc (phase-description id desc)
   :fn f})

(defn- action-fn
  [label phase-id action-key action-map]
  (let [action (get action-map action-key)]
    (when-not (fn? action)
      (throw (ex-info "Missing lifecycle phase action"
                      {:target label
                       :phase phase-id
                       :action action-key
                       :available-actions (sort (keys action-map))})))
    action))

(defn- run-actions!
  [label phase-id action-keys action-map]
  (doseq [action-key action-keys]
    ((action-fn label phase-id action-key action-map))))

(defn build-lifecycle
  "Build a lifecycle-orchestrator spec from a loader-owned manifest and action-map.

  manifest: {:label string :phases [{:id keyword :desc string? :actions [keyword]}]}
  action-map: {action-keyword (fn [])}"
  [{:keys [label phases] :as manifest} action-map]
  (when-not (seq phases)
    (throw (ex-info "Lifecycle manifest requires at least one phase"
                    {:manifest manifest})))
  {:label label
   :phases (mapv (fn [{:keys [id desc actions]}]
                   (phase id desc #(run-actions! label id actions action-map)))
                 phases)})
