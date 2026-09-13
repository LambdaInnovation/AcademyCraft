(ns cn.li.neoforge1211.datagen.item-model-provider
  "NeoForge 1.21.1 Item Model datagen — generates model JSON from item DSL.

  Supported model types:
  - :item/generated (simple items with a single layer texture)
  - Energy-tier items (generated base + half/full tier models with :energy predicate)
  - neoforge:obj 3D items (OBJ model with display transforms per perspective)

  Optional :item-model-energy-levels in :properties generates base + tiered
  sibling models and overrides on predicate <modid>:energy (see client
  `cn.li.mc1211.client.energy-item-model-properties`).

  Optional :item-model-3d-obj in :properties generates a neoforge:obj loader model
  with perspective-specific display transforms.

  Optional :item-model-display in :properties writes vanilla `display` transforms
  on simple generated models (e.g. ItemCoin hand/ground scales)."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mc1211.datagen.resource-location :as rl]
            [cn.li.mcbase.datagen.item-model-provider-core :as item-model-core]
            [clojure.string :as str])
  (:import [cn.li.neoforge1211.shim DelegatingItemModelProvider DelegatingCustomLoaderBuilder]
           [net.minecraft.data PackOutput]
           [net.minecraft.resources ResourceLocation]
           [net.minecraft.world.item ItemDisplayContext]
           [net.neoforged.neoforge.common.data ExistingFileHelper]
           [net.neoforged.neoforge.client.model.generators ItemModelProvider ItemModelBuilder ModelFile$ExistingModelFile]))

(def ^:private display-context-by-key
  "Map item-model JSON perspective keys → ItemDisplayContext via serialized names."
  (into {}
        (map (fn [^ItemDisplayContext ctx]
               [(.getSerializedName ctx) ctx])
             (ItemDisplayContext/values))))

(defn- display-key-name
  [k]
  (cond
    (keyword? k) (name k)
    (string? k) (str/lower-case k)
    :else (str k)))

(defn- apply-display!
  "Write perspective transforms onto an ItemModelBuilder from a display map."
  [^ItemModelBuilder builder display]
  (when (map? display)
    (let [transforms (.transforms builder)]
      (doseq [[perspective opts] display
              :let [ctx (get display-context-by-key (display-key-name perspective))]
              :when (and ctx (map? opts))]
        (let [tb (.transform transforms ^ItemDisplayContext ctx)
              rotation (or (:rotation opts) (get opts "rotation"))
              translation (or (:translation opts) (get opts "translation"))
              scale (or (:scale opts) (get opts "scale"))]
          (when (sequential? rotation)
            (.rotation tb (float (nth rotation 0)) (float (nth rotation 1)) (float (nth rotation 2))))
          (when (sequential? translation)
            (.translation tb (float (nth translation 0)) (float (nth translation 1)) (float (nth translation 2))))
          (when (sequential? scale)
            (.scale tb (float (nth scale 0)) (float (nth scale 1)) (float (nth scale 2))))
          (.end tb)))
      (.end transforms)))
  builder)

(defn- apply-model-spec!
  [^ItemModelProvider provider ^ExistingFileHelper exfile-helper {:keys [model-name json]}]
  (let [^ResourceLocation parent-rl (or (rl/parse-resource-location (:parent json))
                                        (.mcLoc provider "item/generated"))
        ^ItemModelBuilder builder (.withExistingParent provider (str model-name) parent-rl)]
    (doseq [[layer texture-id] (:textures json)]
      (.texture builder (name layer) ^ResourceLocation (rl/parse-resource-location texture-id modid/mod-id)))
    ;; Write overrides in core order (ASCENDING thresholds). Vanilla 1.21.1
    ;; ItemOverrides bakes the JSON list in reverse and resolve() then walks
    ;; the array forward picking the first value >= threshold — ascending
    ;; JSON is what makes the highest threshold win. (Older providers
    ;; reversed here, producing descending JSON: the first threshold always
    ;; matched, freezing damage-frame animations on their first frame.)
    (doseq [{:keys [predicate model]} (:overrides json)]
      (let [override-builder (.override builder)
            model-file (ModelFile$ExistingModelFile. (rl/parse-resource-location model modid/mod-id) exfile-helper)]
        (doseq [[predicate-id value] predicate]
          (.predicate override-builder (rl/parse-resource-location predicate-id modid/mod-id) (float value)))
        (.model override-builder model-file)
        (.end override-builder)))
    (apply-display! builder (:display json))
    builder))

;; ============================================================================
;; OBJ 3D model generation
;; ============================================================================

