(ns cn.li.forge1201.integration.events.event-apply
  "Centralized Forge event mutation helpers to keep write-back semantics consistent."
  (:import [net.minecraft.world InteractionResult]
           [net.minecraftforge.event.entity.player PlayerInteractEvent PlayerInteractEvent$RightClickBlock]
           [net.minecraftforge.eventbus.api Event Event$Result]))

(defn cancel-player-interact-consume!
  [^PlayerInteractEvent evt]
  (.setCancellationResult evt InteractionResult/CONSUME)
  (.setCanceled evt true)
  evt)

(defn cancel-event!
  [^Event evt]
  (.setCanceled evt true)
  evt)

(defn apply-consumed-right-click!
  "Cancel the RightClickBlock event with InteractionResult/CONSUME on both sides.
  This prevents the vanilla Item.use() fallback that would otherwise fire even
  after useItem=DENY (Item.useOn is skipped, but Item.use runs as a last resort).
  The block's GUI was already opened by the handler — cancelling the event does
  not affect the GUI."
  [^PlayerInteractEvent evt client-side?]
  (when (instance? PlayerInteractEvent$RightClickBlock evt)
    (let [^PlayerInteractEvent$RightClickBlock right-click-evt evt]
      (.setUseItem right-click-evt Event$Result/DENY)))
  (cancel-player-interact-consume! evt)
  evt)
