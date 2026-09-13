(ns cn.li.mc1211.bootstrap.installer-core
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as item]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.be :as be]
            [cn.li.mcbase.platform.item-ops :as item-ops]
            [cn.li.mcbase.platform.player-ops :as player-ops]
            [cn.li.mcbase.runtime.spi.network-transport :as network-transport-spi]
            [cn.li.mcbase.platform.world-block-ops :as world-block-ops]
            [cn.li.mcbase.platform.menu-inventory-ops :as menu-inventory-ops]
            [cn.li.mcmod.runtime.install :as install])
  (:import [cn.li.mc1211.runtime BlockRegistry RuntimeAccess]
           [net.minecraft.core BlockPos Direction]
           [net.minecraft.core.component DataComponents]
           [net.minecraft.nbt CompoundTag ListTag StringTag]
           [net.minecraft.network.chat Component]
           [net.minecraft.resources ResourceLocation]
           [cn.li.mcver ItemData ResourceLocations]
           [net.minecraft.core RegistryAccess]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.world.entity Entity]
           [net.minecraft.world.entity.item ItemEntity]
           [net.minecraft.world.entity.player Inventory Player]
           [net.minecraft.world.inventory AbstractContainerMenu]
           [net.minecraft.world.item Item ItemStack]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block Block Blocks]
           [net.minecraft.world.level.block.state BlockState StateDefinition]
           [net.minecraft.world.level.block.state.properties BooleanProperty EnumProperty IntegerProperty Property]
           [net.minecraft.world.level.block.entity BlockEntity]
           [net.minecraft.world.phys Vec3]))

(declare install-item-protocols!)

(defn- world-server-session-id-value
  [^Level level]
  (when-let [sid (RuntimeAccess/getWorldServerSessionId level)]
    [:server sid]))

(defn- world-ops-map
  [adapter]
  {:world-get-tile-entity (fn [^Level level p] (.getBlockEntity level p))
   :world-get-block-state (fn [^Level level p] (.getBlockState level p))
   :world-set-block (fn [^Level level p s flags] (.setBlock level p s (int flags)))
   :world-remove-block (fn [^Level level p]
                                       (let [^BlockState bs (.getBlockState level p)]
                                         ;; destroyBlock returns false for fluid
                                         ;; blocks in some paths; setBlock(air)
                                         ;; is the reliable fluid removal.
                                         (if (.isEmpty (.getFluidState bs))
                                           (.destroyBlock level p false)
                                           (let [^Block air-block Blocks/AIR]
                                             (.setBlock level p (.defaultBlockState air-block) 3)))))
   :world-break-block (fn [^Level level p drop?] (.destroyBlock level p (boolean drop?)))
   :world-place-block-by-id (fn [^Level level block-id p flags]
                              (world-block-ops/world-place-block-by-id adapter level block-id p flags))
   :world-is-chunk-loaded? (fn [^Level level cx cz] (.hasChunk level (int cx) (int cz)))
   :world-get-day-time (fn [^Level level] (.getDayTime level))
   :world-get-game-time (fn [^Level level] (.getGameTime level))
   :world-get-dimension-id (fn [^Level level] (str (.location (.dimension level))))
   :world-server-session-id world-server-session-id-value
   :world-get-players (fn [^Level level] (seq (.players level)))
   :world-is-raining (fn [^Level level] (.isRaining level))
   :world-is-client-side (fn [^Level level] (.isClientSide level))
   :world-can-see-sky (fn [^Level level p] (.canSeeSky level p))})

(defn- install-player-feedback! []
  (install/framework-once! ::player-feedback-installed
    (fn []
      (platform/install-adapter! (fw/fw-atom) :player-feedback
        {:send-player-feedback! (fn [player-uuid {:keys [message args translate?]}]
                                  (try
                                    (when-let [^ServerPlayer player (network-transport-spi/find-player-by-uuid player-uuid)]
                                      (let [argv (object-array (mapv str (or args [])))
                                            component (if translate?
                                                        (Component/translatable (str message) argv)
                                                        (Component/literal (if (seq args)
                                                                             (apply format (str message) args)
                                                                             (str message))))]
                                        (.sendSystemMessage player component)
                                        true))
                                    (catch Throwable t
                                      (log/warn "Failed to send player feedback" player-uuid (ex-message t))
                                      false)))})
      (log/debug "mc1211 player feedback installed"))))

