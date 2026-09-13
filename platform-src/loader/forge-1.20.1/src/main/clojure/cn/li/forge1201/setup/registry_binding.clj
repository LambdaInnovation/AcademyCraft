(ns cn.li.forge1201.setup.registry-binding
  "Forge registry/config registration binding extracted from mod-bus orchestration."
  (:require [cn.li.forge1201.config.bridge :as config-bridge]
            [cn.li.forge1201.integration.side :as side]
            [cn.li.forge1201.setup.deferred-registries :as deferred-registries]
            [cn.li.forge1201.setup.lifecycle-listeners :as lifecycle-listeners])
  (:import [cn.li.forge1201.entity ModEntities]
           [cn.li.forge1201.recipe ModRecipeTypes]
           [cn.li.forge1201.worldgen ModFeatures]
           [net.minecraftforge.eventbus.api IEventBus]))

(defn register-config-phase!
  [^IEventBus mod-bus _opts]
  (config-bridge/register-all! mod-bus)
  (config-bridge/install-config-persist-op!)
  nil)

(defn register-registry-phase!
  [^IEventBus mod-bus {:keys [datagen-run?
                             sounds-register
                             effects-register
                             particle-types-register
                             fluid-types-register
                             fluids-register
                             blocks-register
                             items-register
                             block-entities-register
                             creative-tabs-register
                             gui-menu-register]}]
  (ModEntities/register mod-bus)
  (ModRecipeTypes/register mod-bus)
  (when (and (side/client-side?) (not datagen-run?))
    (lifecycle-listeners/register-client-hooks!))
  (ModFeatures/register mod-bus)
  (deferred-registries/register-deferred-registries! mod-bus [sounds-register
                                                              effects-register
                                                              particle-types-register
                                                              fluid-types-register
                                                              fluids-register
                                                              blocks-register
                                                              items-register
                                                              block-entities-register
                                                              creative-tabs-register
                                                              gui-menu-register])
  nil)
