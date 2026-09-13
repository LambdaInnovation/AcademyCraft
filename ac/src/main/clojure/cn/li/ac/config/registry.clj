(ns cn.li.ac.config.registry
	(:require [cn.li.ac.config.gameplay :as gameplay-config]
						[cn.li.ac.config.worldgen :as worldgen-config]
						[cn.li.ac.ability.config :as ability-config]
						[cn.li.ac.ability.skill-config :as ability-skill-config]
						[cn.li.ac.wireless.config :as wireless-config]
						[cn.li.ac.block.ability-interferer.config :as interferer-config]
						[cn.li.ac.block.cat-engine.config :as cat-config]
						[cn.li.ac.block.developer.config :as developer-config]
						[cn.li.ac.block.phase-gen.config :as phase-config]
						[cn.li.ac.block.solar-gen.config :as solar-config]
						[cn.li.ac.block.wind-gen.config :as wind-config]
						[cn.li.ac.config.common :as config-common]
						[cn.li.ac.integration.block.energy-converter.config :as energy-converter-config]
						[cn.li.ac.tutorial.config :as tutorial-config]
						[cn.li.mcmod.config.registry :as config-reg]))

(def wireless-descriptors
	(vec wireless-config/descriptors))

(def wireless-default-values
	wireless-config/default-values)

(def wireless-devices-descriptors
	(vec (concat solar-config/descriptors
							 wind-config/descriptors
							 phase-config/descriptors
							 cat-config/descriptors
							 energy-converter-config/descriptors)))

(def wireless-devices-default-values
	(merge solar-config/default-values
				 wind-config/default-values
				 phase-config/default-values
				 cat-config/default-values
				 energy-converter-config/default-values))

(def ability-devices-descriptors
	(vec (concat developer-config/descriptors
							 interferer-config/descriptors)))

(def ability-devices-default-values
	(merge developer-config/default-values
				 interferer-config/default-values))

(defn init-configs! []
	(config-reg/register-config-descriptors!
		config-common/wireless-domain
		wireless-descriptors)
	(config-reg/ensure-default-values!
		config-common/wireless-domain
		wireless-default-values)
	(config-reg/register-config-descriptors!
		config-common/wireless-devices-domain
		wireless-devices-descriptors)
	(config-reg/ensure-default-values!
		config-common/wireless-devices-domain
		wireless-devices-default-values)
	(config-reg/register-config-descriptors!
		config-common/gameplay-domain
		gameplay-config/descriptors)
	(config-reg/ensure-default-values!
		config-common/gameplay-domain
		gameplay-config/default-values)
	(config-reg/register-config-descriptors!
		config-common/worldgen-domain
		worldgen-config/descriptors)
	(config-reg/ensure-default-values!
		config-common/worldgen-domain
		worldgen-config/default-values)
	(config-reg/register-config-descriptors!
		config-common/ability-domain
		ability-config/descriptors)
	(config-reg/ensure-default-values!
		config-common/ability-domain
		ability-config/default-values)
	(config-reg/register-config-descriptors!
		config-common/ability-devices-domain
		ability-devices-descriptors)
	(config-reg/ensure-default-values!
		config-common/ability-devices-domain
		ability-devices-default-values)
(config-reg/register-config-descriptors!
			config-common/tutorial-domain
			tutorial-config/descriptors)
		(config-reg/ensure-default-values!
			config-common/tutorial-domain
			tutorial-config/default-values)
	(doseq [category-id ability-skill-config/category-ids
				:let [domain (ability-skill-config/category-domain category-id)]]
		(config-reg/register-config-descriptors!
			domain
			(get ability-skill-config/descriptors-by-category category-id))
		(config-reg/ensure-default-values!
			domain
			(get ability-skill-config/default-values-by-category category-id)))
	nil)