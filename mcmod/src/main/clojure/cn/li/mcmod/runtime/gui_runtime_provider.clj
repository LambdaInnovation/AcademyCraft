(ns cn.li.mcmod.runtime.gui-runtime-provider
  (:require [cn.li.mcmod.gui.registry :as registry]
            [cn.li.mcmod.gui.handler :as handler]
            [cn.li.mcmod.gui.container-state :as state]
            [cn.li.mcmod.gui.adapter.platform-registry :as platform]
            [cn.li.mcmod.gui.container.data-slot-codec :as codec]
            [cn.li.mcmod.gui.slot-registry :as slots]))

(defn runtime-provider [_]
  {:get-all-gui-ids #'registry/get-all-gui-ids :get-registry-name #'registry/get-registry-name
   :get-display-name #'registry/get-display-name :get-slot-layout #'registry/get-slot-layout
   :get-slot-range #'registry/get-slot-range :get-screen-factory-fn #'registry/get-screen-factory-fn
   :get-screen-factory-fn-kw #'registry/get-screen-factory-fn-kw
   :get-gui-handler #'handler/get-gui-handler :get-server-container #'handler/get-server-container
   :get-container-for-menu #'state/get-container-for-menu :owner-from-container #'state/owner-from-container
   :register-menu-container! #'state/register-menu-container! :unregister-menu-container! #'state/unregister-menu-container!
   :get-gui-id-for-container #'platform/get-gui-id-for-container :safe-close! #'platform/safe-close!
   :safe-validate #'platform/safe-validate :server-menu-sync! #'platform/server-menu-sync!
   :slot-can-place? #'platform/slot-can-place? :slot-changed! #'platform/slot-changed!
   :slot-count #'platform/slot-count :slot-get-item #'platform/slot-get-item :slot-set-item! #'platform/slot-set-item!
   :clamp-int #'codec/clamp-int :get-slot-validator #'slots/get-slot-validator})
