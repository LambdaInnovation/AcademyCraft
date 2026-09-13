(ns cn.li.mcmod.runtime.event-runtime-provider
  (:require [cn.li.mcmod.events.dispatcher :as dispatcher]
            [cn.li.mcmod.events.interaction-result :as result]
            [cn.li.mcmod.events.world-lifecycle :as lifecycle]
            [cn.li.mcmod.events.world-save-cache :as save-cache]
            [cn.li.mcmod.events.world-state-notify :as state-notify]
            [cn.li.mcmod.events.world-owner-key :as owner-key]))

(defn runtime-provider [_]
  {:on-block-right-click #'dispatcher/on-block-right-click
   :on-block-place #'dispatcher/on-block-place
   :on-block-break #'dispatcher/on-block-break
   :interaction-consumed? #'result/interaction-consumed?
   :gui-open-result? #'result/gui-open-result?
   :dispatch-world-load #'lifecycle/dispatch-world-load
   :dispatch-world-unload #'lifecycle/dispatch-world-unload
   :dispatch-world-save #'lifecycle/dispatch-world-save
   :dispatch-world-tick #'lifecycle/dispatch-world-tick
   :remember-saved-data! #'save-cache/remember-saved-data!
   :consume-saved-data! #'save-cache/consume-saved-data!
   :clear-world-saved-data! #'save-cache/clear-world-saved-data!
   :set-on-world-state-changed-fn! #'state-notify/set-on-world-state-changed-fn!
   :world-key #'owner-key/world-key})
