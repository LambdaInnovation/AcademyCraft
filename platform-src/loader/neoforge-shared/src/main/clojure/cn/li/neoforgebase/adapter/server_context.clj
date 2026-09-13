(ns cn.li.neoforgebase.adapter.server-context
	"Centralized Forge server context accessor for runtime adapters."
	(:require [cn.li.mcbase.runtime.spi.server-context :as server-context-spi]
	          [cn.li.mcmod.runtime.install :as install])
	(:import [net.minecraft.server MinecraftServer]
					 [net.neoforged.neoforge.common NeoForge]
					 [net.neoforged.neoforge.event.server ServerStartedEvent ServerStoppedEvent]
					 [net.neoforged.bus.api EventPriority]
					 [net.neoforged.neoforge.server ServerLifecycleHooks]))

(defn get-server
	^MinecraftServer
	[]
	(ServerLifecycleHooks/getCurrentServer))

(declare install-server-context!)

(defn- register-server-context-spi!
	[]
	(install/framework-once! ::spi-registered
		#(server-context-spi/register-server-context-impl!
			{:get-current-server get-server
			 :install! install-server-context!}))
	nil)

(defn install-server-context!
	[]
	(register-server-context-spi!)
	;; Forge's NeoForge/EVENT_BUS is a static JVM-process singleton —
	;; re-registering listeners on Framework reinjection would double-fire
	;; after every world reload, so this is process-scoped, not framework-scoped.
	(install/process-once! ::server-lifecycle-listeners
		#(do
			(.addListener (NeoForge/EVENT_BUS)
							EventPriority/NORMAL false ServerStartedEvent
							(reify java.util.function.Consumer
								(accept [_ evt]
									(server-context-spi/notify-server-available! (.getServer ^ServerStartedEvent evt)))))
			(.addListener (NeoForge/EVENT_BUS)
							EventPriority/NORMAL false ServerStoppedEvent
							(reify java.util.function.Consumer
								(accept [_ evt]
									(server-context-spi/notify-server-unavailable! (.getServer ^ServerStoppedEvent evt)))))))
	(when-let [server (get-server)]
		(server-context-spi/notify-server-available! server))
	nil)
