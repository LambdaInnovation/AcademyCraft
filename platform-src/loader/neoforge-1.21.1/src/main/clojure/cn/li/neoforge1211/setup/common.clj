(ns cn.li.neoforge1211.setup.common
  "Install versioned common-setup steps into neoforgebase.setup.common."
  (:require [cn.li.neoforgebase.setup.common :as shared]
            [cn.li.neoforge1211.setup.shared-event-install :as shared-event-install]
            [cn.li.neoforge1211.gui.init :as gui-init]
            [cn.li.neoforge1211.registry.content-registration :as content-registration]
            [cn.li.neoforge1211.runtime.lifecycle :as runtime-lifecycle]
            [cn.li.neoforge1211.integration.forge-energy :as forge-energy]
            [cn.li.neoforgebase.integration.ic2-energy :as ic2-energy]
            [cn.li.neoforge1211.runtime.item-handler :as runtime-item-handler]
            [cn.li.neoforge1211.integration.tutorial-events :as tutorial-events]
            [cn.li.neoforgebase.integration.imc-dispatch :as imc-dispatch]
            [cn.li.neoforge1211.integration.events.world :as world-events]
            [cn.li.neoforge1211.setup.event-registration :as event-registration]))

(shared/install-common-setup-steps!
  {:assert-scripted-blocks-bundled! content-registration/assert-scripted-blocks-bundled!
   :init-common-gui! gui-init/init-common!
   :init-common-lifecycle! runtime-lifecycle/init-common!
   :init-forge-energy! forge-energy/init-forge-energy!
   :init-ic2-energy! ic2-energy/init-ic2-energy!
   :init-item-handler! runtime-item-handler/init!
   :init-tutorial-events! tutorial-events/init!
   :init-imc! imc-dispatch/init!
   :register-world-state-changed! world-events/register-on-world-state-changed!
   :register-common-event-listeners! event-registration/register-common-event-listeners!})

(shared-event-install/install!)

(def run-common-setup! shared/run-common-setup!)
