(ns cn.li.fabric262.datagen.recipe-provider
  "Fabric 26.2 recipe datagen provider shell."
  (:require [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.mc262.datagen.recipe-provider-core :as mc-recipe-provider]
            [cn.li.mc262.datagen.recipe-provider-custom :as recipe-provider-custom]
            [cn.li.mcmod.util.log :as log]
            [cn.li.platform.datagen.recipe-provider-core :as recipe-provider-core])
  (:import [cn.li.mc262.shim DelegatingRecipeProvider DelegatingRecipeProvider$Runner]
           [java.util.concurrent CompletableFuture]
           [net.minecraft.data PackOutput]))

(def ^:private recipe-deps
  {:build-vanilla! mc-recipe-provider/build-recipes!
   :load-recipes recipe-core/load-recipes
   :custom-emitters recipe-provider-custom/custom-emitters
   :emit-recipes! recipe-core/emit-recipes!
   :log-label "fabric-recipe-provider"})

(defn- build-fn []
  (fn [^DelegatingRecipeProvider provider]
    (let [items (.itemLookup provider)
          writer (.recipeOutput provider)]
      (try
        (recipe-provider-core/generate-recipes!
          writer
          {:build-vanilla! (fn [_] (mc-recipe-provider/build-recipes! items writer))
           :load-recipes recipe-core/load-recipes
           :custom-emitters (fn [_] (recipe-provider-custom/custom-emitters writer items))
           :emit-recipes! recipe-core/emit-recipes!
           :log-label "fabric-26.2/recipe-provider"})
        (catch Throwable t
          (log/warn "Recipe datagen failed:" (ex-message t))
          (throw t))))))

(defn create-provider
  [^PackOutput output ^CompletableFuture registries]
  (DelegatingRecipeProvider$Runner. output registries (build-fn)))