(defn- json-float-array
  "Build a Gson JsonArray from numeric values."
  ^com.google.gson.JsonArray [xs]
  (let [arr (com.google.gson.JsonArray.)]
    (doseq [x xs] (.add arr (Float/valueOf (float x))))
    arr))

(defn- perspective-json
  ^com.google.gson.JsonObject [{:keys [rotation scale translation]}]
  (let [obj (com.google.gson.JsonObject.)]
    (when rotation (.add obj "rotation" (json-float-array rotation)))
    (when scale (.add obj "scale" (json-float-array scale)))
    (when translation (.add obj "translation" (json-float-array translation)))
    obj))

(defn- obj-model-json
  "Emit neoforge:obj loader JSON.

  The loader is registered as `neoforge:obj` on 1.21.1 — there is no `forge:`
  alias. Key names are the ones ObjLoader#read actually reads (snake_case);
  the material texture must go in `textures`, because the MTL's
  `map_Kd #default` is resolved through the model's texture slots, not through
  `custom` data. Slot values are atlas paths — no `textures/` prefix and no
  `.png`. `emissive_ambient` is off so the mesh takes normal world lighting
  like upstream's ObjLegacyRender (the MTL's Ka 1,1,1 would fullbright it)."
  ^com.google.gson.JsonObject [mod-id {:keys [obj-model texture display]}]
  (let [json (com.google.gson.JsonObject.)
        texture-id (str mod-id ":" texture)
        _ (.addProperty json "loader" "neoforge:obj")
        _ (.addProperty json "model" (str mod-id ":" obj-model))
        _ (.addProperty json "flip_v" true)
        _ (.addProperty json "automatic_culling" false)
        _ (.addProperty json "emissive_ambient" false)
        textures (doto (com.google.gson.JsonObject.)
                   (.addProperty "particle" texture-id)
                   (.addProperty "default" texture-id))
        _ (.add json "textures" textures)
        ;; Display transforms per perspective
        display-json (reduce-kv (fn [^com.google.gson.JsonObject obj k v]
                                  (.add obj (name k) (perspective-json v))
                                  obj)
                                (com.google.gson.JsonObject.)
                                display)]
    (.add json "display" display-json)
    json))

(defn- apply-obj-model-spec!
  "Generate a neoforge:obj model JSON for a 3D item via datagen.

  Uses ItemModelBuilder.customLoader to emit a neoforge:obj loader model with
  perspective display transforms matching the upstream
  BakedModelForTEISR transform matrices.

  In NeoForge 1.21.1 (47.1.0), customLoader takes a single BiFunction argument,
  not a BiFunction + ExistingFileHelper pair. The BiFunction receives the
  parent builder and existing-file helper, and must return a
  CustomLoaderBuilder whose toJson() produces the final model JSON."
  [^ItemModelProvider provider ^ExistingFileHelper _exfile-helper {:keys [model-name] :as spec}]
  (let [^ItemModelBuilder builder (.withExistingParent provider (str model-name) "item/generated")
        mod-id (str modid/mod-id)
        loader-rl (rl/parse-resource-location "neoforge:obj")]
    (.customLoader builder
                   (reify java.util.function.BiFunction
                     (apply [_ parent-builder helper]
                       (DelegatingCustomLoaderBuilder.
                         loader-rl parent-builder helper
                         (fn [] (obj-model-json mod-id spec))))))
    builder))

(defn create
  "Create Item Model DataProvider instance (factory signature: PackOutput -> DataProvider)"
  [^PackOutput pack-output ^ExistingFileHelper exfile-helper]
  (DelegatingItemModelProvider.
    pack-output modid/mod-id exfile-helper
    (fn [^DelegatingItemModelProvider this-provider]
      (let [{:keys [all-item-count energy-tier-count obj-3d-count simple-count bucket-count models]}
            (item-model-core/gather-model-specs)
            models (item-model-core/flatten-nested-override-chains models)]
        ;; Standard models: item/generated, energy-tier, fluid buckets
        ;; OBJ 3D models: neoforge:obj loader
        (doseq [model-spec models]
          (if (:obj-model model-spec)
            (apply-obj-model-spec! this-provider exfile-helper model-spec)
            (apply-model-spec! this-provider exfile-helper model-spec)))
        (println (str "[item-model-provider] summary: items=" all-item-count
                      ", energy-tier=" energy-tier-count
                      ", obj-3d=" obj-3d-count
                      ", simple-model=" simple-count
                      ", buckets=" bucket-count))))))
