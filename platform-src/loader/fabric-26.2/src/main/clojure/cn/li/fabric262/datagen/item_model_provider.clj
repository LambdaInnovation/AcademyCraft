(ns cn.li.fabric262.datagen.item-model-provider
  "Fabric 26.2 item model datagen provider.

  Emits item model JSON files from DSL item metadata.

  Fabric has no OBJ model loader, so a 3D item's `_3d` model is written as a
  plain vanilla model that only carries the display transforms and the atlas
  texture. The mesh is attached at bake time — see
  `cn.li.fabric262.client.obj-model-registration`."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mcbase.datagen.blockstate-provider-core :as blockstate-core]
            [cn.li.mcbase.datagen.gson-util :as gson-util]
            [cn.li.mcbase.datagen.item-model-provider-core :as item-model-core]
            [clojure.string :as str])
    (:import [com.google.gson Gson JsonElement]
           [java.util.concurrent CompletableFuture]
         [net.minecraft.data CachedOutput DataProvider PackOutput PackOutput$PathProvider PackOutput$Target]
           [net.minecraft.resources Identifier]))

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

(defn- model-ref [model-name]
  {:type "minecraft:model" :model (str modid/mod-id ":item/" model-name)})

(defn- item-definition [spec obj-bases specs-by-name]
  (let [name (str (:model-name spec))
        ;; Overrides become nested range_dispatch trees keyed on their
        ;; predicate property (energy tiers: academy:energy; matter unit:
        ;; minecraft:damage → academy:frame animation chain).
        flat (item-model-core/item-model-tree specs-by-name name)]
    {:model (if (contains? obj-bases name)
              {:type "minecraft:select"
               :property "minecraft:display_context"
               :cases [{:when "gui" :model flat}]
               :fallback (model-ref (str name "_3d"))}
              flat)}))

(defn- auxiliary-model-names [models]
  (into #{}
        (concat
          (mapcat (fn [{:keys [json]}]
                    (keep #(some-> (:model %) str (str/split #"/") last) (:overrides json))) models)
          (keep (fn [{:keys [model-name obj-model]}]
                  (when obj-model (str model-name))) models))))

(defn- block-item-definitions
  "items/*.json definitions for block items.

  The blockstate provider writes models/item/*.json for block items but not
  the 1.21.4+ client-item definition; without it ModelManager reports
  \"Missing item model\" and renders the missing-mesh. Block items render
  their own item model file, so the definition is a plain model reference."
  [^PackOutput$PathProvider item-path-provider ^Gson gson models]
  (keep (fn [{:keys [path-key id]}]
          (when (and (= :item-model path-key) (string? id))
            (let [block-item-name (second (str/split id #":"))]
              (when (and block-item-name
                         (not (contains? (into #{}
                                               (map (fn [s] (str (:model-name s))))
                                               models)
                                         block-item-name)))
                {:path (.json ^PackOutput$PathProvider item-path-provider
                              (Identifier/fromNamespaceAndPath (str modid/mod-id) block-item-name))
                 :json-tree (.toJsonTree gson
                                         (gson-util/normalize-json {:model (model-ref block-item-name)}))}))))
        (blockstate-core/blockstate-write-entries)))

(defn create-provider
    [^PackOutput output]
  (let [^String mod-id (str modid/mod-id)
      path-provider (.createPathProvider output PackOutput$Target/RESOURCE_PACK "models/item")
      item-path-provider (.createPathProvider output PackOutput$Target/RESOURCE_PACK "items")
      ^Gson gson (gson-util/create-pretty-gson)]
    (reify DataProvider
      (^CompletableFuture run [_ ^CachedOutput cached]
        (let [{:keys [all-item-count energy-tier-count simple-count models]} (item-model-core/gather-model-specs)
              specs-by-name (into {} (map (fn [s] [(str (:model-name s)) s])) models)
              auxiliary (auxiliary-model-names models)
              obj-bases (into #{} (keep (fn [{:keys [model-name obj-model]}]
                                          (when (and obj-model (str/ends-with? (str model-name) "_3d"))
                                            (subs (str model-name) 0 (- (count (str model-name)) 3))))) models)
              writes (atom [])]
          (doseq [{:keys [model-name] :as spec} models
                  :let [json (model-json mod-id spec)]]
            (let [target-path (.json ^PackOutput$PathProvider path-provider
                                     (Identifier/fromNamespaceAndPath mod-id model-name))
                  json-tree (.toJsonTree gson (gson-util/normalize-json json))]
              (swap! writes conj
                     (DataProvider/saveStable cached ^JsonElement json-tree ^java.nio.file.Path target-path))))
          (doseq [{:keys [model-name] :as spec} models
                  :when (not (contains? auxiliary (str model-name)))]
            (let [target-path (.json ^PackOutput$PathProvider item-path-provider
                                     (Identifier/fromNamespaceAndPath mod-id model-name))
                  json-tree (.toJsonTree gson (gson-util/normalize-json (item-definition spec obj-bases specs-by-name)))]
              (swap! writes conj
                     (DataProvider/saveStable cached ^JsonElement json-tree ^java.nio.file.Path target-path))))
          (doseq [{:keys [path json-tree]} (block-item-definitions item-path-provider gson models)]
            (swap! writes conj
                   (DataProvider/saveStable cached ^JsonElement json-tree ^java.nio.file.Path path)))
          (println (str "[item-model-provider/fabric] summary: items=" all-item-count
                        ", energy-tier=" energy-tier-count
                        ", simple-model=" simple-count))
          (CompletableFuture/allOf (into-array CompletableFuture @writes))))
      (getName [_] (str mod-id " Item Model Provider")))))
