(ns cn.li.neoforge1211.datagen.recipe-provider
  "NeoForge 1.21.1 recipe datagen provider shell."
  (:require [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.mc1211.datagen.recipe-provider-core :as mc-recipe-provider]
            [cn.li.mc1211.datagen.recipe-provider-custom :as recipe-provider-custom]
            [cn.li.platform.datagen.recipe-provider-core :as recipe-provider-core])
  (:import [cn.li.mc1211.shim DelegatingRecipeProvider]
           [java.util.concurrent CompletableFuture]
           [net.minecraft.core HolderLookup$Provider]
           [net.minecraft.data PackOutput]
           [net.minecraft.data.recipes RecipeOutput]
           [net.neoforged.neoforge.common.data ExistingFileHelper]))

(def ^:private recipe-deps
  {:build-vanilla! mc-recipe-provider/build-recipes!
   :load-recipes recipe-core/load-recipes
   :custom-emitters recipe-provider-custom/custom-emitters
   :emit-recipes! recipe-core/emit-recipes!
   :log-label "recipe-provider"})

(defn create
  "Build DelegatingRecipeProvider. lookup-provider is required on 1.21.1."
  ([^PackOutput pack-output ^ExistingFileHelper _exfile-helper]
   (throw (IllegalArgumentException.
           "recipe-provider/create requires HolderLookup.Provider future (1.21.1)")))
  ([^PackOutput pack-output
    ^CompletableFuture lookup-provider
    ^ExistingFileHelper _exfile-helper]
   (DelegatingRecipeProvider.
    pack-output
    ^CompletableFuture lookup-provider
    (fn [_this ^RecipeOutput writer]
      (recipe-provider-core/generate-recipes! writer recipe-deps)))))
