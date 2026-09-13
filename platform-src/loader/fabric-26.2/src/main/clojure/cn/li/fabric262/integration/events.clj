(ns cn.li.fabric262.integration.events
  "Fabric 26.2 event handlers"
  (:require [cn.li.fabric262.integration.events.interact :as interact-events]
            [cn.li.fabric262.integration.events.block :as block-events]
            [cn.li.fabric262.integration.events.loot :as loot-events]
            [cn.li.fabric262.integration.events.lifecycle :as lifecycle-events]
            [cn.li.fabric262.integration.events.world :as world-events]
            [cn.li.fabric262.commands :as commands]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.runtime.install :as install])
  (:import [net.fabricmc.fabric.api.command.v2 CommandRegistrationCallback]
           [net.fabricmc.fabric.api.loot.v3 LootTableEvents$Modify]
           [net.fabricmc.fabric.api.entity.event.v1 ServerPlayerEvents$CopyFrom
            ServerLivingEntityEvents$AfterDeath]
           [net.fabricmc.fabric.api.networking.v1 ServerPlayConnectionEvents$Join
            ServerPlayConnectionEvents$Disconnect]
           [net.fabricmc.fabric.api.event.player UseBlockCallback
            PlayerBlockBreakEvents$Before]))

(defn handle-block-place-mixin
  "Handle Fabric block placement from BlockItem mixin.
   Returns true when placement should be canceled."
  [player world pos block]
  (block-events/handle-block-place-mixin player world pos block))

(defn register-events
  "Register Fabric event listeners."
  []
  (install/process-once! ::events-registered
    #(do
          (log/debug "Registering Fabric event listeners...")
      (lifecycle-events/install-runtime-callbacks!)

      (.register UseBlockCallback/EVENT
                 (reify UseBlockCallback
                   (interact [_ player world hand hit-result]
                     (interact-events/handle-use-block player world hand hit-result))))

      (.register net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents/BEFORE
                 (reify PlayerBlockBreakEvents$Before
                   (beforeBlockBreak [_ world player pos state block-entity]
                     (block-events/handle-block-break world player pos state block-entity))))

      (.register net.fabricmc.fabric.api.loot.v3.LootTableEvents/MODIFY
                 (reify LootTableEvents$Modify
                   (modifyLootTable [_ id table-builder _source _lookup]
                     (loot-events/handle-loot-table-modify id table-builder))))

      (.register net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents/JOIN
                 (reify ServerPlayConnectionEvents$Join
                   (onPlayReady [_ handler _sender _server]
                     (lifecycle-events/handle-player-login (.-player handler)))))

      (.register net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents/DISCONNECT
                 (reify ServerPlayConnectionEvents$Disconnect
                   (onPlayDisconnect [_ handler _server]
                     (lifecycle-events/handle-player-logout (.-player handler)))))

      (.register net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents/COPY_FROM
                 (reify ServerPlayerEvents$CopyFrom
                   (copyFromPlayer [_ old-player new-player alive]
                     (lifecycle-events/handle-player-clone old-player new-player alive))))

      (.register net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents/AFTER_DEATH
                 (reify ServerLivingEntityEvents$AfterDeath
                   (afterDeath [_ entity _damage-source]
                     (lifecycle-events/handle-player-death entity))))

      (.register CommandRegistrationCallback/EVENT
                 (reify CommandRegistrationCallback
                   (register [_ dispatcher _registry-access _environment]
                     (commands/register-commands dispatcher))))

          (lifecycle-events/install-server-stop-cleanup!)
          (world-events/register-on-world-state-changed!)

          (log/debug "Fabric event listeners registered")))
  nil)
