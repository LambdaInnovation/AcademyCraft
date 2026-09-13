(ns cn.li.ac.block.cat-engine.capability
	(:require [cn.li.ac.block.machine.runtime :as machine-runtime]
						[cn.li.mcmod.platform.be :as platform-be]
						[cn.li.mcmod.platform.position :as pos]
						[cn.li.ac.block.cat-engine.config :as cat-config]
						[cn.li.ac.block.cat-engine.logic :as cat-logic])
	(:import [cn.li.acapi.wireless IWirelessGenerator]))

(deftype CatEngineGeneratorImpl [be]
	IWirelessGenerator

	(getEnergy [_]
		(let [state (or (platform-be/get-custom-state be) cat-logic/cat-default-state)]
			(double (max 0.0 (double (get state :energy 0.0))))))

	(setEnergy [_ energy]
		(let [state (or (platform-be/get-custom-state be) cat-logic/cat-default-state)
					max-energy (double (get state :max-energy (cat-config/max-energy)))
					clamped (-> (double energy) (max 0.0) (min max-energy))]
			(machine-runtime/commit-transform! be cat-logic/cat-default-state
			                                 #(assoc % :energy clamped))))

	(getProvidedEnergy [_ req]
		(let [state (or (platform-be/get-custom-state be) cat-logic/cat-default-state)
					energy (double (get state :energy 0.0))
					requested (max 0.0 (double req))
					actual (min requested energy)]
			(when (or (pos? actual)
								(not= (double (get state :this-tick-gen 0.0)) 0.0))
				(machine-runtime/commit-transform! be cat-logic/cat-default-state
				                                 #(assoc %
				                                         :energy (- energy actual)
				                                         :this-tick-gen (double actual)
				                                         :gen-speed (double actual))
				                                 ;; render.clj reads :this-tick-gen directly for rotor speed.
				                                 :sync-client? true))
			(double actual)))

	(getGeneratorBandwidth [_]
		(double (cat-config/generator-bandwidth)))

	Object
	(toString [_]
		(str "CatEngineGeneratorImpl@" (pos/block-pos be))))