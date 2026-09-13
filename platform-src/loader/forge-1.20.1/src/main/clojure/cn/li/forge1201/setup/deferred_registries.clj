(ns cn.li.forge1201.setup.deferred-registries
  "Helpers for registering Forge DeferredRegister instances."
  (:import [net.minecraftforge.eventbus.api IEventBus]
           [net.minecraftforge.registries DeferredRegister]))

(defn register-deferred-registries!
  [^IEventBus mod-bus registries]
  (doseq [^DeferredRegister registry registries]
    (.register registry mod-bus))
  nil)
