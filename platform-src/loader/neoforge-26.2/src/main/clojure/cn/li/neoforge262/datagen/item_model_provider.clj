(ns cn.li.neoforge262.datagen.item-model-provider
  "Minecraft 26.2 item-model datagen.

   Writes vanilla model JSON plus the 1.21.4+ assets/<ns>/items definition.
   Energy items use a range_dispatch property registered as academy:energy.
   OBJ models use NeoForge's built-in neoforge:obj geometry loader (no custom
   ModelEvent baking). GUI-vs-hand switching uses minecraft:select +
   minecraft:display_context in the item definition (replaces ObjComposite)."
  (:require [cn.li.mcbase.datagen.blockstate-provider-core :as blockstate-core]
            [cn.li.mcbase.datagen.item-model-provider-core :as item-model-core]
            [cn.li.platform.neutral.config :as modid]
            [clojure.string :as str])
  (:import [com.google.gson JsonArray JsonElement JsonNull JsonObject JsonPrimitive]
           [java.util.concurrent CompletableFuture]
           [net.minecraft.data CachedOutput DataProvider PackOutput PackOutput$PathProvider PackOutput$Target]
           [cn.li.mcver ResourceLocations]))

(defn- json-element
  ^JsonElement [value]
  (cond
    (nil? value) JsonNull/INSTANCE
    (instance? JsonElement value) value
    (map? value)
    (let [obj (JsonObject.)]
      (doseq [[k v] value]
        (.add obj (name k) (json-element v)))
      obj)
    (sequential? value)
    (let [arr (JsonArray.)]
      (doseq [v value]
        (.add arr (json-element v)))
      arr)
    (boolean? value) (JsonPrimitive. ^Boolean value)
    (number? value) (JsonPrimitive. ^Number value)
    :else (JsonPrimitive. (str value))))

(defn- normalize-display
  [display]
  (when (map? display)
    (into {}
          (map (fn [[perspective transforms]]
                 [(name perspective)
                  (into {}
                        (keep (fn [[k v]]
                                (when (sequential? v)
                                  [(name k) (mapv float v)])))
                        transforms)]))
          display)))

(defn- vanilla-model-json
  [{:keys [parent textures display]}]
  (cond-> {:parent (str (or parent "item/generated"))
           :textures (into {}
                           (map (fn [[layer texture]]
                                  [(name layer) (str texture)]))
                           textures)}
    (map? display) (assoc :display (normalize-display display))))

(defn- obj-model-json
  "Emit neoforge:obj loader JSON per NeoForge 26.x schema.

   Texture keys are atlas paths (no textures/ prefix, no .png). MTL files
   should reference them as #default / #particle.

   Culling is off and emissive_ambient is off so the mesh renders every face
   under normal world lighting, matching upstream's ObjLegacyRender."
  [{:keys [obj-model texture display]}]
  (let [tex (str modid/mod-id ":" texture)]
    (cond-> {:loader "neoforge:obj"
             :model (str modid/mod-id ":" obj-model)
             :flip_v true
             :automatic_culling false
             :shade_quads true
             :emissive_ambient false
             :textures {:particle tex
                        :default tex}}
      (map? display) (assoc :display (normalize-display display)))))

(defn- model-json
  [{:keys [json obj-model] :as spec}]
  (if obj-model
    (obj-model-json spec)
    (vanilla-model-json json)))

(defn- model-ref
  [model-name]
  {:type "minecraft:model"
   :model (str modid/mod-id ":item/" model-name)})

(defn- obj-3d-basenames
  "Basenames that have a companion `_3d` neoforge:obj model."
  [models]
  (into #{}
        (keep (fn [{:keys [model-name obj-model]}]
                (let [name (str model-name)]
                  (when (and obj-model (str/ends-with? name "_3d"))
                    (subs name 0 (- (count name) 3))))))
        models))

