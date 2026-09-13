(ns cn.li.neoforge262.registry.creative-tab-event
  "BuildCreativeModeTabContentsEvent handler 鈥?1.20+ data-driven creative tab population.

  Replaces the legacy `displayItems` callback on CreativeModeTab.builder()
  with the event-driven pattern recommended by official NeoForge 26.2 docs.

  Architecture:
  - Registered on the ModEventBus alongside lifecycle listeners.
  - Fires lazily when the creative inventory screen opens.
  - Iterates the platform-neutral DSL registry metadata to discover items/blocks.
  - Adds each item + optional NBT variants (energy fully-charged, filled-variant)."
  (:require [cn.li.neoforgebase.registry.state :as registry-state]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [clojure.string :as str])
  (:import [cn.li.mc262.bridge McAccess] [net.minecraft.world.item ItemStack]
           [net.minecraft.world.level ItemLike]
           [net.neoforged.neoforge.event BuildCreativeModeTabContentsEvent]
           [net.minecraft.network.chat Component]
           [cn.li.mcver ItemData]
           [cn.li.mc262.bridge ItemStackInterop]))


;; 鈹€鈹€ NBT variant helpers 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

(defn- ^:private accept-energy-charged-variant
  "Add a fully-charged energy item variant to the creative tab output.

  Reads :energy-capacity, :energy-bandwidth, :battery-type from the item spec
  and writes them as NBT tags on a new ItemStack."
  [^BuildCreativeModeTabContentsEvent event item-id ^ItemLike item-obj {:keys [properties]}]
  (let [capacity (double (or (:energy-capacity properties) 0.0))]
    (when (pos? capacity)
      (try
        (let [full-stack (ItemStack. item-obj)
              tag (ItemData/getOrCreateCustomData full-stack)
              bandwidth (double (or (:energy-bandwidth properties) 0.0))
              battery-type (or (:battery-type properties) item-id)]
          (.putDouble tag "energy" capacity)
          (.putDouble tag "maxEnergy" capacity)
          (.putDouble tag "bandwidth" bandwidth)
          (.putString tag "batteryType" (str battery-type))
          (ItemData/setCustomData full-stack tag)
          (.accept event full-stack))
        (catch Exception e (log/warn "Failed to create energy variant for" item-id (ex-message e)) nil)))))


(defn- ^:private accept-filled-variant
  "Add a generic filled-variant item to the creative tab.

  Reads :filled-variant map from item spec properties and writes NBT + damage.
  Sets a translation-key-based hover name to distinguish the variant from the
  default item: `item.<modid>.<item-id>_<label-as-snake-case>`."
  [^BuildCreativeModeTabContentsEvent event ^ItemLike item-obj variant item-id]
  (try
    (let [variant-stack (ItemStack. item-obj)
          tag (ItemData/getOrCreateCustomData variant-stack)]
      (doseq [[k v] (:nbt variant)]
        (cond
          (string? v) (.putString tag (str k) (str v))
          (number? v) (.putDouble tag (str k) (double v))
          (instance? Boolean v) (.putBoolean tag (str k) (boolean v))
          :else nil))
      (ItemData/setCustomData variant-stack tag)
      (when-let [dmg (:damage variant)]
        (.setDamageValue variant-stack (int dmg)))
      ;; Distinct display name so the variant doesn't duplicate the default entry
      (when-let [label (:label variant)]
        (let [label-key (str/replace (str label) #"-" "_")
              mod-id (var-get #'modid/mod-id)
              translation-key (str "item." mod-id "." item-id "_" label-key)]
          (ItemStackInterop/setHoverName variant-stack (Component/translatable translation-key))))
      (.accept event variant-stack))
    (catch Exception e (log/warn "Failed to create filled variant for" item-id (ex-message e)) nil)))


;; 鈹€鈹€ Event handler 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

(defn- ^:private accept-entry!
  "Add a single registry metadata entry (item or block-item) and any NBT
  variants to the creative tab being built."
  [^BuildCreativeModeTabContentsEvent event {:keys [type id]}]
  (let [item-obj (if (= type :block-item)
                   (registry-state/get-registered-block-item id)
                   (registry-state/get-registered-item id))]
    (when item-obj
      ;; Default (empty) instance
      (.accept event ^ItemLike item-obj)
      ;; Stateful variants 鈥?only for standalone items, not block-items
      (when (= type :item)
        (when-let [spec (registry-metadata/get-item-spec id)]
          (let [props (:properties spec)]
            (when (true? (:energy-item? props))
              (accept-energy-charged-variant event id item-obj spec))
            (when-let [variant (:filled-variant props)]
              (accept-filled-variant event item-obj variant id))))))))


(defn handle-build-contents
  "ModEventBus handler for BuildCreativeModeTabContentsEvent.

  Populates every creative tab registered under this mod's namespace.
  Entries are yielded by `registry-metadata/get-all-creative-tab-entries`
  in DSL registration order, which deterministically controls tab layout.

  Items with :creative-tab nil are skipped (e.g. internal logo item)."
  [^BuildCreativeModeTabContentsEvent event]
  (let [tab-location (McAccess/resourceKeyId (.getTabKey event))
        mod-id-str (var-get #'modid/mod-id)]
    (when (= (.getNamespace tab-location) mod-id-str)
      (doseq [entry (registry-metadata/get-all-creative-tab-entries)]
        (when (some? (:tab entry))
          (accept-entry! event entry))))))
