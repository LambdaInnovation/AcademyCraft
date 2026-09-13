(ns cn.li.test-support.runtime-facades
  "Install the same neutral runtime facades used by platform bootstrap for
  isolated Clojure tests.

  The values intentionally remain Vars rather than dereferenced functions.
  Existing tests use `with-redefs` against the legacy runtime namespaces; Var
  delegation keeps those redefinitions visible to the AOT-facing facades."
  (:require [cn.li.mcmod.content.registry :as content-registry]
            [cn.li.mcmod.hooks.messages :as messages]
            [cn.li.mcmod.hooks.tutorial-events :as tutorial-events]
            [cn.li.mcmod.integration.energy-conversion :as energy-conversion]
            [cn.li.mcmod.integration.energy-hooks :as energy-hooks]
            [cn.li.mcmod.integration.runtime-hooks :as legacy-integration]
            [cn.li.mcmod.network.binary-codec :as binary-codec]
            [cn.li.mcmod.network.client :as client]
            [cn.li.mcmod.network.server :as server]
            [cn.li.mcmod.protocol.metadata :as metadata-provider]
            [cn.li.mcmod.runtime.block-runtime-provider :as block-runtime-provider]
            [cn.li.mcmod.runtime.blockstate-provider :as blockstate-provider]
            [cn.li.mcmod.runtime.client-network-provider :as client-network-provider]
            [cn.li.mcmod.runtime.client-render-provider :as client-render-provider]
            [cn.li.mcmod.runtime.client-runtime-provider :as client-runtime-provider]
            [cn.li.mcmod.runtime.command-runtime-provider :as command-runtime-provider]
            [cn.li.mcmod.runtime.config-provider :as config-provider]
            [cn.li.mcmod.runtime.event-runtime-provider :as event-runtime-provider]
            [cn.li.mcmod.runtime.gui-runtime-provider :as gui-runtime-provider]
            [cn.li.mcmod.runtime.hooks-provider :as hooks-provider]
            [cn.li.mcmod.runtime.integration-runtime-provider :as integration-runtime-provider]
            [cn.li.mcmod.runtime.keyboard-input-provider :as keyboard-input-provider]
            [cn.li.mcmod.runtime.network-runtime-provider :as network-runtime-provider]
            [cn.li.mcmod.runtime.tabbed-gui-provider :as tabbed-gui-provider]
            [cn.li.mcmod.runtime.ui-provider :as ui-provider]
            [cn.li.platform.neutral.block-runtime :as block-runtime]
            [cn.li.platform.neutral.client-network :as client-network]
            [cn.li.platform.neutral.client-render :as client-render]
            [cn.li.platform.neutral.client-runtime :as client-runtime]
            [cn.li.platform.neutral.command-runtime :as command-runtime]
            [cn.li.platform.neutral.config :as config]
            [cn.li.platform.neutral.event-runtime :as event-runtime]
            [cn.li.platform.neutral.gui-runtime :as gui-runtime]
            [cn.li.platform.neutral.hooks :as hooks]
            [cn.li.platform.neutral.integration-runtime :as integration-runtime]
            [cn.li.platform.neutral.keyboard-input :as keyboard-input]
            [cn.li.platform.neutral.network-runtime :as network-runtime]
            [cn.li.platform.neutral.tabbed-gui :as tabbed-gui]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.platform.neutral.ui :as ui]))

(def ^:private facade-installers
  "Every neutral facade cn.li.platform.bootstrap/initialize-common-content!
  installs, paired with the production provider that feeds it.

  Hand-maintaining a subset is what let the suite drift: a test that reached
  code behind an uninstalled facade failed with `provider is unavailable`
  rather than exercising anything. Keep this list complete — one entry per
  cn.li.mcmod.runtime.*-provider namespace."
  [[#'hooks/install! (fn [] (hooks-provider/runtime-provider nil))]
   [#'config/install! (fn [] (config-provider/runtime-provider nil))]
   [#'registry-metadata/install! (fn []
                                   (merge (metadata-provider/runtime-provider nil)
                                          (blockstate-provider/runtime-provider nil)))]
   [#'tabbed-gui/install! (fn [] (tabbed-gui-provider/runtime-provider nil))]
   [#'keyboard-input/install! (fn [] (keyboard-input-provider/runtime-provider nil))]
   [#'client-runtime/install! (fn [] (client-runtime-provider/runtime-provider nil))]
   [#'block-runtime/install! (fn [] (block-runtime-provider/runtime-provider nil))]
   [#'event-runtime/install! (fn [] (event-runtime-provider/runtime-provider nil))]
   [#'command-runtime/install! (fn [] (command-runtime-provider/runtime-provider nil))]
   [#'gui-runtime/install! (fn [] (gui-runtime-provider/runtime-provider nil))]
   [#'client-render/install! (fn [] (client-render-provider/runtime-provider nil))]
   [#'client-network/install! (fn [] (client-network-provider/runtime-provider nil))]
   [#'integration-runtime/install! (fn [] (integration-runtime-provider/runtime-provider nil))]
   [#'network-runtime/install! (fn [] (network-runtime-provider/runtime-provider nil))]
   [#'ui/install! (fn [] (ui-provider/runtime-provider nil))]])

(defn install!
  "Install complete, test-safe provider maps for neutral AOT facades."
  []
  (doseq [[install-var provider-fn] facade-installers]
    (install-var (provider-fn)))
  ;; Var-delegating overrides for the facades whose suites redefine the legacy
  ;; implementation namespaces directly.
  (client-network/install!
    {:register-request-transport! #'client/register-request-transport!
     :send-to-server #'client/send-to-server
     :clear-client-session-state! #'client/clear-client-session-state!
     :handle-push #'client/handle-push
     :handle-response #'client/handle-response})
  (network-runtime/install!
    {:encode #'binary-codec/encode
     :decode #'binary-codec/decode
     :list-descriptors #'content-registry/list-descriptors
     :handle-request #'server/handle-request})
  (integration-runtime/install!
    {:content-to-fe #'energy-conversion/content-to-fe
     :fe-to-content #'energy-conversion/fe-to-content
     :validate-conversion-rate #'energy-conversion/validate-conversion-rate
     :forge-energy-conversion-rate #'energy-hooks/forge-energy-conversion-rate
     :ic2-energy-conversion-rate #'energy-hooks/ic2-energy-conversion-rate
     :jei-get-all-categories #'legacy-integration/jei-get-all-categories
     :jei-get-recipes #'legacy-integration/jei-get-recipes
     :jei-format-recipe #'legacy-integration/jei-format-recipe
     :get-jei-nbt-subtype-item-ids #'legacy-integration/get-jei-nbt-subtype-item-ids
     :msg-id #'messages/msg-id
     :on-item-event! #'tutorial-events/on-item-event!
     :process-pending-activations! #'tutorial-events/process-pending-activations!
     :register-tutorial-activated-hook! #'tutorial-events/register-tutorial-activated-hook!})
  nil)
