(ns cn.li.forge1201.setup.event-registration
	"Unified Forge event registration binder.

	Consumes declarative manifest entries and binds them to ModEventBus / Forge EVENT_BUS."
	(:require [cn.li.forge1201.setup.consumer-support :as consumer-support]
						[cn.li.forge1201.setup.event-registration-manifest :as manifest])
	(:import [net.minecraftforge.common MinecraftForge]
					 [net.minecraftforge.eventbus.api IEventBus]))

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
		(bind-listener-spec! (MinecraftForge/EVENT_BUS) spec))
	nil)