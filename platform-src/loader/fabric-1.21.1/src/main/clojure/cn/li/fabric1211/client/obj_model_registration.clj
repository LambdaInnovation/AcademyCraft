(ns cn.li.fabric1211.client.obj-model-registration
  "Fabric ModelLoadingPlugin registration for OBJ-backed items.

  Fabric ships no OBJ model loader, so the `_3d` model JSON is a plain vanilla
  model carrying only the display transforms and the atlas texture; the mesh is
  read from the `.obj` at bake time by `ObjMeshLoader`. Context switching is a
  mixin (`ItemRendererObjCompositeMixin`) because vanilla has no
  `BakedModel#applyTransform`.

  Both replacements ride `modifyModelAfterBake`, which fires inside baking and
  before results are cached. That is what lets the base model's ItemOverrides
  capture composites for the energy tiers — on Forge/NeoForge the equivalent
  event runs after baking, so those loaders need a custom ItemOverrides."
  (:require [cn.li.mc1211.client.render.obj-model-baking :as baking]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.fabric1211.client.render.item ObjBakedModel ObjMeshLoader]
           [cn.li.mc1211.client.render.item ObjCompositeBakedModel]
           [java.util.function Function]
           [net.fabricmc.fabric.api.client.model.loading.v1 ModelLoadingPlugin
                                                           ModelLoadingPlugin$Context
                                                           ModelModifier$AfterBake
                                                           ModelModifier$AfterBake$Context]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.renderer.block.model BlockModel]
           [net.minecraft.client.renderer.texture TextureAtlasSprite]
           [net.minecraft.client.resources.model BakedModel BlockModelRotation ModelBaker]
           [net.minecraft.resources ResourceLocation]))

(defn- mod-rl
  ^ResourceLocation [path]
  (ResourceLocation/fromNamespaceAndPath (str modid/mod-id) (str path)))

(defn- model-key
  "Normalize a baked-model id to a bare model name.

  `modifyModelAfterBake` fires with plain ResourceLocations (`<ns>:item/foo`)
  while baking, and with ModelResourceLocations (`<ns>:foo#inventory`, whose
  path carries no `item/`) for top-level models."
  [^ResourceLocation id]
  (let [path (.getPath id)]
    (if (.startsWith path "item/")
      (subs path (count "item/"))
      path)))

(defn- mesh-lookup
  "model name of each `_3d` model → its OBJ resource path."
  []
  (into {}
        (map (fn [{:keys [basename obj-path]}] [(str basename "_3d") obj-path]))
        (baking/obj-3d-item-specs)))

(defn- composite-lookup
  "model name of every flat model that should render the mesh outside the GUI —
  the item's base model plus its energy-tier override targets → `_3d` model path."
  []
  (into {}
        (mapcat (fn [{:keys [basename model-path]}]
                  (map (fn [suffix] [(str basename suffix) model-path])
                       ["" "_half" "_full"])))
        (baking/obj-3d-item-specs)))

(defn- slot-sprite-fn
  "Resolve an MTL `#slot` reference through the `_3d` model's texture slots."
  ^Function [source-model ^Function texture-getter]
  (reify Function
    (apply [_ slot]
      (when (instance? BlockModel source-model)
        (let [^BlockModel block-model source-model
              slot-name (str slot)]
          (when (.hasTexture block-model slot-name)
            (.apply texture-getter (.getMaterial block-model slot-name))))))))

(defn- obj-mesh-model
  "Swap the vanilla-baked placeholder for the OBJ mesh, keeping its transforms."
  ^BakedModel [^BakedModel model ^ModelModifier$AfterBake$Context ctx obj-path]
  (let [^TextureAtlasSprite particle (.getParticleIcon model)]
    (try
      (ObjBakedModel. (ObjMeshLoader/load (.getResourceManager (Minecraft/getInstance))
                                          (mod-rl obj-path)
                                          (slot-sprite-fn (.sourceModel ctx) (.textureGetter ctx))
                                          particle)
                      (.getTransforms model)
                      particle)
      (catch Exception e
        (log/stacktrace (str "[obj-model-registration] could not build OBJ mesh from" obj-path "-" ) e)
        model))))

(defn- after-bake
  [^BakedModel model ^ModelModifier$AfterBake$Context ctx meshes composites]
  (if (nil? model)
    model
    (let [key (model-key (.resourceId ^ModelModifier$AfterBake$Context ctx))]
      (cond
        ;; Already swapped — a model can be baked under both its plain and its
        ;; top-level id, and re-wrapping would nest composites.
        (or (instance? ObjBakedModel model) (instance? ObjCompositeBakedModel model))
        model

        (contains? meshes key)
        (obj-mesh-model model ctx (get meshes key))

        (contains? composites key)
        (let [^ModelBaker baker (.baker ctx)]
          (ObjCompositeBakedModel. model (.bake baker (mod-rl (get composites key))
                                                BlockModelRotation/X0_Y0)))

        :else model))))

(defn register!
  "Install the plugin. Safe to call once from client init."
  []
  (ModelLoadingPlugin/register
    (reify ModelLoadingPlugin
      (onInitializeModelLoader [_ plugin-context]
        (let [^ModelLoadingPlugin$Context plugin-context plugin-context
              specs (baking/obj-3d-item-specs)
              meshes (mesh-lookup)
              composites (composite-lookup)]
          (when (seq specs)
            (.addModels plugin-context
                        ^java.util.Collection (mapv (comp mod-rl :model-path) specs))
            (.register (.modifyModelAfterBake plugin-context)
                       (reify ModelModifier$AfterBake
                         (modifyModelAfterBake [_ model ctx]
                           (after-bake model ctx meshes composites))))
            (log/debug "[obj-model-registration] OBJ item models registered:"
                      (mapv :item-id specs))))))))
