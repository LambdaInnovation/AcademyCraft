(ns cn.li.neoforge1211.setup.mod-bus
	"NeoForge mod-event-bus wiring extracted from mod entry.

	Owns deferred-register registration and mod lifecycle/event listeners so
	mod.clj stays focused on bootstrap flow. The mod bus is injected from
	AcademyCraft1211 (no FMLJavaModLoadingContext)."
	(:require [cn.li.neoforgebase.setup.capability-setup :as capability-setup]
				[cn.li.neoforge1211.setup.event-registration :as event-registration]
				[cn.li.neoforgebase.setup.registry-binding :as registry-binding])
	(:import [net.neoforged.bus.api IEventBus]
				 [net.neoforged.fml ModContainer]
				 [cn.li.neoforge1211.gametest ForgeGameTestRegistration]
				 [cn.li.neoforgebase.bootstrap ForgeBootstrapGuard]))

(defn register-config-phase!
	[^IEventBus mod-bus ^ModContainer mod-container _opts]
	(registry-binding/register-config-phase! mod-bus mod-container nil)
	nil)

(defn register-registry-phase!
	[^IEventBus mod-bus _mod-container opts]
	(registry-binding/register-registry-phase! mod-bus opts)
	nil)

(defn register-lifecycle-phase!
	[^IEventBus mod-bus _mod-container {:keys [on-common-setup on-client-setup]}]
	(event-registration/register-lifecycle-phase! mod-bus {:on-common-setup on-common-setup
																 :on-client-setup on-client-setup})
	nil)

(defn register-gametest-phase!
	[^IEventBus mod-bus _mod-container _opts]
	(ForgeGameTestRegistration/register mod-bus)
	nil)

(defn register-capability-phase!
	[^IEventBus mod-bus _mod-container _opts]
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
	([^IEventBus mod-bus opts]
	 (run-registration-phases! mod-bus nil opts))
	([^IEventBus mod-bus ^ModContainer mod-container opts]
	 (if-not (ForgeBootstrapGuard/markModBusRegisteredIfAbsent)
		 nil
		 (do
			 (doseq [[_phase phase-fn] (registration-phase-plan opts)]
				 (phase-fn mod-bus mod-container opts))
			 nil))))

(defn register-mod-bus!
	[mod-bus mod-container opts]
	(run-registration-phases! mod-bus mod-container opts))
