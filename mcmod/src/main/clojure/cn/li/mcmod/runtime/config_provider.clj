(ns cn.li.mcmod.runtime.config-provider
  "Neutral configuration provider for platform AOT adapters."
  (:require [cn.li.mcmod.config :as config]
            [cn.li.mcmod.config.registry :as registry]
            [cn.li.mcmod.config.script-render :as script-render]))

(defn runtime-provider
  "Provider map for the neutral config facade.

  Operations are Vars, not dereferenced functions, so a `with-redefs` against
  these implementation namespaces stays visible through the facade — the same
  contract the other runtime providers and the test-support facades rely on.
  `:mod-id` is exempt: install! calls it once and stores the value."
  [_]
  {:mod-id (constantly config/mod-id)
   :asset-path #'config/asset-path
   :namespaced-path #'config/namespaced-path
   :config-file-path #'config/config-file-path
   :get-all-config-domains #'registry/get-all-config-domains
   :get-config-descriptors #'registry/get-config-descriptors
   :get-config-values #'registry/get-config-values
   :set-config-value! #'registry/set-config-value!
   :set-config-values! #'registry/set-config-values!
   :disabled-renderer-ids #'script-render/disabled-renderer-ids
   :init-descriptors! #'script-render/init-descriptors!
   :script-render-enabled? #'script-render/script-render-enabled?})
