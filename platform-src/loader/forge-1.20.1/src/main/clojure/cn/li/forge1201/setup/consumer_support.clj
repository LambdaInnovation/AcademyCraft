(ns cn.li.forge1201.setup.consumer-support
  "Shared Java Consumer helpers for Forge event/listener registration."
  (:import [net.minecraftforge.eventbus.api EventPriority IEventBus]))

(def ^:private event-priority (delay EventPriority/NORMAL))

(defn consumer
  [f]
  (reify java.util.function.Consumer
    (accept [_ event]
      (f event))))

(defn add-normal-listener!
  [^IEventBus event-bus ^Class listener-class f]
  (.addListener event-bus @event-priority false listener-class (consumer f)))
