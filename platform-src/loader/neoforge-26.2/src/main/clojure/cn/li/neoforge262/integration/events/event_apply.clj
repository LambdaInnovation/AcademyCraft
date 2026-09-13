(ns cn.li.neoforge262.integration.events.event-apply
  "Centralized NeoForge event mutation helpers to keep write-back semantics consistent.

  26.2: TriState moved from neoforge.common.util to net.minecraft.util."
  (:import [net.minecraft.util TriState]
           [net.minecraft.world InteractionResult]
           [net.neoforged.neoforge.event.entity.player PlayerInteractEvent PlayerInteractEvent$RightClickBlock]
           [net.neoforged.bus.api Event]
           [cn.li.neoforgebase.bridge EventInterop]))

(defn cancel-player-interact-consume!
  [^PlayerInteractEvent evt]
  (EventInterop/cancelPlayerInteract evt InteractionResult/CONSUME)
  evt)

(defn cancel-event!
  [^Event evt]
  (EventInterop/cancelEvent evt)
  evt)

(defn apply-consumed-right-click!
  "Cancel the RightClickBlock event with InteractionResult/CONSUME on both sides.
  This prevents the vanilla Item.use() fallback that would otherwise fire even
  after useItem=FALSE (Item.useOn is skipped, but Item.use runs as a last resort).
  The block's GUI was already opened by the handler — cancelling the event does
  not affect the GUI."
  [^PlayerInteractEvent evt client-side?]
  (when (instance? PlayerInteractEvent$RightClickBlock evt)
    (let [^PlayerInteractEvent$RightClickBlock right-click-evt evt]
      (.setUseItem right-click-evt TriState/FALSE)))
  (cancel-player-interact-consume! evt)
  evt)
