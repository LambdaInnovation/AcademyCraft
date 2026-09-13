(ns cn.li.mcbase.runtime.item-callback
  "Build the single native Item.use callback shared by all loaders."
  (:require [cn.li.mcbase.runtime.item-handler-core]
            [cn.li.mcbase.runtime.event.item-use :as item-use])
  (:import [cn.li.mcver ItemUseResults McAccess]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.world.level Level]
           [net.minecraft.world InteractionHand]))

(defn build
  "Create an IFn that dispatches the shared item-use flow.

  `with-owner` is the loader boundary for optional player-owner binding; it
  receives player, side, and a zero-argument function.

  Return type is version-local via ItemUseResults (InteractionResultHolder on
  1.20.1/1.21.1, InteractionResult on 26.2)."
  [label with-owner]
  (reify clojure.lang.IFn
    (invoke [_this level player hand]
      (let [^Level level level
            ^Player player player
            ^InteractionHand hand hand
            side (if (McAccess/isClientSide level) :client :server)
            ^ItemStack stack (.getItemInHand player hand)
            {:keys [consume?]} (with-owner player side
                                 #(item-use/handle-use player hand stack side {} label))]
        (if consume?
          (ItemUseResults/success stack)
          (ItemUseResults/pass stack))))))
