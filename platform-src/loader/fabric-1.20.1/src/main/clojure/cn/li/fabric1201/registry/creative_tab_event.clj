(ns cn.li.fabric1201.registry.creative-tab-event
  "Metadata-driven Fabric creative tab population.

  Fabric exposes item-group events instead of Forge's build-contents event;
  this adapter keeps the same DSL ordering and supports charged/filled item
  variants without leaking the loader API into shared metadata code."
  (:require [cn.li.fabric1201.registry.creative-tab :as tabs]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.fabric.api.itemgroup.v1 ItemGroupEvents ItemGroupEvents$ModifyEntries FabricItemGroupEntries]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.resources ResourceLocation]
           [net.minecraft.world.level ItemLike]))

(defn- item-for-entry [{:keys [id]}]
  (try
    (.get BuiltInRegistries/ITEM (ResourceLocation. (str modid/mod-id) (str id)))
    (catch Throwable _ nil)))

(defn- add-entry! [^FabricItemGroupEntries entries {:keys [type id] :as entry}]
  (when-let [^ItemLike item (item-for-entry entry)]
    (.accept entries (ItemStack. item))
    (when (= type :item)
      (when-let [spec (registry-metadata/get-item-spec id)]
        (let [props (:properties spec)
              capacity (double (or (:energy-capacity props) 0.0))]
          (when (and (true? (:energy-item? props)) (pos? capacity))
            (let [stack (ItemStack. item)]
              (.setDamageValue stack 0)
              (.accept entries stack))))))))

(defn register!
  []
  (doseq [tab (distinct (keep :tab (registry-metadata/get-all-creative-tab-entries)))]
    (let [tab-key (tabs/resolve-tab-key tab)]
      (.register (ItemGroupEvents/modifyEntriesEvent tab-key)
                 (reify ItemGroupEvents$ModifyEntries
                   (^void modifyEntries [_ ^FabricItemGroupEntries entries]
                     (doseq [entry (registry-metadata/get-all-creative-tab-entries)]
                       (when (= tab (:tab entry))
                         (add-entry! entries entry)))
                      nil)))))
  (log/debug "Fabric creative tab population registered"))
