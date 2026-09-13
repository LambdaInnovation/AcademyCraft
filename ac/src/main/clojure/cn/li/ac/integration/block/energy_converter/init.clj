(ns cn.li.ac.integration.block.energy-converter.init
	"Energy converter integration bootstrap.

	Provides content-loader entry points used by integration content init."
	(:require [cn.li.mcmod.block.dsl :as bdsl]
	          [cn.li.mcmod.block.tile-dsl :as tdsl]
	          [cn.li.ac.config.modid :as modid]
		          [cn.li.mcmod.platform.be :as platform-be]
		          [cn.li.ac.integration.block.energy-converter.base :as ec-base]
	          [cn.li.ac.integration.block.energy-converter.config :as ec-config]
		          [cn.li.ac.integration.block.energy-converter.schema :as ec-schema]
	          [cn.li.ac.integration.block.energy-converter.eu-input :as eu-input]
	          [cn.li.ac.integration.block.energy-converter.eu-output :as eu-output]
	          [cn.li.ac.integration.block.energy-converter.rf-input :as rf-input]
	          [cn.li.ac.integration.block.energy-converter.rf-output :as rf-output]
		          [cn.li.ac.block.energy-converter.wireless-impl :as ec-wireless]
	          [cn.li.ac.gui.open :as gui-open]
	          [cn.li.mcmod.runtime.install :as install]
						[cn.li.ac.integration.block.energy-converter.platform-bridge :as ec-bridge]
						[cn.li.mcmod.util.log :as log])
		(:import [cn.li.acapi.wireless IWirelessGenerator IWirelessReceiver]))

(def ^:private converter-definitions
	[{:block-id rf-input/block-id
	  :registry-name rf-input/registry-name
	  :display-name rf-input/display-name
	  :texture rf-input/texture}
	 {:block-id rf-output/block-id
	  :registry-name rf-output/registry-name
	  :display-name rf-output/display-name
	  :texture rf-output/texture}
	 {:block-id eu-input/block-id
	  :registry-name eu-input/registry-name
	  :display-name eu-input/display-name
	  :texture eu-input/texture}
	 {:block-id eu-output/block-id
	  :registry-name eu-output/registry-name
	  :display-name eu-output/display-name
	  :texture eu-output/texture}])

(defn- open-converter-gui!
	[player world pos _block-id & {:keys [sneaking]}]
	(when-not sneaking
		(try
			(gui-open/open-gui-by-type player :energy-converter world pos)
			(catch Exception e
				(log/stacktrace "Failed to open Energy Converter GUI" e)
				nil))))

(defn- register-converter-block!
	[{:keys [block-id registry-name texture]}]
	(tdsl/register-tile!
		(tdsl/create-tile-spec
			block-id
			{:registry-name registry-name
			 :impl :scripted
			 :blocks [block-id]
			 :read-nbt-fn ec-base/read-nbt-fn
			 :write-nbt-fn ec-base/write-nbt-fn}))
	(bdsl/register-block!
		(bdsl/create-block-spec
			block-id
			{:registry-name registry-name
				 :physical {:material :stone
									:hardness 2.5
									:resistance 12.5
									:requires-tool true
									:harvest-tool :pickaxe
									:harvest-level 0
									:sounds :stone}
			 :rendering {:model-parent "minecraft:block/cube_all"
									:textures {:all (modid/asset-path "block" texture)}
									:flat-item-icon? true}
			 :events {:on-right-click open-converter-gui!}})))

(defn load-converters!
	[]
	(install/framework-once! ::converters-loaded?
  (fn []
    (ec-bridge/install-energy-integration-hooks!)
		(log/debug "Energy converters loaded"
							{:count (count ec-config/supported-blocks)}))))

(defn init-converters!
	[]
	(install/framework-once! ::converters-initialized?
  (fn []
    (doseq [converter converter-definitions]
			(register-converter-block! converter))
		;; --- register wireless capability bindings ---
		;; :wireless-generator and :wireless-receiver capabilities are declared
		;; by standard machines (solar_gen, imag_fusor, etc.) with generic
		;; WirelessGeneratorImpl / WirelessReceiverImpl factories that read
		;; :energy, :gen-speed and :max-energy from custom state.
		;; Energy converters use the same factories — :gen-speed is populated
		;; at state init from ec-config/transfer-bandwidth.
		;; Only bind tile-ids; do NOT re-declare the capability (global
		;; singleton — second declare-capability! with a different factory
		;; throws Duplicate capability registration).
		;; receivers: rf-input, eu-input   |  generators: rf-output, eu-output
		(doseq [tile-id ["rf-input" "eu-input"]]
			(tdsl/register-tile-capability-keys! tile-id :wireless-receiver))
		(doseq [tile-id ["rf-output" "eu-output"]]
			(tdsl/register-tile-capability-keys! tile-id :wireless-generator))
		(log/debug "Energy converters initialized"
							{:count (count ec-config/supported-blocks)}))))
