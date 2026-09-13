(ns cn.li.neoforgebase.setup.capability-setup
  "Shared capability + network registration phase."
  (:require [cn.li.neoforgebase.setup.capability-wiring :as capability-wiring]
            [cn.li.neoforgebase.setup.imc-dispatcher :as imc-dispatcher])
  (:import [net.neoforged.bus.api IEventBus]))

(defonce ^:private register-network-atom
  (atom nil))

(defn install-register-network!
  "Install (fn [mod-bus] ...)."
  [f]
  (reset! register-network-atom f)
  f)

(defn register-capability-phase!
  [^IEventBus mod-bus _opts]
  (let [register-network! @register-network-atom]
    (when (nil? register-network!)
      (throw (IllegalStateException. "capability-setup register-network not installed")))
    (imc-dispatcher/register-imc-listener! mod-bus)
    (capability-wiring/register-capability-listener! mod-bus)
    (register-network! mod-bus)
    nil))
