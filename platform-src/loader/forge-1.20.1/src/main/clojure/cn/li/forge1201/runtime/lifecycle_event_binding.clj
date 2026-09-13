(ns cn.li.forge1201.runtime.lifecycle-event-binding
	"Forge EventBus binding for runtime player lifecycle listeners."
	(:require [cn.li.mcmod.runtime.install :as install])
	(:import [net.minecraftforge.common MinecraftForge]
					 [net.minecraftforge.eventbus.api EventPriority]
					 [net.minecraftforge.event.entity.player PlayerEvent$PlayerLoggedInEvent
																									PlayerEvent$PlayerLoggedOutEvent
																	PlayerEvent$Clone
																	PlayerEvent$PlayerChangedDimensionEvent]
					 [net.minecraftforge.event.entity.living LivingDeathEvent]
					 [net.minecraftforge.event TickEvent$ServerTickEvent]))

(defn reset-lifecycle-listeners-registration-for-test!
	[]
	(install/reset-process-flag-for-test! ::lifecycle-listeners-registered)
	nil)

(defn add-listener!
	[event-class handler]
	(.addListener (MinecraftForge/EVENT_BUS)
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
	 [TickEvent$ServerTickEvent on-server-tick]])

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
