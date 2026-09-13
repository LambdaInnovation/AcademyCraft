(ns cn.li.mcmod.runtime.client-render-provider
  (:require [cn.li.mcmod.client.input-buttons :as input]
            [cn.li.mcmod.client.render.script-render-abi :as abi]
            [cn.li.mcmod.client.render.script-render-registry :as registry]))

(defn runtime-provider [_]
  {:initial-button-state #'input/initial-button-state
   :handle-button-state! #'input/handle-button-state!
   :validate-profile! #'abi/validate-profile!
   :kind-renderer-key #'abi/resolve-kind-renderer-key
   :register-scripted-effect-kind! #'abi/register-scripted-effect-kind!
   :resolve-kind-renderer-key #'abi/resolve-kind-renderer-key
   :supported-kinds #'abi/supported-kinds
   :get-profile #'registry/get-profile
   :snapshot #'registry/snapshot})
