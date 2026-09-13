(ns cn.li.mc262.datagen.recipe-provider-custom
  "Custom content recipe emission for datagen (26.2 RecipeOutput + ResourceKey).

  Outputs use ItemStackTemplate (like vanilla 26.2 recipes): its codec uses
  plain Item.CODEC and never requires the item holder to have components
  bound, so datagen and the runtime datapack reload both serialize it."
  (:require [cn.li.mc262.datagen.metadata-resolver :as metadata-resolver]
            [cn.li.mc262.datagen.resource-location :as rl]
            [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.platform.neutral.config :as modid])
  (:import [cn.li.mc262.recipe ContentRecipe]
           [cn.li.mc262.shim DelegatingFinishedRecipe]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.core Holder HolderGetter]
           [net.minecraft.core.registries Registries]
           [net.minecraft.data.recipes RecipeOutput]
           [net.minecraft.resources Identifier ResourceKey]
           [net.minecraft.world.item ItemStackTemplate]
           [net.minecraft.world.item.crafting Ingredient]))

(defn- recipe->content-recipe
  ^ContentRecipe
  [recipe kind ^HolderGetter items]
  (let [^Ingredient input (metadata-resolver/ingredient-from-spec
                           (:input recipe) rl/parse-resource-location items)
        out (:output recipe)
        ^Identifier output-id (rl/parse-resource-location (:item out))
        ^Holder output-holder (.getOrThrow items
                                           (ResourceKey/create Registries/ITEM output-id))
        ^ItemStackTemplate output-template
        (ItemStackTemplate. output-holder (int (:count out 1)))
        consume-liquid (int (or (:consume-liquid recipe) 0))
        craft-time (int (or (:time recipe) 200))
        mode (str (or (:mode recipe) ""))]
    (ContentRecipe. input output-template consume-liquid craft-time mode kind)))

(defn- emit-custom-recipe!
  [^RecipeOutput output recipe kind ^HolderGetter items]
  (let [^String mod-id modid/mod-id
        ^String recipe-id (recipe-core/normalize-recipe-id (:id recipe))
        ^Identifier id (ResourceLocations/of mod-id recipe-id)
        ^ContentRecipe content (recipe->content-recipe recipe kind items)]
    (DelegatingFinishedRecipe/accept output id content nil)))

(defn custom-emitters
  "Emitter map for custom recipe types. HolderGetter<Item> is required."
  ([^RecipeOutput output]
   (custom-emitters output nil))
  ([^RecipeOutput output ^HolderGetter items]
   (when (nil? items)
     (throw (ex-info "custom-emitters requires HolderGetter<Item> on 26.2" {})))
   {:custom-process (fn [recipe]
                      (emit-custom-recipe! output recipe "process" items))
    :custom-mode (fn [recipe]
                   (emit-custom-recipe! output recipe "mode" items))}))
