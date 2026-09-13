(ns cn.li.ac.wireless.gui.message.bootstrap
  "Centralized wireless message registration.

  This provides the single startup registration path for wireless GUI messages."
  (:require [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]))

(defn register-all!
  []
  (doseq [[domain actions] gui-manifest/message-domain-actions]
    (msg-registry/register-block-messages!
      domain
      actions
      (gui-manifest/message-domain-contract domain)))
  (msg-registry/freeze-registry!)
  true)
