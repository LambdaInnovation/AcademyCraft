(ns cn.li.forge1201.setup.capability-setup
  "Forge capability registration extracted from mod-bus orchestration."
  (:require [cn.li.forge1201.setup.capability-wiring :as capability-wiring]
            [cn.li.forge1201.setup.imc-dispatcher :as imc-dispatcher])
  (:import [net.minecraftforge.eventbus.api IEventBus]))

(defn register-capability-phase!
  [^IEventBus mod-bus _opts]
  (imc-dispatcher/register-imc-listener! mod-bus)
  (capability-wiring/register-capability-listener! mod-bus)
  nil)
