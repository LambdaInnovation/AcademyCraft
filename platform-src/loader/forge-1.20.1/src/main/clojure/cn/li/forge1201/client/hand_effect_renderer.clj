(ns cn.li.forge1201.client.hand-effect-renderer
  "CLIENT-ONLY first-person hand effect renderer for runtime animations."
  (:require [cn.li.mcbase.client.effects.hand :as hand]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.forge1201.shim ForgeClientHelper]
           [net.minecraft.client Minecraft]
           [net.minecraft.world InteractionHand]
           [net.minecraftforge.client.event RenderHandEvent]
           [net.minecraftforge.common MinecraftForge]
           [net.minecraftforge.event TickEvent$ClientTickEvent TickEvent$Phase]
           [net.minecraftforge.eventbus.api EventPriority]))

(defn- on-client-tick [^TickEvent$ClientTickEvent evt]
  (when (= TickEvent$Phase/END (.phase evt))
    (hand/tick-hand-effects!)
    (when-let [mc (Minecraft/getInstance)]
      (when-let [player (.player mc)]
        (hand/apply-camera-pitch-deltas! player)))))

;; renderTransformedMainHand re-enters renderHandsWithItems, which fires
;; RenderHandEvent again from inside its own handler. Without a guard that
;; would recurse until StackOverflowError. The nested events must behave
;; differently: the MAIN hand render must pass through (the pose stack
;; already carries the transform, so the nested vanilla render draws it
;; transformed), while the OFF-hand event must be canceled — the nested
;; call would otherwise draw it transformed AND the outer vanilla flow
;; draws it untransformed, leaving a double image.
(def ^:private ^:dynamic *in-transformed-hand-render* false)

(defn- on-render-hand [^RenderHandEvent evt]
  (if *in-transformed-hand-render*
    (when (not= InteractionHand/MAIN_HAND (.getHand evt))
      (.setCanceled evt true))
    (try
      (when-let [{:keys [tx ty tz rot-x rot-y rot-z]} (hand/current-hand-transform)]
        (binding [*in-transformed-hand-render* true]
          (when (ForgeClientHelper/renderTransformedMainHand
                  evt
                  (float tx)
                  (float ty)
                  (float tz)
                  (float rot-x)
                  (float rot-y)
                  (float rot-z))
            (.setCanceled evt true))))
      (catch Exception e
        (log/stacktrace "Transformed hand render failed" e)))))

(defn init! []
  (install/process-once! ::tick-listener-registered
    #(.addListener (MinecraftForge/EVENT_BUS)
                   EventPriority/NORMAL false TickEvent$ClientTickEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-client-tick evt)))))
  (install/process-once! ::render-listener-registered
    #(.addListener (MinecraftForge/EVENT_BUS)
                   EventPriority/NORMAL false RenderHandEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-render-hand evt)))))
  (log/debug "Hand effect renderer initialized"))