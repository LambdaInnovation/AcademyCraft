(ns cn.li.mc1211.datagen.recipe-provider-custom
  "Custom content recipe emission for datagen (1.21.1 RecipeOutput)."
  (:require [cn.li.mc1211.datagen.metadata-resolver :as metadata-resolver]
            [cn.li.mc1211.datagen.resource-location :as rl]
            [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.platform.neutral.config :as modid])
  (:import [cn.li.mc1211.recipe ContentRecipe]
           [cn.li.mc1211.shim DelegatingFinishedRecipe]
           [net.minecraft.data.recipes RecipeOutput]
           [net.minecraft.resources ResourceLocation]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.world.item ItemStack]
           [net.minecraft.world.item.crafting Ingredient]
           [net.minecraft.world.level ItemLike]))

(defn- recipe->content-recipe
  ^ContentRecipe [recipe kind]
  (let [^Ingredient input (metadata-resolver/ingredient-from-spec (:input recipe) rl/parse-resource-location)
        out (:output recipe)
        ^ItemLike item (metadata-resolver/resolve-item (:item out) rl/parse-resource-location)
        ^ItemStack stack (ItemStack. item (int (:count out 1)))
        consume-liquid (int (or (:consume-liquid recipe) 0))
        craft-time (int (or (:time recipe) 200))
        mode (str (or (:mode recipe) ""))]
    (ContentRecipe. input stack consume-liquid craft-time mode kind)))

(defn- emit-custom-recipe!
  "Accept a ContentRecipe into RecipeOutput (FinishedRecipe removed in 1.21)."
  [^RecipeOutput output recipe kind]
  (let [^String mod-id modid/mod-id
        ^String recipe-id (recipe-core/normalize-recipe-id (:id recipe))
        ^ResourceLocation id (ResourceLocations/of mod-id recipe-id)
        ^ContentRecipe content (recipe->content-recipe recipe kind)]
    (DelegatingFinishedRecipe/accept output id content nil)))

(defn custom-emitters
  "Return emitter map for custom recipe types keyed by recipe type keyword.
  Each emitter is a 1-arg fn [recipe]; the writer is captured in a closure
  to match emit-recipes!'s calling convention."
  [^RecipeOutput output]
  {:custom-process (fn [recipe]
                     (emit-custom-recipe! output recipe "process"))
   :custom-mode (fn [recipe]
                  (emit-custom-recipe! output recipe "mode"))})
