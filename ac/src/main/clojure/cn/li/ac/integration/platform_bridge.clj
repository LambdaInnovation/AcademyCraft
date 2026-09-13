(ns cn.li.ac.integration.platform-bridge
	"AC bindings for optional integration hooks exposed through mcmod."
	(:require [cn.li.mcmod.integration.runtime-hooks :as integration-hooks]
						[cn.li.ac.integration.jei.plugin :as jei-plugin]
						[cn.li.ac.integration.crafttweaker.bridge :as ct-bridge]
						[cn.li.mcmod.runtime.install :as install]
						[cn.li.mcmod.util.log :as log]))

(defn install-integration-hooks!
	[]
	(install/framework-once! ::hooks-installed?
  (fn []
    (integration-hooks/register-integration-hooks!
			{:jei-get-all-categories jei-plugin/get-all-categories
			 :jei-get-recipes jei-plugin/get-recipes
			 :jei-format-recipe jei-plugin/format-recipe
			 :describe-recipe ct-bridge/describe-recipe})
		(log/debug "AC integration hooks installed")))
	nil)
