(ns cn.li.neoforgebase.runtime.lifecycle-event-binding
	"NeoForge EventBus binding for runtime player lifecycle listeners."
	(:require [cn.li.mcmod.runtime.install :as install])
	(:import [net.neoforged.neoforge.common NeoForge]
					 [net.neoforged.bus.api EventPriority]
					 [net.neoforged.neoforge.event.entity.player PlayerEvent$PlayerLoggedInEvent
																									PlayerEvent$PlayerLoggedOutEvent
																	PlayerEvent$Clone
																	PlayerEvent$PlayerChangedDimensionEvent]
					 [net.neoforged.neoforge.event.entity.living LivingDeathEvent]
					 [net.neoforged.neoforge.event.tick ServerTickEvent$Post]))

(defn reset-lifecycle-listeners-registration-for-test!
	[]
	(install/reset-process-flag-for-test! ::lifecycle-listeners-registered)
	nil)

(defn add-listener!
	[event-class handler]
	(.addListener (NeoForge/EVENT_BUS)
								EventPriority/NORMAL false event-class
								(reify java.util.function.Consumer
									(accept [_ evt] (handler evt)))))

(defn- listener-bindings
	[{:keys [on-player-login
					 on-player-logout
					 on-player-clone
					 on-player-death
					 on-player-dimension-change
					 on-server-tick]}]
	[[PlayerEvent$PlayerLoggedInEvent on-player-login]
	 [PlayerEvent$PlayerLoggedOutEvent on-player-logout]
	 [PlayerEvent$Clone on-player-clone]
	 [LivingDeathEvent on-player-death]
	 [PlayerEvent$PlayerChangedDimensionEvent on-player-dimension-change]
	 [ServerTickEvent$Post on-server-tick]])

(defn register-lifecycle-listeners!
	[{:keys [on-player-login
					 on-player-logout
					 on-player-clone
					 on-player-death
					 on-player-dimension-change
					 on-server-tick]}]
	(install/process-once! ::lifecycle-listeners-registered
		#(doseq [[event-class handler]
					(listener-bindings {:on-player-login on-player-login
											 :on-player-logout on-player-logout
											 :on-player-clone on-player-clone
											 :on-player-death on-player-death
												 :on-player-dimension-change on-player-dimension-change
												 :on-server-tick on-server-tick})]
			(add-listener! event-class handler)))
	nil)
