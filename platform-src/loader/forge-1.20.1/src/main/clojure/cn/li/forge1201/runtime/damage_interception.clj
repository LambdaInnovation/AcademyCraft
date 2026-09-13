(ns cn.li.forge1201.runtime.damage-interception
  "Forge implementation of IDamageInterception protocol.

  Intercepts LivingHurtEvent to allow runtime effects to modify incoming damage."
  (:require [cn.li.mc1201.runtime.damage-interception-core :as core]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraftforge.common MinecraftForge]
           [net.minecraftforge.eventbus.api EventPriority]
           [net.minecraftforge.event.entity.living LivingAttackEvent]
           [net.minecraftforge.event.entity.living LivingHurtEvent]))

(defn install-damage-interception! []
  ;; Install shared protocol implementation
  (core/install-damage-interception!)

  ;; Register event listener
  (.addListener (MinecraftForge/EVENT_BUS)
                EventPriority/LOWEST ; Let earlier compatibility handlers decide first.
                false
                LivingAttackEvent
                (reify java.util.function.Consumer
                  (accept [_ event]
                    (let [^LivingAttackEvent evt event]
                      (core/apply-attack-result!
                       (.getEntity evt)
                       (.getSource evt)
                       (.getAmount evt)
                       #(.setCanceled evt (boolean %)))))))

  (.addListener (MinecraftForge/EVENT_BUS)
                EventPriority/LOWEST ; Apply after other mutable-damage handlers.
                false
                LivingHurtEvent
                (reify java.util.function.Consumer
                  (accept [_ event]
                    (let [^LivingHurtEvent evt event]
                      (core/apply-damage-result!
                       (.getEntity evt)
                       (.getSource evt)
                       (.getAmount evt)
                       #(.setAmount evt (float %)))))))

  (log/debug "Forge damage interception installed"))
