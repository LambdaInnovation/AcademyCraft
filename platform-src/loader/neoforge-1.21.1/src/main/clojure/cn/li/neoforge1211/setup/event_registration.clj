(ns cn.li.neoforge1211.setup.event-registration
	"Unified Forge event registration binder.

	Consumes declarative manifest entries and binds them to ModEventBus / Forge EVENT_BUS."
	(:require [cn.li.neoforgebase.setup.consumer-support :as consumer-support]
						[cn.li.neoforge1211.setup.event-registration-manifest :as manifest])
	(:import [net.neoforged.neoforge.common NeoForge]
					 [net.neoforged.bus.api IEventBus]))

(defn- bind-listener-spec!
	[event-bus {:keys [listener-class handler]}]
	(consumer-support/add-normal-listener! event-bus listener-class handler)
	nil)

(defn register-lifecycle-phase!
	[^IEventBus mod-bus opts]
	(doseq [spec (manifest/lifecycle-listener-specs opts)]
		(bind-listener-spec! mod-bus spec))
	nil)

(defn register-common-event-listeners!
	[]
	(doseq [spec (manifest/common-event-listener-specs)]
		(bind-listener-spec! (NeoForge/EVENT_BUS) spec))
	nil)