(defn install-block-state-protocol!
  [_adapter]
  (install/framework-once! ::block-state-protocols-installed
    (fn []
      (let [current (world/current-ops)
            bs-ops {:block-state-is-air               (fn [^BlockState this] (.isAir this))
                    :block-state-is-replaceable?      (fn [^BlockState this] (.canBeReplaced this))
                    :block-state-get-block            (fn [^BlockState this] (.getBlock this))
                    :block-state-get-state-definition (fn [^BlockState this] (.getStateDefinition (.getBlock this)))
                    :block-state-get-property         (fn [_this ^StateDefinition state-def prop-name]
                                                        (.getProperty state-def (str prop-name)))
                    ;; Clojure integers arrive as Long; IntegerProperty.setValue
                    ;; rejects Long (its possible-values set holds Integer), so
                    ;; cast by property kind before setting.
                    :block-state-set-property         (fn [^BlockState this ^Property prop value]
                                                        (.setValue this prop
                                                                   (cond
                                                                     (instance? IntegerProperty prop) (int value)
                                                                     (instance? BooleanProperty prop) (boolean value)
                                                                     (instance? EnumProperty prop)
                                                                     (let [v (.getValue prop (name value))]
                                                                       ;; 1.20.1 Property.getValue returns Optional<T>, not T — unwrap
                                                                       ;; before setValue (its Comparable cast rejects the Optional).
                                                                       (if (instance? java.util.Optional v)
                                                                         (.orElse ^java.util.Optional v nil)
                                                                         v))
                                                                     :else value)))}]
        (world/install-block-state-ops! bs-ops "mc1211 block-state"))
      (log/debug "mc1211 block-state ops initialized"))))

