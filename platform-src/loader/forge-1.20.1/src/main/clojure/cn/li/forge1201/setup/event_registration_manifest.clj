(ns cn.li.forge1201.setup.event-registration-manifest
  "Declarative event registration manifest for Forge lifecycle/common events."
  (:require [cn.li.forge1201.integration.events.interact :as interact-events]
            [cn.li.forge1201.integration.events.block :as block-events]
            [cn.li.forge1201.integration.events.loot :as loot-events]
            [cn.li.forge1201.integration.events.world :as world-events]
            [cn.li.forge1201.integration.events.entity-attributes :as entity-attr-events]
            [cn.li.forge1201.registry.creative-tab-event :as creative-tab-event]))

(defn lifecycle-listener-specs
  [{:keys [on-common-setup on-client-setup]}]
  [{:listener-class net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
    :handler on-common-setup}
   {:listener-class net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
    :handler on-client-setup}
   ;; 1.20+ data-driven creative tab population (ModEventBus)
   {:listener-class net.minecraftforge.event.BuildCreativeModeTabContentsEvent
    :handler creative-tab-event/handle-build-contents}
   ;; Register PathfinderMob default attributes for every :scripted-mob entity type
   {:listener-class net.minecraftforge.event.entity.EntityAttributeCreationEvent
    :handler entity-attr-events/handle-entity-attribute-creation}])

(defn common-event-listener-specs
  []
  [{:listener-class net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock
    :handler interact-events/handle-right-click-event}
   {:listener-class net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent
    :handler block-events/handle-block-place-event}
   {:listener-class net.minecraftforge.event.level.BlockEvent$BreakEvent
    :handler block-events/handle-block-break-event}
   {:listener-class net.minecraftforge.event.LootTableLoadEvent
    :handler loot-events/handle-loot-table-load}
     {:listener-class net.minecraftforge.event.level.LevelEvent$Load
    :handler world-events/handle-world-load}
     {:listener-class net.minecraftforge.event.level.LevelEvent$Save
    :handler world-events/handle-world-save}
     {:listener-class net.minecraftforge.event.level.LevelEvent$Unload
    :handler world-events/handle-world-unload}
     {:listener-class net.minecraftforge.event.TickEvent$LevelTickEvent
    :handler world-events/handle-world-tick}])