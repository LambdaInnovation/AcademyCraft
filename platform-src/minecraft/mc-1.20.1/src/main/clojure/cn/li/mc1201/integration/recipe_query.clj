(ns cn.li.mc1201.integration.recipe-query
  "CLIENT-ONLY: Dynamic recipe query for tutorial preview system.

  Queries Minecraft's RecipeManager for crafting/furnace/content recipes
  matching a given output item."
  (:require [clojure.string :as str]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mc1201.recipe ContentRecipe]
           [net.minecraft.client Minecraft]
           [net.minecraft.world.item.crafting RecipeType RecipeManager Recipe Ingredient]
           [net.minecraft.world.item ItemStack Item]
           [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.resources ResourceLocation]))

(defn item-id->stack
  "Resolve an ItemStack from a runtime item-id string."
  [^String item-id]
  (try
    (let [parts (str/split item-id #":" 2)
          rl (if (= 2 (count parts))
               (ResourceLocation. (first parts) (second parts))
               (ResourceLocation. item-id))
          ^Item item (.get BuiltInRegistries/ITEM rl)]
      (when item (ItemStack. item)))
    (catch Exception e
      (log/warn "item-id->stack failed for" item-id ":" (ex-message e))
      nil)))

(defn- stack->item-id
  "Get runtime item-id string from an ItemStack."
  [^ItemStack stack]
  (when stack
    (try
      (let [^Item item (.getItem stack)
            rl (.getKey BuiltInRegistries/ITEM item)]
        (when rl (str (.getNamespace rl) ":" (.getPath rl))))
      (catch Exception e
        (log/warn "stack->item-id failed:" (ex-message e))
        nil))))

(defn- recipes-for-type
  "Get all recipes of a given RecipeType whose output matches target-id."
  [^RecipeManager rm ^RecipeType rtype target-id]
  (try
    (when rtype
      (let [recipes (.getAllRecipesFor rm rtype)]
        (filter (fn [^Recipe recipe]
                  (= target-id (stack->item-id (.getResultItem recipe nil))))
                recipes)))
    (catch Exception e
      (log/warn "[rq] recipes-for-type failed for" target-id ":" (ex-message e))
      nil)))

(defn find-recipes
  "Find all recipes that produce `target-id` (e.g. \"academy:constrained_ore\").
  Returns a map: {:crafting [...] :smelting [...] :imag-fusor [...] :metal-former [...]}"
  [^String target-id]
  (try
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [level (.level mc)]
        (let [rm (.getRecipeManager level)]
          {:crafting (recipes-for-type rm RecipeType/CRAFTING target-id)
           :smelting (recipes-for-type rm RecipeType/SMELTING target-id)
           :imag-fusor (recipes-for-type rm (ContentRecipe/contentProcessType) target-id)
           :metal-former (recipes-for-type rm (ContentRecipe/contentModeType) target-id)})))
    (catch Exception e
      (log/debug "[rq] Recipe query failed for" target-id ":" (.getMessage e))
      nil)))

(defn has-recipes?
  "Quick check: does `target-id` have any recipes?"
  [^String target-id]
  (when-let [result (find-recipes target-id)]
    (or (seq (:crafting result)) (seq (:smelting result))
        (seq (:imag-fusor result)) (seq (:metal-former result)))))

(defn first-recipe-for
  "Get the first recipe of a given kind for `target-id`.
  Returns {:input [item-id...] :output item-id :count N} or nil."
  [^String target-id recipe-kind]
  (when-let [result (find-recipes target-id)]
    (when-let [recipes (get result (case recipe-kind
                                     :smelting :smelting
                                     :imag-fusor :imag-fusor
                                     :metal-former :metal-former
                                     :crafting :crafting
                                     nil))]
      (when-let [^Recipe recipe (first (seq recipes))]
        (try
          (let [^ItemStack output (.getResultItem recipe nil)]
            {:input (mapv (fn [^Ingredient ing]
                            (when-let [stacks (.getItems ing)]
                              (when-let [^ItemStack s (first (seq stacks))]
                                (stack->item-id s))))
                          (.getIngredients recipe))
             :output (stack->item-id output)
             :count (.getCount output)})
          (catch Exception e
            (log/warn "first-recipe-for failed for" target-id recipe-kind ":" (ex-message e))
            nil))))))

(defn all-recipes-for
  "Get all recipes of a given kind for `target-id`.
  Returns vector of {:input [item-id...] :output item-id :count N} or nil."
  [^String target-id recipe-kind]
  (when-let [result (find-recipes target-id)]
    (when-let [recipes (get result (case recipe-kind
                                     :smelting :smelting
                                     :imag-fusor :imag-fusor
                                     :metal-former :metal-former
                                     :crafting :crafting
                                     nil))]
      (seq (keep (fn [^Recipe recipe]
                   (try
                     (let [^ItemStack output (.getResultItem recipe nil)]
                       {:input (mapv (fn [^Ingredient ing]
                                       (when-let [stacks (.getItems ing)]
                                         (when-let [^ItemStack s (first (seq stacks))]
                                           (stack->item-id s))))
                                     (.getIngredients recipe))
                        :output (stack->item-id output)
                        :count (.getCount output)})
                     (catch Exception _
                       nil)))
                 recipes)))))