(defn- install-structured-data! []
  (install/framework-once! ::structured-data-installed
    (fn []
      (sd/install-structured-data-ops!
        {:sd-set-int!      (fn [^CompoundTag this key value] (.putInt this (str key) (int value)) this)
         :sd-get-int       (fn [^CompoundTag this key] (.getInt this (str key)))
         :sd-set-string!   (fn [^CompoundTag this key value] (.putString this (str key) (str value)) this)
         :sd-set-string-list! (fn [^CompoundTag this key strings]
                              (let [lst (ListTag.)]
                                (doseq [s strings] (.add lst (StringTag/valueOf (str s))))
                                (.put this (str key) lst))
                              this)
         :sd-get-string-list  (fn [^CompoundTag this key]
                              (let [lst (.getList this (str key) 8)]
                                (vec (for [i (range (.size lst))] (.getString lst i)))))
         :sd-get-string    (fn [^CompoundTag this key] (.getString this (str key)))
         :sd-set-boolean!  (fn [^CompoundTag this key value] (.putBoolean this (str key) (boolean value)) this)
         :sd-get-boolean   (fn [^CompoundTag this key] (.getBoolean this (str key)))
         :sd-set-double!   (fn [^CompoundTag this key value] (.putDouble this (str key) (double value)) this)
         :sd-get-double    (fn [^CompoundTag this key] (.getDouble this (str key)))
         :sd-set-entry!    (fn [^CompoundTag this key entry] (.put this (str key) entry) this)
         :sd-remove-entry! (fn [^CompoundTag this key] (.remove this (str key)))
         :sd-get-entry     (fn [^CompoundTag this key] (.get this (str key)))
         :sd-get-structured (fn [^CompoundTag this key] (.getCompound this (str key)))
         :sd-get-list      (fn [^CompoundTag this key] (.getList this (str key) 10))
         :sd-has-key?      (fn [^CompoundTag this key] (.contains this (str key)))
         :sd-set-float!    (fn [^CompoundTag this key value] (.putFloat this (str key) (float value)) this)
         :sd-get-float     (fn [^CompoundTag this key] (.getFloat this (str key)))
         :sd-set-long!     (fn [^CompoundTag this key value] (.putLong this (str key) (long value)) this)
         :sd-get-long      (fn [^CompoundTag this key] (.getLong this (str key)))
         :sd-append!       (fn [^ListTag this el] (.add this el) this)
         :sd-list-size     (fn [^ListTag this] (.size this))
         :sd-list-get      (fn [^ListTag this idx]
                             (let [i (int idx) n (int (.size this))]
                               (when (and (>= i 0) (< i n)) (.get this i))))
         :sd-list-get-structured (fn [^ListTag this idx]
                                   (let [i (int idx) n (int (.size this))]
                                     (when (and (>= i 0) (< i n)) (.getCompound this i))))
         :create-structured #(CompoundTag.)
         :create-list       #(ListTag.)}
        "mc1211"))))

(defn- install-position! [_adapter]
  (install/framework-once! ::pos-installed
    (fn []
      (pos/install-position-ops!
        {:pos-x (fn [^BlockPos this] (.getX this))
         :pos-y (fn [^BlockPos this] (.getY this))
         :pos-z (fn [^BlockPos this] (.getZ this))
         :create-block-pos (fn [x y z] (BlockPos. (int x) (int y) (int z)))
         :pos-above (fn [^BlockPos p] (.above p))
         :position-get-block-pos (fn [^BlockEntity be] (.getBlockPos be))
         :position-get-pos (fn [^BlockEntity be] (.getBlockPos be))}
        "mc1211"))))

(defn install-item-protocols!
  [adapter]
  (install/framework-once! ::item-protocols-installed
    (fn []
      (item/install-item-ops!
       {:item-is-empty?          (fn [^ItemStack this] (.isEmpty this))
        :item-get-count          (fn [^ItemStack this] (.getCount this))
        :item-get-max-stack-size (fn [^ItemStack this] (.getMaxStackSize this))
        :item-is-equal?          (fn [^ItemStack this ^ItemStack other]
                                  (.is this (.getItem other)))
        :item-save-to-data         (fn [^ItemStack this ^CompoundTag data]
                                      (let [^CompoundTag saved (.save this RegistryAccess/EMPTY)]
                                        (.merge data saved)
                                        data))
        ;; ItemData returns a mutable COPY — callers that mutate must persist via
        ;; ItemData/setCustomData (creative-tab variants do; AC energy writeback TBD).
        :item-ensure-custom-data   (fn [^ItemStack this]
                                      (ItemData/getOrCreateCustomData this))
        :item-get-max-damage       (fn [^ItemStack this] (.getMaxDamage this))
        :item-set-damage!          (fn [^ItemStack this dmg] (.setDamageValue this (int dmg)))
        :item-set-hover-name!     (fn [^ItemStack this name-key]
                                    ;; name-key is a translation key, not
                                    ;; literal text — keeps variants localized.
                                    ;; 1.21.1 removed setHoverName; custom names
                                    ;; are a data component now.
                                    (.set this (DataComponents/CUSTOM_NAME) (Component/translatable (str name-key))))
        :item-copy-stack          (fn [^ItemStack this] (.copy this))
        :item-set-count!          (fn [^ItemStack this n] (.setCount this (int n)))
        :item-get-damage           (fn [^ItemStack this] (.getDamageValue this))
        :item-get-item             (fn [^ItemStack this] (.getItem this))
        :item-get-custom-data      (fn [^ItemStack this]
                                      (when (ItemData/hasCustomData this)
                                        (ItemData/getCustomDataCopy this)))
        :item-split                (fn [^ItemStack this amount] (.split this (int amount)))
        :item-get-description-id   (fn [^Item this] (.getDescriptionId this))
        :item-get-registry-name    (fn [^Item this] (item-ops/item-registry-name adapter this))
        :create-item-from-data     (fn [data] (item-ops/item-stack-of adapter data))
        :create-item-stack-by-id   (fn [item-id count] (item-ops/create-item-stack-by-id adapter (str item-id) (int count)))
        :item-stack-empty?         (fn [stack] (item-ops/item-stack-empty? adapter stack))
        :item-in-tag?              (fn [^ItemStack stack tag-str]
                                     (try
                                       (let [tag-str (str tag-str)
                                             colon (.indexOf tag-str ":")
                                             ns (if (pos? colon) (.substring tag-str 0 (int colon)) "minecraft")
                                             path (if (pos? colon) (.substring tag-str (inc (int colon))) tag-str)
                                             rl (ResourceLocations/of ns path)
                                             tag-key (net.minecraft.tags.TagKey/create
                                                      net.minecraft.core.registries.Registries/ITEM rl)]
                                         (boolean (.is stack tag-key)))
                                       (catch Throwable _ false)))
        :item-tag-stack-resolver   (fn [tag-str count]
                                     (try
                                       (let [tag-str (str tag-str)
                                             colon (.indexOf tag-str ":")
                                             ns (if (pos? colon) (.substring tag-str 0 (int colon)) "minecraft")
                                             path (if (pos? colon) (.substring tag-str (inc (int colon))) tag-str)
                                             rl (ResourceLocations/of ns path)
                                             tag-key (net.minecraft.tags.TagKey/create
                                                      net.minecraft.core.registries.Registries/ITEM rl)
                                             items (.iterator (.getTagOrEmpty net.minecraft.core.registries.BuiltInRegistries/ITEM tag-key))]
                                         (when (.hasNext items)
                                           (let [^Item item (.value ^net.minecraft.core.Holder (.next items))
                                                 stack (ItemStack. item (int count))]
                                             stack)))
                                       (catch Throwable _ nil)))}
       "mc1211")
      (log/debug "mc1211 shared item ops initialized"))))

(defn- install-world! [adapter]
  (install/framework-once! ::world-installed
    (fn []
      (world/install-world-ops! (world-ops-map adapter) "mc1211"))))

(defn install-entity-protocols!
  [adapter]
  (install/framework-once! ::entity-installed
    (fn []
      (let [block-item-info (fn [^Player this]
                              (try
                                (let [^ItemStack stack (.getMainHandItem this)]
                                  (when-not (.isEmpty stack)
                                    (let [^Item item (.getItem stack)
                                          placeable? (BlockRegistry/isPlaceableBlockItem item)
                                          item-id (item-ops/item-registry-name adapter item)]
                                      {:placeable? placeable?
                                       :item-id item-id})))
                                (catch Throwable _
                                  nil)))
            player-impl {:entity-distance-to-sqr (fn [^Entity this x y z] (.distanceToSqr this (double x) (double y) (double z)))
                         :entity-get-x           (fn [^Entity this] (let [^Vec3 pos (.position this)] (double (.x pos))))
                         :entity-get-y           (fn [^Entity this] (let [^Vec3 pos (.position this)] (double (.y pos))))
                         :entity-get-z           (fn [^Entity this] (let [^Vec3 pos (.position this)] (double (.z pos))))
                         :player-creative?       (fn [^Player this] (.isCreative this))
                         :player-spectator?      (fn [^Player this] (.isSpectator this))
                         :player-get-name        (fn [^Player this] (let [^Component nc (.getName this)] (.getString nc)))
                         :player-get-uuid        (fn [^Entity this] (.getUUID this))
                         :player-get-horizontal-facing (fn [^Player this]
                                                          (let [^Direction d (.getDirection this)]
                                                            (.getSerializedName d)))
                         :player-get-main-hand-item-count (fn [^Player this]
                                                            (let [^ItemStack stack (.getMainHandItem this)]
                                                              (if (.isEmpty stack) 0 (int (.getCount stack)))))
                         :player-get-main-hand-item-stack (fn [^Player this]
                                                            (let [^ItemStack stack (.getMainHandItem this)]
                                                              (when-not (.isEmpty stack) stack)))
                         :player-consume-main-hand-item! (fn [^Player this amount]
                                                           (let [n (int (max 0 (or amount 0)))]
                                                             (cond (zero? n) true
                                                                   (.isCreative this) true
                                                                   :else
                                                                   (let [^ItemStack stack (.getMainHandItem this)]
                                                                     (if (or (.isEmpty stack) (< (int (.getCount stack)) n))
                                                                       false
                                                                       (do (.shrink stack n) true))))))
                         :player-get-level (fn [this] (player-ops/player-level adapter this))
                         :player-get-main-hand-item-id (fn [^Player this]
                                                         (let [^ItemStack stack (.getMainHandItem this)]
                                                           (when-not (.isEmpty stack)
                                                             (item-ops/item-registry-name adapter (.getItem stack)))))
                         :player-main-hand-placeable-block? (fn [this]
                                                             (boolean (:placeable? (block-item-info this))))
                         :player-place-main-hand-block-at-hit! (fn [^Player this _world-id x y z face]
                                                                (let [px (int x)
                                                                      py (int y)
                                                                      pz (int z)
                                                                      base-result {:placed? false
                                                                                   :fallback-drop? true
                                                                                   :pos {:x px :y py :z pz}
                                                                                   :face face}]
                                                                  (try
                                                                    ;; Resolve the block from the held item itself (upstream
                                                                    ;; Block.getBlockFromItem) — the item-id path only knew
                                                                    ;; mod-registered blocks, so vanilla block items (dirt,
                                                                    ;; stone, ...) never placed and were consumed silently.
                                                                    (let [^ItemStack stack (.getMainHandItem this)
                                                                          ^Block block (when-not (.isEmpty stack)
                                                                                         (BlockRegistry/blockByItem (.getItem stack)))]
                                                                      (if (and block (not (BlockRegistry/isAirBlock block (BlockRegistry/getAirBlock))))
                                                                        (let [^Level level (player-ops/player-level adapter this)
                                                                              placed? (boolean (.setBlock level (BlockPos. px py pz) (.defaultBlockState block) 3))]
                                                                          (assoc base-result
                                                                                 :placed? placed?
                                                                                 :fallback-drop? (not placed?)))
                                                                        base-result))
                                                                    (catch Throwable _
                                                                      base-result))))
                         :player-drop-main-hand-item-at! (fn [this amount x y z]
                                                           (player-ops/drop-player-main-hand-item-at! adapter this amount x y z))
                         :player-spawn-main-hand-item-copy-at!
                         (fn [^Player this amount x y z]
                           (let [n (int (max 0 (or amount 0)))
                                 ^ItemStack stack (.getMainHandItem this)]
                             (if (or (zero? n) (.isEmpty stack))
                               false
                               (let [^ItemStack copy (.copyWithCount stack
                                                                     (min n (int (.getCount stack))))
                                     ^ItemEntity item-entity
                                     (ItemEntity. (.level this)
                                                  (double x) (double y) (double z)
                                                  copy)]
                                 (boolean (.addFreshEntity (.level this) item-entity))))))
                         :player-count-item-by-id (fn [this item-id] (player-ops/count-player-item-by-id adapter this item-id))
                         :player-consume-item-by-id! (fn [this item-id amount] (player-ops/consume-player-item-by-id! adapter this item-id amount))
                         :player-give-item-stack! (fn [this stack] (player-ops/give-player-item-stack! adapter this stack))
                         :player-spawn-entity-by-id! (fn [this entity-id speed] (player-ops/spawn-entity-by-id! adapter this entity-id speed))
                         :player-spawn-tracked-entity-by-id! (fn [this entity-id speed life] (player-ops/spawn-tracked-entity-by-id-with-life! adapter this entity-id speed life))
                         :player-raytrace-block (fn [this reach fluid-source-only?] (player-ops/raytrace-block adapter this reach fluid-source-only?))
                         :player-get-container-menu (fn [this] (player-ops/player-container-menu adapter this))
                         :inventory-get-player (fn [this] (menu-inventory-ops/inventory-owner adapter this))
                         :menu-get-container-id (fn [this] (menu-inventory-ops/menu-container-id adapter this))}]
        (entity/install-entity-ops! player-impl "mc1211")
        (log/debug "mc1211 shared entity protocols initialized")))))

(defn- install-resource-location-factory! []
  (install/framework-once! ::resource-installed
    (fn []
      (platform/install-adapter! (fw/fw-atom) :resource
                                 {:factory (fn [namespace path]
                                             (if namespace
                                               (ResourceLocations/of (str namespace) (str path))
                                               (ResourceLocations/parse (str path))))})
      (log/debug "mc1211 resource factory installed"))))

(defn install-resource-factory!
  []
  (install-resource-location-factory!)
  (log/debug "mc1211 shared resource factory initialized"))

(defn install-be-fns!
  [fns-map]
  (install/framework-once! ::be-fns-installed
    (fn []
      (be/install-be-ops! fns-map "mc1211")
      (log/debug "mc1211 shared block-entity function hooks initialized"))))

(defn install-world-fns!
  [fns-map]
  (install/framework-once! ::world-fns-installed
    (fn []
      (world/install-world-ops! fns-map "mc1211")
      (log/debug "mc1211 shared world function hooks initialized"))))

(defn install-platform-core!
  [adapter]
  (install-structured-data!)
  (install-position! adapter)
  (install-item-protocols! adapter)
  (install-world! adapter)
  (install-entity-protocols! adapter)
  (install-player-feedback!)
  (install-resource-location-factory!)
  (log/debug "mc1211 shared installer initialized"))

(defn install-platform-services!
  [adapter world-fns-map be-fns-map]
  (install-structured-data!)
  (install-position! adapter)
  (install-entity-protocols! adapter)
  (install-item-protocols! adapter)
  (install-block-state-protocol! adapter)
  (install-player-feedback!)
  (install-resource-factory!)
  (when world-fns-map
    (install-world-fns! world-fns-map))
  (when be-fns-map
    (install-be-fns! be-fns-map))
  (log/debug "mc1211 platform services initialized"))
