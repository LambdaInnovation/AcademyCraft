(ns cn.li.forge1201.setup.imc-dispatcher
	"Forge mod-bus listener for processing incoming IMC registrations."
	(:require [cn.li.forge1201.integration.imc-dispatch :as imc-dispatch]
						[cn.li.mcmod.util.log :as log])
	(:import [java.util.function Consumer Supplier]
					 [net.minecraftforge.eventbus.api EventPriority IEventBus]
					 [net.minecraftforge.fml.event.lifecycle InterModProcessEvent]
					 [net.minecraftforge.fml InterModComms$IMCMessage]))

(def ^:private event-priority (delay EventPriority/NORMAL))

(defn- imc-method-key
	[^InterModComms$IMCMessage msg]
	(str (.method msg)))

(defn- imc-supplier
	[^InterModComms$IMCMessage msg]
	(.messageSupplier msg))

(defn- resolve-payload
	[msg]
	(let [supplier (imc-supplier msg)]
		(cond
			(instance? Supplier supplier) (.get ^Supplier supplier)
			(fn? supplier) (supplier)
			:else supplier)))

(defn- handle-imc-message!
	[msg]
	(imc-dispatch/register-by-method-key! (imc-method-key msg) (resolve-payload msg)))

(defn- handle-imc-process-event!
	[^InterModProcessEvent evt]
	(try
		(let [stream (.getIMCStream evt)]
			(.forEach stream
								(reify Consumer
									(accept [_ msg]
										(handle-imc-message! msg)))))
		(catch Throwable t
			(log/stacktrace "handle-imc-process-event! caught exception" t))))

(defn register-imc-listener!
	[^IEventBus mod-bus]
	(.addListener mod-bus
								@event-priority
								false
								InterModProcessEvent
								(reify Consumer
									(accept [_ event]
										(handle-imc-process-event! event))))
	nil)
