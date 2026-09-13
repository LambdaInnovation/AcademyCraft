(ns cn.li.neoforge262.runtime.item-handler
  "Install owner binding then re-export shared NeoForge item-handler."
  (:require [cn.li.neoforgebase.runtime.item-handler :as shared]
            [cn.li.neoforge262.runtime.owner :as runtime-owner]))

(shared/install-with-player-owner! runtime-owner/with-player-owner)

(def init! shared/init!)
