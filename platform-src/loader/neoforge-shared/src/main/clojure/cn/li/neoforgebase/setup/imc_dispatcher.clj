(ns cn.li.neoforgebase.setup.imc-dispatcher
  "Forge mod-bus listener for processing incoming IMC registrations."
  (:require [cn.li.neoforgebase.integration.imc-dispatch :as imc-dispatch]
            [cn.li.neoforgebase.setup.consumer-support :as consumer-support]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util.function Consumer Supplier]
           [net.neoforged.bus.api IEventBus]
           [net.neoforged.fml.event.lifecycle InterModProcessEvent]
           [net.neoforged.fml InterModComms$IMCMessage]))

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
  (consumer-support/add-normal-listener! mod-bus InterModProcessEvent handle-imc-process-event!)
  nil)
