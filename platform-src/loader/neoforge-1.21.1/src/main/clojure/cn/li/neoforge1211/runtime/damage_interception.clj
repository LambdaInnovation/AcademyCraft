(ns cn.li.neoforge1211.runtime.damage-interception
  "NeoForge implementation of IDamageInterception protocol.

  Intercepts LivingIncomingDamageEvent (cancel) and LivingDamageEvent.Pre
  (mutable amount) so runtime effects can rewrite incoming damage."
  (:require [cn.li.mc1211.runtime.damage-interception-core :as core]
            [cn.li.mcmod.util.log :as log])
  (:import [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
           [net.neoforged.neoforge.event.entity.living LivingIncomingDamageEvent]
           [net.neoforged.neoforge.event.entity.living LivingDamageEvent$Pre]))

(defn install-damage-interception! []
  ;; Install shared protocol implementation
  (core/install-damage-interception!)

  ;; Register event listener — LivingAttackEvent → LivingIncomingDamageEvent
  (.addListener (NeoForge/EVENT_BUS)
                EventPriority/LOWEST ; Let earlier compatibility handlers decide first.
                false
                LivingIncomingDamageEvent
                (reify java.util.function.Consumer
                  (accept [_ event]
                    (let [^LivingIncomingDamageEvent evt event]
                      (core/apply-attack-result!
                       (.getEntity evt)
                       (.getSource evt)
                       (.getAmount evt)
                       #(.setCanceled evt (boolean %)))))))

  ;; LivingHurtEvent → LivingDamageEvent.Pre (getNewDamage/setNewDamage)
  (.addListener (NeoForge/EVENT_BUS)
                EventPriority/LOWEST ; Apply after other mutable-damage handlers.
                false
                LivingDamageEvent$Pre
                (reify java.util.function.Consumer
                  (accept [_ event]
                    (let [^LivingDamageEvent$Pre evt event]
                      (core/apply-damage-result!
                       (.getEntity evt)
                       (.getSource evt)
                       (.getNewDamage evt)
                       #(.setNewDamage evt (float %)))))))

  (log/debug "NeoForge damage interception installed"))
