(ns cn.li.neoforgebase.runtime.item-handler
  "NeoForge event handler for item finish-using lifecycle events.

  Version loaders install with-player-owner before init!."
  (:require [cn.li.mcbase.runtime.item-handler-core]
            [cn.li.mcbase.runtime.event.item-use :as item-use]
            [cn.li.mcmod.util.log :as log])
  (:import [net.neoforged.neoforge.event.entity.living LivingEntityUseItemEvent$Finish]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
           [net.minecraft.world.entity.player Player]))

(defonce ^:private with-player-owner-atom (atom nil))

(defn install-with-player-owner!
  "Install (fn [player side f] ...) from versioned runtime.owner."
  [f]
  (reset! with-player-owner-atom f)
  f)

(defn- with-player-owner
  [player side f]
  (let [bound @with-player-owner-atom]
    (when (nil? bound)
      (throw (IllegalStateException. "with-player-owner not installed")))
    (bound player side f)))

(defn- on-item-finish-using
  [^LivingEntityUseItemEvent$Finish event]
  (let [entity (.getEntity event)
        stack (.getItem event)
        side (if (and (instance? Player entity)
                      (.isClientSide (.level ^Player entity)))
               :client
               :server)]
    (if (instance? Player entity)
      (with-player-owner ^Player entity side
        #(item-use/handle-finish-using! entity stack side "Forge"))
      (item-use/handle-finish-using! entity stack side "Forge"))))

(defn init!
  "Initialize Forge item lifecycle handlers."
  []
  (.addListener (NeoForge/EVENT_BUS)
                EventPriority/NORMAL
                false
                LivingEntityUseItemEvent$Finish
                (reify java.util.function.Consumer
                  (accept [_ evt] (on-item-finish-using evt))))
  (log/debug "Forge item lifecycle handler initialized"))
