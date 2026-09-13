(ns cn.li.mcbase.block.logic-compile
  "Compile declarative tile hook maps into loader-neutral Java TileLogicBundle instances."
  (:require [cn.li.mcmod.capability.registry :as cap-registry])
  (:import            [cn.li.mcbase.block.entity IScriptedBlockEntity]
           [cn.li.mcbase.block.logic
            ITileTickLogic ITileNbtLogic ITileContainerLogic
            ITileCapabilityLogic TileLogicBundle]
           [cn.li.mcbase.shim FnTileTickLogic FnTileNbtLogic]
           [net.minecraft.core Direction]
           [net.minecraft.nbt CompoundTag]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.world.level Level]
           [net.minecraft.core BlockPos]
           [net.minecraft.world.level.block.state BlockState]))

(def ^:private EMPTY-INTS (int-array 0))

(defn- container-key [c primary alt]
  (or (get c primary) (get c alt)))

(defn- ^ITileContainerLogic compile-container [c]
  (when c
    (let [get-size              (or (container-key c :get-size nil)
                                    (constantly 0))
          get-item              (or (container-key c :get-item nil)
                                    (fn [_ _] ItemStack/EMPTY))
          set-item!             (or (container-key c :set-item! nil)
                                    (fn [_ _ _] nil))
          remove-item           (or (container-key c :remove-item nil)
                                    (fn [_ _ _] ItemStack/EMPTY))
          remove-item-no-update (or (container-key c :remove-item-no-update nil)
                                    (fn [_ _] ItemStack/EMPTY))
          clear!                (or (container-key c :clear! nil)
                                    (fn [_] nil))
          still-valid           (or (container-key c :still-valid :still-valid?)
                                    (constantly true))
          slots-for-face        (or (container-key c :slots-for-face nil)
                                    (fn [_ _] EMPTY-INTS))
          can-place             (or (container-key c :can-place :can-place-through-face?)
                                    (constantly false))
          can-take              (or (container-key c :can-take :can-take-through-face?)
                                    (constantly true))]
      (reify ITileContainerLogic
        (getSize [_ be] (int (get-size be)))
        ;; nil→ItemStack/EMPTY guards: Clojure fns conventionally return nil
        ;; for empty slots, but ITileContainerLogic requires @Nonnull ItemStack.
        (getItem [_ be slot] (or (get-item be slot) ItemStack/EMPTY))
        (setItem [_ be slot stack]
          (set-item! be slot stack)
          nil)
        (removeItem [_ be slot amt] (or (remove-item be slot amt) ItemStack/EMPTY))
        (removeItemNoUpdate [_ be slot] (or (remove-item-no-update be slot) ItemStack/EMPTY))
        (clearContent [_ be]
          (clear! be)
          nil)
        (stillValid [_ be player] (boolean (still-valid be player)))
        (getSlotsForFace [_ be side] (slots-for-face be side))
        (canPlaceItemThroughFace [_ be s st sd] (boolean (can-place be s st sd)))
        (canTakeItemThroughFace [_ be s st sd] (boolean (can-take be s st sd)))))))

(defn- ^ITileCapabilityLogic compile-capability [capability-keys]
  (when (seq capability-keys)
    (let [keys (set capability-keys)]
      (reify ITileCapabilityLogic
        (resolve [_ ^IScriptedBlockEntity be ^String cap-key side]
          (let [k (keyword cap-key)]
            (when (contains? keys k)
              (when-let [factory (cap-registry/get-handler-factory k)]
                (factory be side)))))))))

(defn ^TileLogicBundle compile-tile-logic
  "Compile merged tile cfg (post tile-kind merge) into a single TileLogicBundle."
  [cfg]
  (let [tick-fn      (:tick-fn cfg)
        read-nbt-fn  (:read-nbt-fn cfg)
        write-nbt-fn (:write-nbt-fn cfg)
        tick (when tick-fn (FnTileTickLogic. tick-fn))
        nbt (when (or read-nbt-fn write-nbt-fn)
              (FnTileNbtLogic.
                (when read-nbt-fn
                  (fn [^IScriptedBlockEntity be ^CompoundTag tag]
                    (when-let [data (read-nbt-fn tag)]
                      (.setCustomState be data))))
                write-nbt-fn))
        container (compile-container (:container cfg))
        capability (compile-capability (:capability-keys cfg))]
    (TileLogicBundle. tick nbt container capability)))
