(ns cn.li.mcbase.runtime.block-manipulation-core
  "Loader-agnostic block manipulation helpers.

  Uses only vanilla MC APIs + mcver seams. Platform adapters supply:
  - break-guard-fn: (fn [level pos player]) -> boolean"
  (:require [cn.li.mcbase.runtime.adapter.block-manipulation :as block-adapter]
            [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.core BlockPos]
           [net.minecraft.core.registries BuiltInRegistries]
           [cn.li.mcver ItemStackEnchants RegistryValues ResourceLocations]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerPlayer ServerLevel]
           [net.minecraft.tags BlockTags]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block Block Blocks]
           [net.minecraft.world.level.block.entity BlockEntity]
           [net.minecraft.world.entity Entity]))

(defn- fortune-tool-stack
  ^ItemStack [^ServerLevel level fortune-level]
  (ItemStackEnchants/fortuneNetheritePickaxe level (int fortune-level)))

(defn get-level-by-id
  ^ServerLevel [^MinecraftServer server world-id]
  (try
    (query-core/resolve-level-strict server world-id)
    (catch Exception e
      (log/warn "Failed to get level:" world-id (ex-message e))
      nil)))

(defn- block-key-str [^Block block]
  (str (.getKey BuiltInRegistries/BLOCK block)))

(defn break-block!
  "Break a block at [x y z] in world-id.
  break-guard-fn: (fn [^ServerLevel level ^BlockPos pos ^ServerPlayer player]) -> boolean"
  ([^MinecraftServer server player-uuid world-id x y z drop? break-guard-fn]
   (break-block! server player-uuid world-id x y z drop? 0 break-guard-fn))
  ([^MinecraftServer server player-uuid world-id x y z drop? fortune-level break-guard-fn]
   (try
     (when-let [^ServerLevel level (get-level-by-id server world-id)]
       (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
         (let [pos (BlockPos. (int x) (int y) (int z))]
           (when (break-guard-fn level pos player)
             (let [state (.getBlockState level pos)
                   ^BlockEntity be (.getBlockEntity level pos)]
               (when drop?
                 (if (pos? (int fortune-level))
                   (Block/dropResources state ^Level level pos be ^Entity player
                                        (fortune-tool-stack level fortune-level))
                   (Block/dropResources state ^Level level pos)))
               (.levelEvent level 2001 pos (Block/getId state)))
             (.removeBlock level pos false)
             true))))
     (catch Exception e
       (log/warn "Failed to break block:" (ex-message e))
       false))))

(defn can-break-block?
  [^MinecraftServer server player-uuid world-id x y z break-guard-fn]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (when-let [^ServerPlayer player (query-core/get-player-by-uuid server player-uuid)]
        (let [pos (BlockPos. (int x) (int y) (int z))]
          (boolean (break-guard-fn level pos player)))))
    (catch Exception e
      (log/warn "Failed to check can-break-block:" (ex-message e))
      false)))

(defn set-block!
  [^MinecraftServer server world-id x y z block-id]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            ^Block block (RegistryValues/getBlock (ResourceLocations/parse (str block-id)))]
        (when block
          (.setBlock level pos (.defaultBlockState block) 3)
          true)))
    (catch Exception e
      (log/warn "Failed to set block:" (ex-message e))
      false)))

(defn get-block
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            state (.getBlockState level pos)
            block (.getBlock state)]
        (when-not (.isAir state)
          (block-key-str block))))
    (catch Exception e
      (log/warn "Failed to get block:" (ex-message e))
      nil)))

(defn get-block-hardness
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            state (.getBlockState level pos)]
        (.getDestroySpeed state level pos)))
    (catch Exception e
      (log/warn "Failed to get block hardness:" (ex-message e))
      nil)))

(defn block-collidable?
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            state (.getBlockState level pos)]
        (not (.isEmpty (.getCollisionShape state level pos)))))
    (catch Exception e
      (log/warn "Failed to check block collision:" (ex-message e))
      false)))

(defn find-blocks-in-line
  [^MinecraftServer server world-id x1 y1 z1 dx dy dz max-distance]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [step-size 0.5
            steps (int (/ max-distance step-size))
            results (transient [])]
        (doseq [i (range steps)]
          (let [t (* i step-size)
                x (+ x1 (* dx t))
                y (+ y1 (* dy t))
                z (+ z1 (* dz t))
                pos (BlockPos. (int x) (int y) (int z))
                state (.getBlockState level pos)
                block (.getBlock state)]
            (when-not (.isAir state)
              (conj! results
                     {:x (int x)
                      :y (int y)
                      :z (int z)
                      :block-id (block-key-str block)
                      :hardness (.getDestroySpeed state level pos)}))))
        (persistent! results)))
    (catch Exception e
      (log/warn "Failed to find blocks in line:" (ex-message e))
      [])))

(defn liquid-block?
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            fluid-state (.getFluidState (.getBlockState level pos))]
        (and fluid-state (not (.isEmpty fluid-state)))))
    (catch Exception e
      (log/warn "Failed to check liquid block:" (ex-message e))
      false)))

(defn requires-high-tier-tool?
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            state (.getBlockState level pos)]
        (boolean (.is state BlockTags/NEEDS_DIAMOND_TOOL))))
    (catch Exception e
      (log/warn "Failed to check block tool tier:" (ex-message e))
      false)))

(defn farmland-block?
  [^MinecraftServer server world-id x y z]
  (try
    (when-let [^ServerLevel level (get-level-by-id server world-id)]
      (let [pos (BlockPos. (int x) (int y) (int z))
            state (.getBlockState level pos)]
        (= (.getBlock state) Blocks/FARMLAND)))
    (catch Exception e
      (log/warn "Failed to check farmland block:" (ex-message e))
      false)))

(block-adapter/install-block-core!
  {:break-block! break-block!
   :set-block! set-block!
   :get-block get-block
   :get-block-hardness get-block-hardness
   :block-collidable? block-collidable?
   :can-break-block? can-break-block?
   :requires-high-tier-tool? requires-high-tier-tool?
   :find-blocks-in-line find-blocks-in-line
   :liquid-block? liquid-block?
   :farmland-block? farmland-block?})