(defn- item-definition
  "Item definition for a registry item.

   Overrides become nested range_dispatch trees keyed on their predicate
   property (energy tiers: academy:energy; matter unit: minecraft:damage →
   academy:frame animation chain). When a `_3d` OBJ companion exists, wrap
   with display_context select: GUI flat, all other contexts use the OBJ
   model. Matches former ObjCompositeBakedModel behavior without runtime
   baking."
  [spec obj-3d-bases specs-by-name]
  (let [name (str (:model-name spec))
        flat (item-model-core/item-model-tree specs-by-name name)]
    (if (contains? obj-3d-bases name)
      {:model
       {:type "minecraft:select"
        :property "minecraft:display_context"
        :cases [{:when "gui"
                 :model flat}]
        :fallback (model-ref (str name "_3d"))}}
      {:model flat})))

(defn- auxiliary-model-names
  "Model names that are only referenced from overrides / OBJ companions —
   they must not get a top-level items/*.json."
  [models]
  (let [from-overrides
        (into #{}
              (mapcat (fn [{:keys [json]}]
                        (keep (fn [{:keys [model]}]
                                (some-> model str (str/split #"/") last))
                              (:overrides json))))
              models)
        from-obj-3d
        (into #{}
              (keep (fn [{:keys [model-name obj-model]}]
                      (when obj-model (str model-name))))
              models)]
    (into from-overrides from-obj-3d)))

(defn- save-json!
  [^CachedOutput output path value]
  (DataProvider/saveStable output (json-element value) path))

(defn- block-item-definitions
  "items/*.json definitions for block items.

  The blockstate provider writes models/item/*.json for block items but not
  the 1.21.4+ client-item definition; without it ModelManager reports
  \"Missing item model\" and renders the missing-mesh. Block items render
  their own item model file, so the definition is a plain model reference."
  [^CachedOutput output ^PackOutput$PathProvider item-paths models]
  (keep (fn [{:keys [path-key id]}]
          (when (and (= :item-model path-key) (string? id))
            (let [block-item-name (second (str/split id #":"))]
              (when (and block-item-name
                         (not (contains? (into #{}
                                               (map (fn [s] (str (:model-name s))))
                                               models)
                                         block-item-name)))
                (save-json! output
                            (.json item-paths
                                   (ResourceLocations/of (str modid/mod-id) block-item-name))
                            {:model (model-ref block-item-name)})))))
        (blockstate-core/blockstate-write-entries)))

(defn create
  "Create a DataProvider that writes models/item and items definitions."
  [^PackOutput pack-output]
  (let [model-paths (.createPathProvider pack-output PackOutput$Target/RESOURCE_PACK "models/item")
        item-paths (.createPathProvider pack-output PackOutput$Target/RESOURCE_PACK "items")]
    (reify DataProvider
      (getName [_] "Academy Item Models (26.2)")
      (run [_ output]
        (let [{:keys [models all-item-count energy-tier-count obj-3d-count simple-count bucket-count]}
              (item-model-core/gather-model-specs)
              specs-by-name (into {} (map (fn [s] [(str (:model-name s)) s])) models)
              auxiliary (auxiliary-model-names models)
              obj-bases (obj-3d-basenames models)
              futures
              (concat
                (map (fn [{:keys [model-name] :as spec}]
                       (save-json! output
                                   (.json model-paths
                                          (ResourceLocations/of (str modid/mod-id) (str model-name)))
                                   (model-json spec)))
                     models)
                (keep (fn [{:keys [model-name] :as spec}]
                        (when-not (contains? auxiliary (str model-name))
                          (save-json! output
                                      (.json item-paths
                                             (ResourceLocations/of (str modid/mod-id) (str model-name)))
                                      (item-definition spec obj-bases specs-by-name))))
                      models)
                (block-item-definitions output item-paths models))]
          (println (str "[item-model-provider] summary: items=" all-item-count
                        ", energy-tier=" energy-tier-count
                        ", obj-3d=" obj-3d-count
                        ", simple-model=" simple-count
                        ", buckets=" bucket-count))
          (CompletableFuture/allOf
            (into-array CompletableFuture futures)))))))
