(ns cn.li.fabric1211.integration.events
  "Fabric 1.21.1 event handlers"
  (:require [cn.li.fabric1211.integration.events.interact :as interact-events]
            [cn.li.fabric1211.integration.events.block :as block-events]
            [cn.li.fabric1211.integration.events.loot :as loot-events]
            [cn.li.fabric1211.integration.events.lifecycle :as lifecycle-events]
            [cn.li.fabric1211.integration.events.world :as world-events]
            [cn.li.fabric1211.registry.creative-tab-event :as creative-tabs]
            [cn.li.fabric1211.commands :as commands]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.event-runtime :as world-lifecycle]
            [cn.li.mcmod.runtime.install :as install])
  (:import [net.fabricmc.fabric.api.command.v2 CommandRegistrationCallback]
           [net.fabricmc.fabric.api.loot.v2 LootTableEvents$Modify]
           [net.fabricmc.fabric.api.entity.event.v1 ServerPlayerEvents$CopyFrom
            ServerLivingEntityEvents$AfterDeath
            ServerEntityWorldChangeEvents
            ServerEntityWorldChangeEvents$AfterPlayerChange]
           [net.fabricmc.fabric.api.networking.v1 ServerPlayConnectionEvents$Join
            ServerPlayConnectionEvents$Disconnect]
           [net.fabricmc.fabric.api.event.lifecycle.v1 ServerWorldEvents$Load
            ServerWorldEvents$Unload
            ServerTickEvents$EndTick
            ServerTickEvents$EndWorldTick]
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

      (.register net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents/LOAD
                 (reify ServerWorldEvents$Load
                   (onWorldLoad [_ _server world]
                     (world-events/handle-world-load world))))

      (.register net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents/UNLOAD
                 (reify ServerWorldEvents$Unload
                   (onWorldUnload [_ _server world]
                     (world-events/handle-world-unload world))))

      (.register net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents/END_WORLD_TICK
                 (reify ServerTickEvents$EndWorldTick
                   (onEndTick [_ world]
                     (world-lifecycle/dispatch-world-tick world))))

      (.register net.fabricmc.fabric.api.loot.v2.LootTableEvents/MODIFY
                 (reify LootTableEvents$Modify
                   (modifyLootTable [_ id table-builder _source]
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

      (.register ServerEntityWorldChangeEvents/AFTER_PLAYER_CHANGE_WORLD
                 (reify ServerEntityWorldChangeEvents$AfterPlayerChange
                   (afterChangeWorld [_ player origin destination]
                     (lifecycle-events/handle-player-dimension-change player origin destination))))

      (.register net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents/END_SERVER_TICK
                 (reify ServerTickEvents$EndTick
                   (onEndTick [_ server]
                     (lifecycle-events/handle-player-tick server))))

      (.register CommandRegistrationCallback/EVENT
                 (reify CommandRegistrationCallback
                   (register [_ dispatcher _registry-access _environment]
                     (commands/register-commands dispatcher))))

          (lifecycle-events/install-server-stop-cleanup!)
          (world-events/register-on-world-state-changed!)
          (creative-tabs/register!)

          (log/debug "Fabric event listeners registered")))
  nil)
