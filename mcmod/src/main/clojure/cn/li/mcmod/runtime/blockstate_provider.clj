(ns cn.li.mcmod.runtime.blockstate-provider
  "Neutral provider for blockstate datagen callbacks used by platform AOT code."
  (:require [cn.li.mcmod.block.blockstate-definition :as blockstate-definition]))

(defn runtime-provider
  [_]
  {:get-all-blockstate-definitions #'blockstate-definition/get-all-definitions
   :get-block-state-definition #'blockstate-definition/get-block-state-definition
   :is-multipart-block? #'blockstate-definition/is-multipart-block?
   :get-model-cube-texture-config #'blockstate-definition/get-model-cube-texture-config
   :get-model-texture-config #'blockstate-definition/get-model-texture-config
   :get-item-model-id #'blockstate-definition/get-item-model-id})
