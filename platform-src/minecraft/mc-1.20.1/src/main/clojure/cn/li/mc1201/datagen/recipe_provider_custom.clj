(ns cn.li.mc1201.datagen.recipe-provider-custom
  "Custom content recipe emission for datagen.

  Serializers are resolved via ContentRecipe install bindings from the loader."
  (:require [cn.li.mcbase.datagen.recipe-core :as recipe-core]
            [cn.li.platform.neutral.config :as modid])
  (:import [cn.li.mc1201.recipe ContentRecipe]
           [cn.li.mc1201.shim DelegatingFinishedRecipe]
           [com.google.gson JsonObject JsonPrimitive JsonArray JsonElement]
           [java.util.function Consumer]
           [net.minecraft.data.recipes FinishedRecipe]
           [net.minecraft.resources ResourceLocation]
           [net.minecraft.world.item.crafting RecipeSerializer]))

(defn- clj-map->json-object
  "Recursively convert a Clojure map/vector to a Gson JsonObject/JsonArray."
  [data]
  (cond
    (map? data)
    (let [obj (JsonObject.)]
      (doseq [[k v] data]
        (.add obj (name k) ^JsonElement (clj-map->json-object v)))
      obj)
    (vector? data)
    (let [arr (JsonArray.)]
      (doseq [v data]
        (.add arr ^JsonElement (clj-map->json-object v)))
      arr)
    (string? data)  (JsonPrimitive. ^String data)
    (integer? data)  (JsonPrimitive. ^Number data)
    (float? data)    (JsonPrimitive. ^Number data)
    (double? data)   (JsonPrimitive. ^Number data)
    (boolean? data)  (JsonPrimitive. ^Boolean data)
    (number? data)   (JsonPrimitive. ^Number data)
    :else (JsonPrimitive. (str data))))

(defn- emit-custom-recipe!
  "Emit a custom recipe as a FinishedRecipe JSON.
  Implements serializeRecipe() directly — Clojure proxy does not inherit
  the default method from FinishedRecipe."
  [^Consumer writer recipe ^RecipeSerializer serializer]
  (let [json-obj (clj-map->json-object (recipe-core/recipe-json recipe))
        ^String mod-id modid/mod-id
        ^String recipe-id (recipe-core/normalize-recipe-id (:id recipe))
        ^ResourceLocation id (ResourceLocation. mod-id recipe-id)
        finished (DelegatingFinishedRecipe.
                   (fn [] id)
                   (fn [] serializer)
                   (fn [] json-obj)
                   (fn [] nil)
                   (fn [] nil))]
    (.accept writer finished)))

(defn custom-emitters
  "Return emitter map for custom recipe types keyed by recipe type keyword.
  Each emitter is a 1-arg fn [recipe]; the writer is captured in a closure
  to match emit-recipes!'s calling convention."
  [^Consumer writer]
  {:custom-process (fn [recipe]
                     (emit-custom-recipe! writer recipe
                       (ContentRecipe/contentProcessSerializer)))
   :custom-mode (fn [recipe]
                  (emit-custom-recipe! writer recipe
                    (ContentRecipe/contentModeSerializer)))})
