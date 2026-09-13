(ns cn.li.mcbase.platform.runtime-ops
  "Minecraft-touching runtime callables shared by Forge/Fabric adapter maps.

  Consumed by cn.li.platform.adapter.minecraft-ops/build-adapter-map.
  Version modules install RuntimeAccess/BlockRegistry/ItemRegistry/ParticleEntity
  wrappers via install-runtime-ops!."
  (:import [cn.li.mcbase.runtime ItemPlayerOps]
           [net.minecraft.core BlockPos Direction]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.entity.item ItemEntity]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block.state BlockState]
           [net.minecraft.world.phys BlockHitResult]))

(defonce ^:private ops-atom (atom nil))

(defn install-runtime-ops!
  "Install map of versioned Java helper wrappers."
  [m]
  (reset! ops-atom m)
  m)

(defn- ops []
  (let [m @ops-atom]
    (when (nil? m)
      (throw (IllegalStateException. "runtime-ops not installed")))
    m))

(defn- may-modify-block?
  "Whether `player` is allowed to change the block at `pos`.

  The 1.20+ equivalent of what legacy item code spelled as
  `canPlayerEdit` + `World.canMineBlockBody`: mayUseItemAt covers adventure
  mode and the mayBuild flag, isUnderSpawnProtection covers the server's
  spawn radius. The spawn check only exists server-side, so a client-side or
  serverless level answers on mayUseItemAt alone."
  [^Player player ^Level level ^BlockPos pos ^Direction face]
  (and (some? player)
       (.mayUseItemAt player pos face (.getMainHandItem player))
       (if (instance? ServerLevel level)
         (let [^MinecraftServer server (.getServer level)]
           (or (nil? server)
               (not (.isUnderSpawnProtection server ^ServerLevel level pos player))))
         true)))

(defn- raytrace-block
  [player reach fluid-source-only?]
  (when-let [^BlockHitResult hit ((:playerRaytraceBlock (ops))
                                  player
                                  (double (or reach 5.0))
                                  (boolean fluid-source-only?))]
    (let [^BlockPos hit-pos (.getBlockPos hit)
          ^Direction face (.getDirection hit)
          ^BlockPos place-pos (.relative hit-pos face)
          ^Level level ((:getEntityLevel (ops)) player)
          ^BlockState hit-state (.getBlockState level hit-pos)
          ^BlockState place-state (.getBlockState level place-pos)
          ^Player p player]
      {:hit-pos {:x (.getX hit-pos) :y (.getY hit-pos) :z (.getZ hit-pos)}
       :place-pos {:x (.getX place-pos) :y (.getY place-pos) :z (.getZ place-pos)}
       :block-id ((:getBlockKey (ops)) (.getBlock hit-state))
       :direction (.getName face)
       ;; canBeReplaced is the modern spelling of Block#isReplaceable: true for
       ;; air, snow layers, tall grass, fluids, fire — false for stone, chests.
       :hit-replaceable? (.canBeReplaced hit-state)
       :place-replaceable? (.canBeReplaced place-state)
       :may-edit-hit? (may-modify-block? p level hit-pos face)
       :may-edit-place? (may-modify-block? p level place-pos face)})))

(defn- drop-player-main-hand-item-at!
  [player amount x y z]
  (let [n (int (max 0 (or amount 0)))]
    (cond
      (nil? player) false
      (zero? n) true
      (boolean (.isCreative ^Player player)) true
      :else
      (let [^ItemStack stack (.getMainHandItem ^Player player)]
        (if (or (nil? stack)
                (.isEmpty stack)
                (< (int (.getCount stack)) n))
          false
          (let [^ItemStack drop-stack (.copy stack)
                level ^Level ((:getEntityLevel (ops)) player)]
            (.setCount drop-stack n)
            (.shrink stack n)
            (if (or (nil? level) (.isClientSide level))
              true
              (boolean (.addFreshEntity level
                                        (ItemEntity. level (double x) (double y) (double z) drop-stack))))))))))

(defn standard-runtime-ops
  "Return the shared runtime callable map (without loader-specific slots)."
  []
  {:entity-class #((:getEntityClass (ops)))
   :player-class #((:getPlayerClass (ops)))
   :server-player-class #((:getServerPlayerClass (ops)))
   :inventory-class #((:getInventoryClass (ops)))
   :menu-class #((:getAbstractContainerMenuClass (ops)))
   :item-stack-class #((:getItemStackClass (ops)))
   :item-class #((:getItemClass (ops)))
   :block-state-class #((:getBlockStateClass (ops)))
   :level-class #((:getLevelClass (ops)))
   :item-registry-name (fn [item] ((:getItemKeyString (ops)) item))
   :block-registry-name (fn [block] ((:getBlockKey (ops)) block))
   :item-stack-of (fn [nbt] ((:itemStackOf (ops)) nbt))
   :create-item-stack-by-id (fn [item-id count]
                              ((:createItemStackById (ops)) (str item-id) (int count)))
   :item-stack-empty? (fn [stack] ((:isItemStackEmpty (ops)) stack))
   :player-level (fn [player] ((:getEntityLevel (ops)) player))
   :player-container-menu (fn [player] ((:getPlayerContainerMenu (ops)) player))
   :count-player-item-by-id (fn [player item-id]
                              (ItemPlayerOps/countPlayerItemById player (str item-id)))
   :consume-player-item-by-id! (fn [player item-id amount]
                                 (ItemPlayerOps/consumePlayerItemById
                                  player (str item-id) (int (or amount 0))))
   :drop-player-main-hand-item-at! drop-player-main-hand-item-at!
   :give-player-item-stack! (fn [player stack]
                              (ItemPlayerOps/givePlayerItemStack player stack))
   :spawn-entity-by-id! (fn [player entity-id speed]
                          ((:spawnEntityByIdFromPlayer (ops))
                           player (str entity-id) (float (or speed 1.0))))
   :spawn-tracked-entity-by-id! (fn [player entity-id speed life]
                                  ((:spawnTrackedEntityByIdFromPlayer (ops))
                                   player
                                   (str entity-id)
                                   (float (or speed 0.0))
                                   (when life (int life))))
   :raytrace-block raytrace-block
   :inventory-owner (fn [inventory] ((:getInventoryPlayer (ops)) inventory))
   :menu-container-id (fn [menu] ((:getMenuContainerId (ops)) menu))})
