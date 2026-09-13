(ns cn.li.ac.block.datagen.registry
  "Register machine domain recipes into shared datagen metadata.

  Machine recipes (ImagFusor, MetalFormer) are stored in per-machine
  Clojure data structures.  During datagen they are collected here,
  tagged with their recipe type (:imag-fusor / :metal-former), and
  appended to the recipe list so that recipe_core emit-recipes! can
  generate JSON datapack files for them.

  Tag-based outputs (e.g. {:tag \"forge:ingots/tin\"}) are filtered out:
  they resolve to different concrete items depending on installed mods,
  so they cannot be represented as static JSON recipe files.  These
  recipes remain available at runtime via the Clojure recipe store.

  Must be called AFTER ability-datagen/register-datagen-metadata!
  (which calls set-recipes! and replaces the list)."
  (:require [cn.li.ac.block.imag-fusor.recipes :as imag-fusor.recipes]
            [cn.li.ac.block.metal-former.recipes :as metal-former.recipes]
            [cn.li.mcmod.datagen.metadata :as metadata]))

(defn- tag-machine-recipes
  "Attach :type to machine recipe maps so emit-recipes! can dispatch on them."
  [type-key recipes]
  (mapv #(assoc % :type type-key) recipes))

(defn- datagen-compatible?
  "True when a recipe output references a concrete :item (not a :tag).
  Tag-based outputs resolve dynamically at runtime and cannot be
  serialized as static JSON."
  [recipe]
  (boolean (:item (:output recipe))))

(defn register-datagen-metadata!
  "Collect ImagFusor & MetalFormer recipes and register them for datagen."
  []
  (let [existing (metadata/get-recipes)
        if-recipes (->> (imag-fusor.recipes/recipes-snapshot)
                        (filter datagen-compatible?)
                        (tag-machine-recipes :custom-process))
        mf-recipes (->> (metal-former.recipes/recipes-snapshot)
                        (filter datagen-compatible?)
                        (tag-machine-recipes :custom-mode))]
    (metadata/set-recipes! (concat existing if-recipes mf-recipes)))
  nil)
