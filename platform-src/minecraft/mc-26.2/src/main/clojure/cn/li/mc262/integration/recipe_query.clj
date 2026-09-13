(ns cn.li.mc262.integration.recipe-query
  "CLIENT-ONLY: Dynamic recipe query for tutorial preview system.

  26.2: full RecipeManager lives on the integrated server. Dedicated multiplayer
  clients only get RecipeAccess property sets — queries fall back to nil there."
  (:require [clojure.string :as str]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mc262.runtime.registry :as registry])
  (:import [cn.li.mc262.recipe ContentRecipe]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.client Minecraft]
           [net.minecraft.core Holder]
           [net.minecraft.world.item Item ItemStack]
           [net.minecraft.world.item.crafting Ingredient Recipe RecipeHolder RecipeManager
            RecipeMap RecipeType]
           [net.minecraft.world.item.crafting.display RecipeDisplay]
           [net.minecraft.world.item.crafting.display SlotDisplayContext]
           [net.minecraft.world.level Level]))

(defn item-id->stack
  "Resolve an ItemStack from a runtime item-id string."
  [^String item-id]
  (try
    (let [parts (str/split item-id #":" 2)
          rl (if (= 2 (count parts))
               (ResourceLocations/of (first parts) (second parts))
               (ResourceLocations/parse item-id))
          ^Item item (.getValue (registry/builtin "ITEM") rl)]
      (when item
        (ItemStack. item)))
    (catch Exception e
      (log/warn "item-id->stack failed for" item-id ":" (ex-message e))
      nil)))

(defn- stack->item-id
  "Get runtime item-id string from an ItemStack."
  [^ItemStack stack]
  (when (and stack (not (.isEmpty stack)))
    (try
      (let [^Item item (.getItem stack)
            rl (.getKey (registry/builtin "ITEM") item)]
        (when rl (str rl)))
      (catch Exception e
        (log/warn "stack->item-id failed:" (ex-message e))
        nil))))

(defn- client-recipe-manager
  "Prefer integrated-server RecipeManager (full RecipeMap). Returns nil on remote clients."
  ^RecipeManager
  []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [server (.getSingleplayerServer mc)]
      (.getRecipeManager server))))

(defn- recipe-output-stack
  "Best-effort output ItemStack for a 26.2 Recipe (no getResultItem)."
  ^ItemStack
  [^Recipe recipe ^Level level]
  (cond
    (instance? ContentRecipe recipe)
    (.create (.getOutput ^ContentRecipe recipe))

    :else
    (try
      (let [ctx (SlotDisplayContext/fromLevel level)
            displays (.display recipe)]
        (when-let [^RecipeDisplay disp (first displays)]
          (let [^ItemStack stack (.resolveForFirstStack (.result disp) ctx)]
            (when (and stack (not (.isEmpty stack)))
              stack))))
      (catch Exception _
        nil))))

(defn- ingredient->item-id
  [^Ingredient ing]
  (when (and ing (not (.isEmpty ing)))
    (when-let [^Holder holder (first (iterator-seq (.iterator (.items ing))))]
      (when-let [rl (.getKey (registry/builtin "ITEM") (.value holder))]
        (str rl)))))

(defn- recipe-ingredients
  [^Recipe recipe]
  (cond
    (instance? ContentRecipe recipe)
    [(.getInput ^ContentRecipe recipe)]

    :else
    (try
      (vec (.ingredients (.placementInfo recipe)))
      (catch Exception _
        []))))

(defn- recipes-for-type
  "Get all recipes of a given RecipeType whose output matches target-id."
  [^RecipeManager rm ^RecipeType rtype target-id ^Level level]
  (try
    (when (and rm rtype)
       (let [holders (filter (fn [^RecipeHolder holder]
                               (= rtype (.getType ^Recipe (.value holder))))
                             (.getRecipes rm))]
        (filter (fn [^RecipeHolder holder]
                  (let [^Recipe recipe (.value holder)
                        ^ItemStack out (recipe-output-stack recipe level)]
                    (= target-id (stack->item-id out))))
                holders)))
    (catch Exception e
      (log/warn "[rq] recipes-for-type failed for" target-id ":" (ex-message e))
      nil)))

(defn find-recipes
  "Find all recipes that produce `target-id`.
  Returns a map: {:crafting [...] :smelting [...] :imag-fusor [...] :metal-former [...]}"
  [^String target-id]
  (try
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [^Level level (.level mc)]
        (when-let [^RecipeManager rm (client-recipe-manager)]
          {:crafting (recipes-for-type rm RecipeType/CRAFTING target-id level)
           :smelting (recipes-for-type rm RecipeType/SMELTING target-id level)
           :imag-fusor (recipes-for-type rm (ContentRecipe/contentProcessType) target-id level)
           :metal-former (recipes-for-type rm (ContentRecipe/contentModeType) target-id level)})))
    (catch Exception e
      (log/debug "[rq] Recipe query failed for" target-id ":" (.getMessage e))
      nil)))

(defn has-recipes?
  "Quick check: does `target-id` have any recipes?"
  [^String target-id]
  (when-let [result (find-recipes target-id)]
    (or (seq (:crafting result)) (seq (:smelting result))
        (seq (:imag-fusor result)) (seq (:metal-former result)))))

(defn- recipe-summary
  [^RecipeHolder holder ^Level level]
  (try
    (let [^Recipe recipe (.value holder)
          ^ItemStack output (recipe-output-stack recipe level)]
      (when output
        {:input (mapv ingredient->item-id (recipe-ingredients recipe))
         :output (stack->item-id output)
         :count (.getCount output)}))
    (catch Exception e
      (log/warn "recipe-summary failed:" (ex-message e))
      nil)))

(defn first-recipe-for
  "Get the first recipe of a given kind for `target-id`.
  Returns {:input [item-id...] :output item-id :count N} or nil."
  [^String target-id recipe-kind]
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [^Level level (.level mc)]
      (when-let [result (find-recipes target-id)]
        (when-let [recipes (get result (case recipe-kind
                                         :smelting :smelting
                                         :imag-fusor :imag-fusor
                                         :metal-former :metal-former
                                         :crafting :crafting
                                         nil))]
          (when-let [^RecipeHolder holder (first (seq recipes))]
            (recipe-summary holder level)))))))

(defn all-recipes-for
  "Get all recipes of a given kind for `target-id`.
  Returns vector of {:input [item-id...] :output item-id :count N} or nil."
  [^String target-id recipe-kind]
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [^Level level (.level mc)]
      (when-let [result (find-recipes target-id)]
        (when-let [recipes (get result (case recipe-kind
                                         :smelting :smelting
                                         :imag-fusor :imag-fusor
                                         :metal-former :metal-former
                                         :crafting :crafting
                                         nil))]
          (seq (keep #(recipe-summary % level) recipes)))))))
