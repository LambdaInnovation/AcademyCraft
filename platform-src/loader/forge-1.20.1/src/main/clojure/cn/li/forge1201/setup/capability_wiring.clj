(ns cn.li.forge1201.setup.capability-wiring
  "Forge capability registration listener wiring."
  (:require [cn.li.forge1201.setup.consumer-support :as consumer-support]
            [cn.li.mcmod.capability.registry :as cap-registry])
  (:import [cn.li.forge1201.capability ForgeProvidedCapabilitySupport]
           [net.minecraftforge.common.capabilities RegisterCapabilitiesEvent]
           [net.minecraftforge.eventbus.api IEventBus]))

(defn- add-listener!
  [^IEventBus mod-bus ^Class listener-class f]
  (consumer-support/add-normal-listener! mod-bus listener-class f))

(defn- forge-provided-capability?
  "Forge-provided capabilities (ForgeCapabilities/*) are already registered by Forge.
  Mods attach handlers and map tokens via CapabilityRegistry; do not re-register here."
  [^Class java-type]
  (ForgeProvidedCapabilitySupport/isForgeProvidedCapabilityType java-type))

(defn register-capability-listener!
  [^IEventBus mod-bus]
  (add-listener! mod-bus RegisterCapabilitiesEvent
                 (fn [event]
                   (let [^RegisterCapabilitiesEvent event event]
                     (doseq [^Class java-type (distinct (keep (fn [[_key {:keys [java-type]}]]
                                                                java-type)
                                                              (cap-registry/capability-type-registry-snapshot)))]
                       (when-not (forge-provided-capability? java-type)
                         (.register event java-type))))))
  nil)
