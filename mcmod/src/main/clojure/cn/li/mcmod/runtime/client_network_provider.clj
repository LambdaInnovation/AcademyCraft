(ns cn.li.mcmod.runtime.client-network-provider
  "Neutral client-network callbacks consumed by platform packet bridges."
  (:require [cn.li.mcmod.network.client :as client]))

(defn runtime-provider
  [_]
  {:register-request-transport! #'client/register-request-transport!
   :send-to-server #'client/send-to-server
   :clear-client-session-state! #'client/clear-client-session-state!
   :handle-push #'client/handle-push
   :handle-response #'client/handle-response})
