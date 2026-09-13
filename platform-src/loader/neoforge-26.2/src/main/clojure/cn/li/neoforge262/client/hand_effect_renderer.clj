(ns cn.li.neoforge262.client.hand-effect-renderer
  "CLIENT-ONLY first-person hand effect renderer for runtime animations."
  (:require [cn.li.mcbase.client.effects.hand :as hand]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.neoforge262.shim ForgeClientHelper]
           [net.minecraft.client Minecraft]
           [net.minecraft.world InteractionHand]
           [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.neoforge.client.event ClientTickEvent$Post RenderHandEvent]
           [net.neoforged.bus.api EventPriority]))

(defn- on-client-tick [^ClientTickEvent$Post evt]
  (hand/tick-hand-effects!)
  (when-let [mc (Minecraft/getInstance)]
    (when-let [player (.player mc)]
      (hand/apply-camera-pitch-deltas! player))))

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
                  (float tx) (float ty) (float tz)
                  (float rot-x) (float rot-y) (float rot-z))
            (.setCanceled evt true))))
      (catch Exception e
        (log/debug "Transformed hand render failed:" (ex-message e))))))

(defn init! []
  (install/process-once! ::tick-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false ClientTickEvent$Post
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-client-tick evt)))))
  (install/process-once! ::render-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false RenderHandEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-render-hand evt)))))
  (log/debug "Hand effect renderer initialized"))
