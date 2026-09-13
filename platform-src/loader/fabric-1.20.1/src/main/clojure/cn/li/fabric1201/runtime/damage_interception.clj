(ns cn.li.fabric1201.runtime.damage-interception
  "Fabric implementation of IDamageInterception protocol.

  Fabric registers attack cancellation here. Its mixin registration delegates
  mutable damage rewriting to the common Minecraft runtime."
  (:require [cn.li.mc1201.runtime.damage-interception-core :as core]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.fabric.api.entity.event.v1 ServerLivingEntityEvents$AllowDamage]))

(defn install-damage-interception! []
  (install/process-once! ::installed
    #(do
       ;; Install shared protocol implementation
       (core/install-damage-interception!)

       ;; Register ALLOW_DAMAGE listener (pre-check cancel path).
       (.register net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents/ALLOW_DAMAGE
                  (reify ServerLivingEntityEvents$AllowDamage
                    (allowDamage [_ entity damage-source amount]
                      (core/allow-attack? entity damage-source amount))))

       (log/debug "Fabric damage interception installed")))
  nil)
