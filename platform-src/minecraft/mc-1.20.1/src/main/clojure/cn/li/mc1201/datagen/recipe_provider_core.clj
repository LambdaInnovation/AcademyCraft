(ns cn.li.mc1201.datagen.recipe-provider-core
  "Shared Minecraft recipe builder emission used by loader datagen shells."
  (:require [cn.li.mc1201.datagen.resource-location :as rl]
            [cn.li.mc1201.datagen.metadata-resolver :as metadata-resolver]
            [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.mcbase.datagen.recipe-patterns :as recipe-patterns]
            [cn.li.platform.neutral.config :as modid])
  (:import [java.util.function Consumer]
           [net.minecraft.advancements CriterionTriggerInstance]
           [net.minecraft.advancements.critereon InventoryChangeTrigger$TriggerInstance]
           [net.minecraft.data.recipes RecipeBuilder RecipeCategory
            ShapedRecipeBuilder ShapelessRecipeBuilder SimpleCookingRecipeBuilder]
           [net.minecraft.resources ResourceLocation]
           [net.minecraft.world.item.crafting Ingredient]
           [net.minecraft.world.level ItemLike]))

(defn- define-key!
  [^ShapedRecipeBuilder builder k spec]
  (let [ch (if (char? k) k (first (str k)))
        ^Character key-char (Character/valueOf (char ch))
        ^Ingredient ingredient (metadata-resolver/ingredient-from-spec spec rl/parse-resource-location)]
    (.define builder key-char ingredient)))

(defn- criterion-for-item
  ^CriterionTriggerInstance
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
      (.unlockedBy builder ^String unlock-name ^CriterionTriggerInstance criterion-instance)))
  builder)

(defn- emit-shaped!
  [^Consumer writer recipe]
  (let [metadata (recipe-patterns/shaped-recipe-metadata recipe)
        result-item (metadata-resolver/resolve-item (get-in metadata [:result :item]) rl/parse-resource-location)
        result-count (:count (:result metadata))
        ^ShapedRecipeBuilder builder
        (ShapedRecipeBuilder/shaped RecipeCategory/MISC result-item result-count)]
    (doseq [^String row (:pattern metadata)]
      (.pattern builder row))
    (doseq [[k spec] (:key metadata)]
      (define-key! builder k spec))
    (add-unlock-to-builder! builder recipe)
    (.save builder writer (ResourceLocation. modid/mod-id (str (:id recipe))))))

(defn- emit-shapeless!
  [^Consumer writer recipe]
  (let [metadata (recipe-patterns/shapeless-recipe-metadata recipe)
        result-item (metadata-resolver/resolve-item (get-in metadata [:result :item]) rl/parse-resource-location)
        result-count (:count (:result metadata))
        ^ShapelessRecipeBuilder builder
        (ShapelessRecipeBuilder/shapeless RecipeCategory/MISC result-item result-count)]
    (doseq [ingredient-spec (:ingredients metadata)]
      (.requires builder ^Ingredient (metadata-resolver/ingredient-from-spec ingredient-spec rl/parse-resource-location)))
    (add-unlock-to-builder! builder recipe)
    (.save builder writer (ResourceLocation. modid/mod-id (str (:id recipe))))))

(defn- emit-smelting!
  [^Consumer writer recipe]
  (let [metadata (recipe-patterns/cooking-recipe-metadata recipe)
        result-item (metadata-resolver/resolve-item (get-in metadata [:result :item]) rl/parse-resource-location)
        ingredient (metadata-resolver/ingredient-from-spec (:ingredient metadata) rl/parse-resource-location)
        experience (:experience metadata)
        cooking-time (:cooking-time metadata)
        ^SimpleCookingRecipeBuilder builder
        (SimpleCookingRecipeBuilder/smelting
         ingredient
         RecipeCategory/MISC
         result-item
         experience
         cooking-time)]
    (add-unlock-to-builder! builder recipe)
    (.save builder writer (ResourceLocation. modid/mod-id (str (:id recipe))))))

(defn build-recipes!
  "Emit all supported vanilla recipes through Minecraft recipe builders.
  Returns the emitted count. Content-specific recipe types are supplied by
  loader/content components through the provider registry.
  are handled by recipe-provider-custom (serializer bindings via ContentRecipe)."
  [^Consumer writer]
  (let [recipes (recipe-core/load-recipes)]
    (recipe-core/emit-recipes!
     recipes
     {:shaped (fn [recipe] (emit-shaped! writer recipe))
      :shapeless (fn [recipe] (emit-shapeless! writer recipe))
      :smelting (fn [recipe] (emit-smelting! writer recipe))})))
