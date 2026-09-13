(ns cn.li.neoforge262.datagen.recipe-provider
  "NeoForge 26.2 recipe datagen — RecipeProvider.Runner wiring."
  (:require [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.mc262.datagen.recipe-provider-core :as mc-recipe-provider]
            [cn.li.mc262.datagen.recipe-provider-custom :as recipe-provider-custom]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.datagen.recipe-provider-core :as recipe-provider-core])
  (:import [cn.li.mc262.shim DelegatingRecipeProvider DelegatingRecipeProvider$Runner]
           [java.util.concurrent CompletableFuture]
           [net.minecraft.data PackOutput]
           [net.minecraft.data.recipes RecipeOutput]))

(defn- build-fn
  []
  (fn [^DelegatingRecipeProvider provider]
    (let [items (.itemLookup provider)
          ^RecipeOutput writer (.recipeOutput provider)]
      (try
        (recipe-provider-core/generate-recipes!
         writer
         {:build-vanilla! (fn [_w] (mc-recipe-provider/build-recipes! items writer))
          :load-recipes recipe-core/load-recipes
          :custom-emitters (fn [_w] (recipe-provider-custom/custom-emitters writer items))
          :emit-recipes! recipe-core/emit-recipes!
          :log-label "neoforge-26.2/recipe-provider"})
        (catch Throwable t
          (log/warn "Recipe datagen failed:" (ex-message t))
          (.printStackTrace t)
          ;; Do not swallow: an empty/partial recipe set must fail the run.
          (throw t))))))

(defn create
  "Build RecipeProvider.Runner for GatherDataEvent.addProvider / createProvider."
  [^PackOutput pack-output
   ^CompletableFuture lookup-provider]
  (DelegatingRecipeProvider$Runner. pack-output
                                    lookup-provider
                                    (build-fn)))
