(ns cn.li.forge1201.setup.mod-bus
	"Forge mod-event-bus wiring extracted from mod entry.

	Owns deferred-register registration and mod lifecycle/event listeners so
	mod.clj stays focused on bootstrap flow."
	(:require [cn.li.forge1201.setup.capability-setup :as capability-setup]
				[cn.li.forge1201.setup.event-registration :as event-registration]
				[cn.li.forge1201.setup.registry-binding :as registry-binding])
	(:import [net.minecraftforge.eventbus.api IEventBus]
				 [net.minecraftforge.fml.javafmlmod FMLJavaModLoadingContext]
				 [cn.li.forge1201.gametest ForgeGameTestRegistration]
				 [cn.li.forge1201.bootstrap ForgeBootstrapGuard]))

(defn- resolve-mod-bus
	[]
	(.getModEventBus (FMLJavaModLoadingContext/get)))

(defn register-config-phase!
	[^IEventBus mod-bus _opts]
	(registry-binding/register-config-phase! mod-bus nil)
	nil)

(defn register-registry-phase!
	[^IEventBus mod-bus opts]
	(registry-binding/register-registry-phase! mod-bus opts)
	nil)

(defn register-lifecycle-phase!
	[^IEventBus mod-bus {:keys [on-common-setup on-client-setup]}]
	(event-registration/register-lifecycle-phase! mod-bus {:on-common-setup on-common-setup
																 :on-client-setup on-client-setup})
	nil)

(defn register-gametest-phase!
	[^IEventBus mod-bus _opts]
	(ForgeGameTestRegistration/register mod-bus)
	nil)

(defn register-capability-phase!
	[^IEventBus mod-bus _opts]
	(capability-setup/register-capability-phase! mod-bus nil)
	nil)

(defn registration-phase-plan
	[_opts]
	[[:config register-config-phase!]
	 [:registry register-registry-phase!]
	 [:lifecycle register-lifecycle-phase!]
	 [:gametest register-gametest-phase!]
	 [:capability register-capability-phase!]])

(defn run-registration-phases!
	[opts]
	(if-not (ForgeBootstrapGuard/markModBusRegisteredIfAbsent)
		nil
		(let [^IEventBus mod-bus (resolve-mod-bus)]
			(doseq [[_phase phase-fn] (registration-phase-plan opts)]
				(phase-fn mod-bus opts))
			nil)))

(defn register-mod-bus!
	[opts]
	(run-registration-phases! opts))