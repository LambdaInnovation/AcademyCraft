(ns cn.li.mc1211.client.energy-item-model-properties
  "Registers item model predicates for :item-model-energy-levels items and
   :item-model-damage-frame items.

  Predicate ids are `<modid>:energy` and `<modid>:frame` (see datagen
  item_model_provider)."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.resources ResourceLocation]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.client.renderer.item ItemProperties]
           [net.minecraft.world.item Item]
           [cn.li.mc1211.client.render.item EnergyItemPropertyFunction
            FrameItemPropertyFunction
            MatterKindItemPropertyFunction]
           [cn.li.mc1211.runtime ItemRegistry]))

(defn- resolve-item
  ^Item
  [item-id]
  (ItemRegistry/getItemById
    (modid/namespaced-path (registry-metadata/get-item-registry-name item-id))))

(defn register!
  "Call from client setup after items are registered."
  []
  (let [pred-rl (ResourceLocations/of (str modid/mod-id) "energy")]
    (doseq [item-id (registry-metadata/get-all-item-ids)]
      (when (get-in (registry-metadata/get-item-spec item-id) [:properties :item-model-energy-levels])
        (when-let [item (resolve-item item-id)]
          (ItemProperties/register item pred-rl EnergyItemPropertyFunction/INSTANCE)
          (log/debug "Item model energy predicate registered" {:item-id item-id})))))
  ;; Matter-unit variant + frame predicates (:item-model-damage-frame,
  ;; upstream ItemMatterUnit: per-damage models + `frame` override for the
  ;; flowing-liquid animation).
  (let [kind-rl (ResourceLocations/of (str modid/mod-id) "matter_kind")
        frame-rl (ResourceLocations/of (str modid/mod-id) "frame")]
    (doseq [item-id (registry-metadata/get-all-item-ids)]
      (when (get-in (registry-metadata/get-item-spec item-id) [:properties :item-model-damage-frame])
        (when-let [item (resolve-item item-id)]
          (ItemProperties/register item kind-rl MatterKindItemPropertyFunction/INSTANCE)
          (ItemProperties/register item frame-rl FrameItemPropertyFunction/INSTANCE)
          (log/debug "Item model matter_kind + frame predicates registered" {:item-id item-id}))))))
