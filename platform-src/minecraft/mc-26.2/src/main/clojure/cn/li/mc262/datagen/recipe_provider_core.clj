(ns cn.li.mc262.datagen.recipe-provider-core
  "Shared Minecraft recipe builder emission for 26.2 datagen.

  Requires a HolderGetter<Item> (RecipeProvider.items) because shaped/shapeless
  builders and tag ingredients are HolderGetter-backed. save() takes
  ResourceKey<Recipe<?>>."
  (:require [cn.li.mc262.datagen.resource-location :as rl]
            [cn.li.mc262.datagen.metadata-resolver :as metadata-resolver]
            [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.mcbase.datagen.recipe-patterns :as recipe-patterns]
            [cn.li.platform.neutral.config :as modid])
  (:import [net.minecraft.advancements.triggers Criterion
            InventoryChangeTrigger$TriggerInstance]
           [net.minecraft.core HolderGetter]
           [net.minecraft.core.registries Registries]
           [net.minecraft.data.recipes RecipeBuilder RecipeCategory RecipeOutput
            ShapedRecipeBuilder ShapelessRecipeBuilder SimpleCookingRecipeBuilder]
           [net.minecraft.resources ResourceKey]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.world.item Item]
           [net.minecraft.world.item.crafting CookingBookCategory Ingredient]
           [net.minecraft.world.level ItemLike]))

(defn- recipe-key
  ^ResourceKey
  [recipe-id]
  (ResourceKey/create Registries/RECIPE
                      (ResourceLocations/of modid/mod-id (str recipe-id))))

(defn- define-key!
  [^HolderGetter items ^ShapedRecipeBuilder builder k spec]
  (let [ch (if (char? k) k (first (str k)))
        ^Character key-char (Character/valueOf (char ch))
        ^Ingredient ingredient (metadata-resolver/ingredient-from-spec spec rl/parse-resource-location items)]
    (.define builder key-char ingredient)))

(defn- criterion-for-item
  ^Criterion
  [item-id]
  (let [^ItemLike item (metadata-resolver/resolve-item item-id rl/parse-resource-location)]
    (InventoryChangeTrigger$TriggerInstance/hasItems
     ^"[Lnet.minecraft.world.level.ItemLike;"
     (into-array ItemLike [item]))))

(defn- add-unlock-to-builder!
  [^RecipeBuilder builder recipe]
  (when-let [unlock-item-id (recipe-patterns/first-item-id recipe)]
    (let [{:keys [unlock-name criterion-instance]}
          (recipe-patterns/criterion-metadata unlock-item-id criterion-for-item)]
      (.unlockedBy builder ^String unlock-name ^Criterion criterion-instance)))
  builder)

(defn- emit-shaped!
  [^HolderGetter items ^RecipeOutput writer recipe]
  (let [metadata (recipe-patterns/shaped-recipe-metadata recipe)
        result-item (metadata-resolver/resolve-item (get-in metadata [:result :item]) rl/parse-resource-location)
        result-count (:count (:result metadata))
        ^ShapedRecipeBuilder builder
        (ShapedRecipeBuilder/shaped items RecipeCategory/MISC result-item (int result-count))]
    (doseq [^String row (:pattern metadata)]
      (.pattern builder row))
    (doseq [[k spec] (:key metadata)]
      (define-key! items builder k spec))
    (add-unlock-to-builder! builder recipe)
    (.save builder writer ^ResourceKey (recipe-key (:id recipe)))))

(defn- emit-shapeless!
  [^HolderGetter items ^RecipeOutput writer recipe]
  (let [metadata (recipe-patterns/shapeless-recipe-metadata recipe)
        result-item (metadata-resolver/resolve-item (get-in metadata [:result :item]) rl/parse-resource-location)
        result-count (:count (:result metadata))
        ^ShapelessRecipeBuilder builder
        (ShapelessRecipeBuilder/shapeless items RecipeCategory/MISC result-item (int result-count))]
    (doseq [ingredient-spec (:ingredients metadata)]
      (.requires builder ^Ingredient
                 (metadata-resolver/ingredient-from-spec ingredient-spec rl/parse-resource-location items)))
    (add-unlock-to-builder! builder recipe)
    (.save builder writer ^ResourceKey (recipe-key (:id recipe)))))

(defn- emit-smelting!
  [^HolderGetter items ^RecipeOutput writer recipe]
  (let [metadata (recipe-patterns/cooking-recipe-metadata recipe)
        ^ItemLike result-item (metadata-resolver/resolve-item (get-in metadata [:result :item]) rl/parse-resource-location)
        ^Ingredient ingredient (metadata-resolver/ingredient-from-spec (:ingredient metadata) rl/parse-resource-location items)
        experience (:experience metadata)
        cooking-time (:cooking-time metadata)
        ^SimpleCookingRecipeBuilder builder
        (SimpleCookingRecipeBuilder/smelting
         ingredient
         RecipeCategory/MISC
         CookingBookCategory/MISC
         result-item
         (float experience)
         (int cooking-time))]
    (add-unlock-to-builder! builder recipe)
    (.save builder writer ^ResourceKey (recipe-key (:id recipe)))))

(defn build-recipes!
  "Emit vanilla recipes through Minecraft recipe builders.
  items — HolderGetter<Item> from RecipeProvider; writer — RecipeOutput."
  [^HolderGetter items ^RecipeOutput writer]
  (let [recipes (recipe-core/load-recipes)]
    (recipe-core/emit-recipes!
     recipes
     {:shaped (fn [recipe] (emit-shaped! items writer recipe))
      :shapeless (fn [recipe] (emit-shapeless! items writer recipe))
      :smelting (fn [recipe] (emit-smelting! items writer recipe))})))
