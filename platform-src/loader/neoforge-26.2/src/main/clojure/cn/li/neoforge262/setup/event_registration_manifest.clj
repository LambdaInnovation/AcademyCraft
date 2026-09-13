(ns cn.li.neoforge262.setup.event-registration-manifest
  "Declarative event registration manifest for Forge lifecycle/common events."
  (:require [cn.li.neoforge262.integration.events.interact :as interact-events]
            [cn.li.neoforge262.integration.events.block :as block-events]
            [cn.li.neoforgebase.integration.events.loot :as loot-events]
            [cn.li.neoforge262.integration.events.world :as world-events]
            [cn.li.neoforgebase.integration.events.entity-attributes :as entity-attr-events]
            [cn.li.neoforge262.registry.creative-tab-event :as creative-tab-event]))

(defn lifecycle-listener-specs
  [{:keys [on-common-setup on-client-setup]}]
  [{:listener-class net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
    :handler on-common-setup}
   {:listener-class net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
    :handler on-client-setup}
   ;; 1.20+ data-driven creative tab population (ModEventBus)
   {:listener-class net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
    :handler creative-tab-event/handle-build-contents}
   ;; Register PathfinderMob default attributes for every :scripted-mob entity type
   {:listener-class net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
    :handler entity-attr-events/handle-entity-attribute-creation}])

(defn common-event-listener-specs
  []
  [{:listener-class net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
    :handler interact-events/handle-right-click-event}
   {:listener-class net.neoforged.neoforge.event.level.BlockEvent$EntityPlaceEvent
    :handler block-events/handle-block-place-event}
   ;; 26.2: BreakEvent relocated to event.level.block.BreakBlockEvent
   {:listener-class net.neoforged.neoforge.event.level.block.BreakBlockEvent
    :handler block-events/handle-block-break-event}
   {:listener-class net.neoforged.neoforge.event.LootTableLoadEvent
    :handler loot-events/handle-loot-table-load}
     {:listener-class net.neoforged.neoforge.event.level.LevelEvent$Load
    :handler world-events/handle-world-load}
     {:listener-class net.neoforged.neoforge.event.level.LevelEvent$Save
    :handler world-events/handle-world-save}
     {:listener-class net.neoforged.neoforge.event.level.LevelEvent$Unload
    :handler world-events/handle-world-unload}
     {:listener-class net.neoforged.neoforge.event.tick.LevelTickEvent$Post
    :handler world-events/handle-world-tick}])