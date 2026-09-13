(ns cn.li.ac.gui.registry-verify
  "Post-init verification for wireless block GUI message/handler contracts."
  (:require [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.wireless.gui.message.api :as wireless-msgs]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.network.server :as net-server]))

(defn verify-wireless-message-handler-registration!
  "Ensure every wireless catalog message has a server handler with matching contract."
  []
  (registry-contract/verify-catalog-handlers!
   (:handlers (net-server/handlers-snapshot))
   (wireless-msgs/catalog)
   gui-manifest/message-domain-contracts)
  nil)

(defn finalize-gui-network-registration!
  "Verify wireless GUI handler contracts, then freeze handler/message registries."
  []
  (verify-wireless-message-handler-registration!)
  (net-server/freeze-handlers!)
  (when-not (:frozen? (msg-registry/registry-snapshot))
    (msg-registry/freeze-registry!))
  nil)
