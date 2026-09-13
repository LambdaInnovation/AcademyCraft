(ns cn.li.fabric1211.registry.creative-tab-event
  "Metadata-driven Fabric creative tab population."
  (:require [cn.li.fabric1211.registry.creative-tab :as tabs]
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
    (.get BuiltInRegistries/ITEM (ResourceLocation/fromNamespaceAndPath modid/mod-id (str id)))
    (catch Throwable _ nil)))

(defn- add-entry! [^FabricItemGroupEntries entries {:keys [type] :as entry}]
  (when-let [^ItemLike item (item-for-entry entry)]
    (.accept entries (ItemStack. item))))

(defn register!
  []
  (doseq [tab (distinct (keep :tab (registry-metadata/get-all-creative-tab-entries)))]
    (.register (ItemGroupEvents/modifyEntriesEvent (tabs/resolve-tab-key tab))
               (reify ItemGroupEvents$ModifyEntries
                 (^void modifyEntries [_ ^FabricItemGroupEntries entries]
                   (doseq [entry (registry-metadata/get-all-creative-tab-entries)]
                     (when (= tab (:tab entry))
                       (add-entry! entries entry)))
                   nil))))
  (log/debug "Fabric 1.21.1 creative tab population registered"))
