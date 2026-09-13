(ns cn.li.neoforge1211.integration.tutorial-events
  "NeoForge event listeners for tutorial condition-based unlock.

  Listens to ItemCraftedEvent, ItemEntityPickupEvent.Post, ItemSmeltedEvent
  and dispatches matching condition checks through mcmod platform hooks.

  Uses the same NeoForge/EVENT_BUS pattern as item_handler.clj."
  (:require [cn.li.platform.neutral.integration-runtime :as tutorial-hooks]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.neoforged.neoforge.event.entity.player PlayerEvent$ItemCraftedEvent
                                                 PlayerEvent$ItemSmeltedEvent
                                                 ItemEntityPickupEvent$Post]
           [net.neoforged.neoforge.event.tick PlayerTickEvent$Post]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
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
  [^ItemEntityPickupEvent$Post event]
  (when-let [player (.getPlayer event)]
    (when (instance? Player player)
      (let [item-id (item-stack->id (.getOriginalStack event))]
        (dispatch-item-event! ^Player player item-id :item-pickup)))))

;; ============================================================================
;; Tick-based activation batching
;; ============================================================================

(def ^:private tick-counter
  "Server-process-global tick batching counter (not per-player/session state)."
  (atom 0))

(defn- on-player-tick
  "Called on PlayerTickEvent.Post (server players).
  Every 3 ticks, calls process-pending-activations! for the player
  to batch-check newly-satisfied tutorial conditions."
  [^PlayerTickEvent$Post event]
  (when (instance? ServerPlayer (.getEntity event))
    (let [c (swap! tick-counter inc)]
      (when (zero? (mod c 3))
        (let [^ServerPlayer player (.getEntity event)]
          (try
            (tutorial-hooks/process-pending-activations! player)
            (catch Throwable e
              (log/stacktrace "on-player-tick: process-pending-activations! failed" e))))))))

;; ============================================================================
;; Registration
;; ============================================================================

(defn init!
  "Register NeoForge item event listeners for tutorial condition tracking
  and tick-based activation batching.
  Called from client/server setup. Process-scoped guard: EVENT_BUS
  listener registration must not redo on Framework reinjection."
  []
  (install/process-once! ::registered
    #(do
       (.addListener (NeoForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     PlayerEvent$ItemCraftedEvent
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-item-crafted evt))))
       (.addListener (NeoForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     PlayerEvent$ItemSmeltedEvent
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-item-smelted evt))))
       (.addListener (NeoForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     ItemEntityPickupEvent$Post
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-item-pickup evt))))
       (.addListener (NeoForge/EVENT_BUS)
                     EventPriority/NORMAL false
                     PlayerTickEvent$Post
                     (reify java.util.function.Consumer
                       (accept [_ evt] (on-player-tick evt))))
       (log/debug "Tutorial item event listeners registered")))
  nil)
