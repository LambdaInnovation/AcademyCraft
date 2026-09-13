(ns cn.li.platform.bootstrap
  (:require [cn.li.platform.target :as target]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.platform.neutral.config :as config]
            [cn.li.platform.neutral.hooks :as hooks]
            [cn.li.platform.neutral.tabbed-gui :as tabbed-gui]
            [cn.li.platform.neutral.client-network :as client-network]
            [cn.li.platform.neutral.keyboard-input :as keyboard-input]
            [cn.li.platform.neutral.client-runtime :as client-runtime]
            [cn.li.platform.neutral.block-runtime :as block-runtime]
            [cn.li.platform.neutral.event-runtime :as event-runtime]
            [cn.li.platform.neutral.command-runtime :as command-runtime]
            [cn.li.platform.neutral.gui-runtime :as gui-runtime]
            [cn.li.platform.neutral.client-render :as client-render]
            [cn.li.platform.neutral.integration-runtime :as integration-runtime]
            [cn.li.platform.neutral.network-runtime :as network-runtime]
            [cn.li.platform.neutral.ui :as ui]))

(defn- require-resolve! [namespace-name symbol-name]
  (let [ns-sym (symbol namespace-name) var-sym (symbol symbol-name)]
    (require ns-sym)
    (or (ns-resolve ns-sym var-sym)
        (throw (ex-info "Platform target entrypoint missing"
                        {:namespace namespace-name :symbol symbol-name})))))

(defn- verify-capability-owners! [{:keys [id capabilities capability-owners]}]
  (let [owners (into {} (map (fn [[cap owner-list]]
                               [(if (keyword? cap) (name cap) (str cap)) owner-list])
                             (or capability-owners {})))
        missing (remove #(contains? owners %) capabilities)
        duplicate (keep (fn [[cap owner-list]]
                          (when (not= 1 (count owner-list)) [cap owner-list])) owners)]
    (when (seq missing)
      (throw (ex-info "Platform target has capabilities without owners"
                      {:target id :missing (vec missing)})))
    (when (seq duplicate)
      (throw (ex-info "Platform target capability must have exactly one owner"
                      {:target id :duplicate (into {} duplicate)})))))

(defn start! []
  (let [target-model (target/current-target!)]
    (verify-capability-owners! target-model)
    (let [{:keys [entrypoint]} target-model]
      ((require-resolve! (:namespace entrypoint) (:function entrypoint))))))

(defn initialize-common-content!
  "Invoke the fixed neutral common-content bootstrap during platform startup.

   This is deliberately not a general symbol resolver. The target entrypoint
   is already AOT/remapped; only the verified neutral bootstrap is loaded from
   source, before listener registration begins."
  [target-model]
  (let [bootstrap (requiring-resolve 'cn.li.mcmod.runtime.bootstrap/initialize-common-content!)]
    (let [providers (bootstrap target-model)
          operations (merge (:registry-metadata providers)
                            (:blockstate-metadata providers))]
      (when (seq operations)
        (registry-metadata/install! operations))
      (when-let [operations (:config providers)]
        (config/install! operations))
      (when-let [operations (:hooks providers)]
        (hooks/install! operations))
      (when-let [operations (:tabbed-gui providers)]
        (tabbed-gui/install! operations))
      (when-let [operations (:client-network providers)]
        (client-network/install! operations))
      (when-let [operations (:keyboard-input providers)]
        (keyboard-input/install! operations))
      (when-let [operations (:client-runtime providers)]
        (client-runtime/install! operations))
      (when-let [operations (:block-runtime providers)]
        (block-runtime/install! operations))
      (when-let [operations (:event-runtime providers)]
        (event-runtime/install! operations))
      (when-let [operations (:command-runtime providers)]
        (command-runtime/install! operations))
      (when-let [operations (:gui-runtime providers)]
        (gui-runtime/install! operations))
      (when-let [operations (:client-render providers)]
        (client-render/install! operations))
      (when-let [operations (:integration-runtime providers)]
        (integration-runtime/install! operations))
      (when-let [operations (:network-runtime providers)]
        (network-runtime/install! operations))
      (when-let [operations (:ui providers)]
        (ui/install! operations))))
  nil)

(defn initialize-datagen-content!
  "Invoke the fixed neutral datagen bootstrap during platform datagen setup."
  [target-model]
  (let [bootstrap (requiring-resolve 'cn.li.mcmod.runtime.bootstrap/initialize-datagen-content!)]
    (bootstrap target-model))
  nil)

(defn- neutral-lifecycle-callback! [operation]
  (let [callbacks ((requiring-resolve 'cn.li.mcmod.runtime.bootstrap/lifecycle-callbacks))
        callback (get callbacks operation)]
    (when-not (ifn? callback)
      (throw (ex-info "Neutral lifecycle callback is not callable"
                      {:operation operation :available (keys callbacks)})))
    callback))

(defn world-tick-callback!
  "Resolve the neutral world-tick operation once for listener registration.

   Callers retain the returned concrete IFn in their platform listener map;
   tick execution does not resolve namespaces or traverse Framework maps."
  []
  (neutral-lifecycle-callback! :world-tick!))

(defn runtime-content-activation-callback! []
  (neutral-lifecycle-callback! :runtime-content-activation!))

(defn post-spi-client-init-callback! []
  (neutral-lifecycle-callback! :post-spi-client-init!))

(defn client-init-callback! []
  (neutral-lifecycle-callback! :client-init!))
