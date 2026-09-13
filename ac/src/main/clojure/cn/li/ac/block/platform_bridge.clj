(ns cn.li.ac.block.platform-bridge
	"AC blockstate datagen bindings for mcmod blockstate bridge."
	(:require [cn.li.mcmod.block.blockstate-definition :as mcmod-blockstate]
						[cn.li.ac.block.blockstate-definition :as ac-blockstate]
						[cn.li.mcmod.runtime.install :as install]
						[cn.li.mcmod.util.log :as log]))

(defn install-blockstate-hooks!
	[]
	(install/framework-once! ::hooks-installed?
  (fn []
    (mcmod-blockstate/register-blockstate-hooks!
			{:get-all-definitions ac-blockstate/get-all-definitions
			 :get-block-state-definition ac-blockstate/get-block-state-definition
			 :is-multipart-block? ac-blockstate/is-multipart-block?
			 :get-model-cube-texture-config ac-blockstate/get-model-cube-texture-config
			 :get-model-texture-config ac-blockstate/get-model-texture-config
			 :get-item-model-id ac-blockstate/get-item-model-id})
		(log/debug "AC blockstate hooks installed")))
	nil)