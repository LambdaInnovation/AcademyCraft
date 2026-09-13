(ns cn.li.fabric1211.datagen.item-model-provider
  "Fabric 1.21.1 item model datagen provider.

  Emits item model JSON files from DSL item metadata.

  Fabric has no OBJ model loader, so a 3D item's `_3d` model is written as a
  plain vanilla model that only carries the display transforms and the atlas
  texture. The mesh is attached at bake time — see
  `cn.li.fabric1211.client.obj-model-registration`."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mcbase.datagen.gson-util :as gson-util]
            [cn.li.mcbase.datagen.item-model-provider-core :as item-model-core])
    (:import [com.google.gson Gson JsonElement]
           [java.util.concurrent CompletableFuture]
         [net.minecraft.data CachedOutput DataProvider PackOutput PackOutput$PathProvider PackOutput$Target]
           [net.minecraft.resources ResourceLocation]))

(defn- obj-3d-json
  "Vanilla-loadable stand-in for the `_3d` model: no parent and no elements, so
  it bakes to an empty mesh with our display transforms. Declaring `particle`
  gets the OBJ texture stitched onto the block atlas (that is the only slot
  `BlockModel#getMaterials` collects without elements); `default` is what the
  MTL's `map_Kd #default` resolves against."
  [mod-id {:keys [texture display]}]
  (let [texture-id (str mod-id ":" texture)]
    (cond-> {:textures {:particle texture-id
                        :default texture-id}}
      (seq display) (assoc :display display))))

(defn- model-json
  [mod-id {:keys [json obj-model] :as spec}]
  (if obj-model
    (obj-3d-json mod-id spec)
    json))

(defn create-provider
    [^PackOutput output]
  (let [^String mod-id (str modid/mod-id)
      path-provider (.createPathProvider output PackOutput$Target/RESOURCE_PACK "models/item")
      ^Gson gson (gson-util/create-pretty-gson)]
    (reify DataProvider
      (^CompletableFuture run [_ ^CachedOutput cached]
        (let [{:keys [all-item-count energy-tier-count simple-count models]} (item-model-core/gather-model-specs)
              models (item-model-core/flatten-nested-override-chains models)
              writes (atom [])]
          (doseq [{:keys [model-name] :as spec} models
                  :let [json (model-json mod-id spec)]]
            (let [target-path (.json ^PackOutput$PathProvider path-provider (ResourceLocation/fromNamespaceAndPath mod-id model-name))
                  json-tree (.toJsonTree gson (gson-util/normalize-json json))]
              (swap! writes conj
                     (DataProvider/saveStable cached ^JsonElement json-tree ^java.nio.file.Path target-path))))
          (println (str "[item-model-provider/fabric] summary: items=" all-item-count
                        ", energy-tier=" energy-tier-count
                        ", simple-model=" simple-count))
          (CompletableFuture/allOf (into-array CompletableFuture @writes))))
      (getName [_] (str mod-id " Item Model Provider")))))
