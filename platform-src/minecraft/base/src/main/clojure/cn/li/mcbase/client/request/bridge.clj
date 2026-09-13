(ns cn.li.mcbase.client.request.bridge
  "Provides a neutral client request dispatch helper for runtime operations."
  (:require [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.util.log :as log]))

(defn dispatch!
  "Dispatch an opaque content action from a client UI/request host."
  ([action-id payload]
   (dispatch! action-id payload nil))
  ([action-id payload callback]
   (power-runtime/dispatch-action! action-id {:callback callback} payload)))

(defn init! []
  (log/debug "Client request bridge initialized"))
