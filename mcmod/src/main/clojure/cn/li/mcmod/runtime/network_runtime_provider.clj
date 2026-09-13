(ns cn.li.mcmod.runtime.network-runtime-provider
  (:require [cn.li.mcmod.network.binary-codec :as codec]
            [cn.li.mcmod.network.server :as server]
            [cn.li.mcmod.content.registry :as content-registry]))

(defn runtime-provider [_]
  {:encode #'codec/encode :decode #'codec/decode
   :list-descriptors #'content-registry/list-descriptors
   :handle-request #'server/handle-request})
