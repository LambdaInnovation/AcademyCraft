(ns cn.li.ac.block.imag-phase.handlers
	(:require [cn.li.ac.block.phase-gen.config :as phase-config]
						[cn.li.mcmod.platform.item :as pitem]
						[cn.li.mcmod.platform.structured-data :as sd]
						[cn.li.mcmod.platform.world :as world]))

(defn- stack-empty? [stack]
	(or (nil? stack)
			(try
				(boolean (pitem/empty? stack))
				(catch Exception _ false))))

(defn- stack-id [stack]
	(when-not (stack-empty? stack)
		(try
			(some-> stack pitem/object pitem/registry-name str)
			(catch Exception _ nil))))

(defn- to-phase-liquid-matter-unit! [stack]
	(try
		(let [data (pitem/ensure-custom-data stack)]
			(sd/set-string! data "matterKind" "phase-liquid")
			(pitem/set-custom-data! stack data)
			(pitem/set-damage! stack phase-config/matter-unit-phase-liquid-meta))
		true
		(catch Exception _ false)))

(defn handle-imag-phase-click
	[{:keys [world item-stack] :as _ctx}]
	(when (and world
						 (not (world/client-side? world))
						 (not (stack-empty? item-stack))
						 (= (stack-id item-stack) phase-config/matter-unit-item-id))
		(when (to-phase-liquid-matter-unit! item-stack)
			{:consume? true})))