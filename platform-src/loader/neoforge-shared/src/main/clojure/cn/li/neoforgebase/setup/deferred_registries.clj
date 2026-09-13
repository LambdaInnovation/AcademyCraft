(ns cn.li.neoforgebase.setup.deferred-registries
  "Helpers for registering Forge DeferredRegister instances."
  (:import [net.neoforged.bus.api IEventBus]
           [net.neoforged.neoforge.registries DeferredRegister]))

(defn register-deferred-registries!
  [^IEventBus mod-bus registries]
  (doseq [^DeferredRegister registry registries]
    (.register registry mod-bus))
  nil)
