(ns cn.li.neoforgebase.setup.registry-binding
  "Shared NeoForge registry/config phase. Version loaders install Mod* bridges."
  (:require [cn.li.neoforgebase.config.bridge :as config-bridge]
            [cn.li.neoforgebase.integration.side :as side]
            [cn.li.neoforgebase.setup.deferred-registries :as deferred-registries])
  (:import [net.neoforged.bus.api IEventBus]))

(defonce ^:private registry-bridge-atom (atom nil))

(defn install-registry-bridge!
  "Install {:register-entities! :register-recipes! :register-triggers!
            :register-features! :register-client-hooks!}."
  [bridge]
  (reset! registry-bridge-atom bridge)
  bridge)

(defn- bridge! []
  (let [b @registry-bridge-atom]
    (when (nil? b)
      (throw (IllegalStateException. "registry-binding bridge not installed")))
    b))

(defn register-config-phase!
  ([^IEventBus mod-bus _opts]
   (register-config-phase! mod-bus nil _opts))
  ([^IEventBus mod-bus mod-container _opts]
   (config-bridge/register-all! mod-bus mod-container)
   (config-bridge/install-config-persist-op!)
   nil))

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
  (let [b (bridge!)]
    ((:register-entities! b) mod-bus)
    ((:register-recipes! b) mod-bus)
    ((:register-triggers! b) mod-bus)
    (when (and (side/client-side?) (not datagen-run?))
      ((:register-client-hooks! b)))
    ((:register-features! b) mod-bus)
    (deferred-registries/register-deferred-registries! mod-bus [sounds-register
                                                              effects-register
                                                              particle-types-register
                                                              fluid-types-register
                                                              fluids-register
                                                              blocks-register
                                                              items-register
                                                              block-entities-register
                                                              creative-tabs-register
                                                              gui-menu-register]))
  nil)
