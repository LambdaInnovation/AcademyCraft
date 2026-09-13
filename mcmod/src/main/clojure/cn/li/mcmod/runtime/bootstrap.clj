(ns cn.li.mcmod.runtime.bootstrap
  "Neutral initialization operations invoked by platform AOT entrypoints.

   Platform code reaches this namespace only through a fixed
   `requiring-resolve` call in cn.li.platform.bootstrap. Keeping the ordinary
   static requires here, in the neutral source closure, prevents platform AOT
   from pulling these namespaces into the remapped class set."
  (:require [cn.li.mcmod.content :as content]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.lifecycle :as lifecycle]))

(defn- providers-installed?
  "True after the normal mod bootstrap has installed its neutral providers.
   Datagen can run after that bootstrap in the same JVM, so it must not try to
   install the immutable provider maps a second time."
  []
  (boolean
    (some-> (fw/fw-atom)
            deref
            (get-in [:service :runtime-providers])
            seq)))

(defn initialize-common-content!
  "Install common providers, then run the registered common content hooks."
  [target-model]
  (let [providers (content/register-all-content! (:provider-manifests target-model) target-model)]
    (lifecycle/run-content-init!)
    providers))

(defn initialize-datagen-content!
  "Initialize common providers and execute the complete datagen lifecycle."
  [target-model]
  (when-not (providers-installed?)
    (initialize-common-content! target-model))
  (lifecycle/run-runtime-content-activation!)
  (lifecycle/run-datagen-metadata-init!)
  nil)

(defn lifecycle-callbacks
  "Return the concrete neutral lifecycle IFns used by platform listeners.

   This map is assembled during listener registration, never consulted through
   Framework state in a tick/render/network hot path."
  []
  {:world-tick! lifecycle/run-world-tick!
   :runtime-content-activation! lifecycle/run-runtime-content-activation!
   :post-spi-client-init! lifecycle/run-post-spi-client-init!
   :client-init! lifecycle/run-client-init!})
