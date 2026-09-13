(ns cn.li.fabric1201.datagen.recipe-provider
  "Fabric 1.20.1 recipe datagen provider shell."
  (:require [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.mc1201.datagen.recipe-provider-core :as mc-recipe-provider]
            [cn.li.mc1201.datagen.recipe-provider-custom :as recipe-provider-custom]
            [cn.li.platform.datagen.recipe-provider-core :as recipe-provider-core])
  (:import [cn.li.mc1201.shim DelegatingRecipeProvider]
           [java.util.function Consumer]
           [net.minecraft.data PackOutput]))

(def ^:private recipe-deps
  {:build-vanilla! mc-recipe-provider/build-recipes!
   :load-recipes recipe-core/load-recipes
   :custom-emitters recipe-provider-custom/custom-emitters
   :emit-recipes! recipe-core/emit-recipes!
   :log-label "fabric-recipe-provider"})

(defn create-provider
  [^PackOutput output]
  (DelegatingRecipeProvider.
    output
    (fn [_this ^Consumer writer]
      (recipe-provider-core/generate-recipes! writer recipe-deps))))
