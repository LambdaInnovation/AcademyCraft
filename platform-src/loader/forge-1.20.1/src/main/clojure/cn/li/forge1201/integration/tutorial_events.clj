(ns cn.li.forge1201.integration.tutorial-events
  "Forge event listeners for tutorial condition-based unlock.

  Listens to ItemCraftedEvent, EntityItemPickupEvent, ItemSmeltedEvent
  and dispatches matching condition checks through mcmod platform hooks.

  Uses the same MinecraftForge/EVENT_BUS pattern as item_handler.clj."
  (:require [cn.li.platform.neutral.integration-runtime :as tutorial-hooks]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraftforge.event.entity.player PlayerEvent$ItemCraftedEvent
                                                 PlayerEvent$ItemSmeltedEvent]
           [net.minecraftforge.event.entity.player EntityItemPickupEvent]
           [net.minecraftforge.event TickEvent$PlayerTickEvent TickEvent$Phase]
           [net.minecraftforge.common MinecraftForge]
           [net.minecraftforge.eventbus.api EventPriority]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.resources ResourceLocation]))

;; ============================================================================
;; Item ID resolution
;; ============================================================================

(defn- item-stack->id
  "Get the runtime item id (\"namespace:path\") from an ItemStack."
  [^ItemStack stack]
  (when stack
    (try
      (let [item (.getItem stack)
            ^ResourceLocation key (.getKey BuiltInRegistries/ITEM item)]
        (when key
          (str (.getNamespace key) ":" (.getPath key))))
      (catch Exception e
        (log/stacktrace "item-stack->id: failed to resolve item id" e)
        nil))))

;; ============================================================================
;; Event dispatch helpers
;; ============================================================================

(defn- dispatch-item-event!
  [^Player player item-id event-type]
  (when (and player item-id)
    (try
      (tutorial-hooks/on-item-event! player item-id event-type)
      (catch Throwable e
        (log/stacktrace "dispatch-item-event!: failed to dispatch item event" e)))))

(defn- on-item-crafted
  [^PlayerEvent$ItemCraftedEvent event]
  (when-let [player (.getEntity event)]
    (when (instance? Player player)
      (let [item-id (item-stack->id (.getCrafting event))]
        (dispatch-item-event! ^Player player item-id :item-crafted)))))

(defn- on-item-smelted
  [^PlayerEvent$ItemSmeltedEvent event]
  (when-let [player (.getEntity event)]
    (when (instance? Player player)
      (let [item-id (item-stack->id (.getSmelting event))]
        (dispatch-item-event! ^Player player item-id :item-smelted)))))

(defn- on-item-pickup
  [^EntityItemPickupEvent event]
  (when-let [player (.getEntity event)]
    (when (instance? Player player)
      (let [item-entity (.getItem event)
            item-id (when item-entity
                      (item-stack->id (.getItem item-entity)))]
        (dispatch-item-event! ^Player player item-id :item-pickup)))))

;; ============================================================================
;; Tick-based activation batching
;; ============================================================================

(def ^:private tick-counter
  "Server-process-global tick batching counter (not per-player/session state)."
  (atom 0))

(defn- on-player-tick
  "Called on PlayerTickEvent (SERVER, END phase).
  Every 3 ticks, calls process-pending-activations! for the player
  to batch-check newly-satisfied tutorial conditions."
  [^TickEvent$PlayerTickEvent event]
  (when (and (= TickEvent$Phase/END (.phase event))
             (instance? ServerPlayer (.player event)))
    (let [c (swap! tick-counter inc)]
      (when (zero? (mod c 3))
        (let [^ServerPlayer player (.player event)]
          (try
            (tutorial-hooks/process-pending-activations! player)
            (catch Throwable e
              (log/stacktrace "on-player-tick: process-pending-activations! failed" e))))))))

;; ============================================================================
;; Registration
;; ============================================================================

(defn init!
  "Register Forge item event listeners for tutorial condition tracking
  and tick-based activation batching.
  Called from Forge client/server setup. Process-scoped guard: EVENT_BUS
  listener registration must not redo on Framework reinjection."
  []
  (install/process-once! ::registered
    #(do
       (.addListener (MinecraftForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     PlayerEvent$ItemCraftedEvent
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-item-crafted evt))))
       (.addListener (MinecraftForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     PlayerEvent$ItemSmeltedEvent
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-item-smelted evt))))
       (.addListener (MinecraftForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     EntityItemPickupEvent
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-item-pickup evt))))
       (.addListener (MinecraftForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     TickEvent$PlayerTickEvent
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-player-tick evt))))
       (log/debug "Tutorial item event listeners registered")))
  nil)
