(ns cn.li.mcbase.datagen.recipe-patterns
  "Shared recipe generation patterns for datagen.
  
  Provides utilities for recipe metadata transformation, unlock condition
  generation, and common recipe processing logic. Platform-specific recipe
  builder invocations remain in loader-specific implementations."
  (:require [clojure.string])
  (:import [clojure.lang Keyword]
           [java.util.function Function]))

(defn first-item-id
  "Extract first item ID from recipe for unlock condition.
  
  Checks multiple recipe spec formats:
  - Shaped recipes: first item in :key map
  - Shapeless recipes: first item in :ingredients vector
  - Cooking recipes: single item in :ingredient
  - Fallback: item in :result
  
  Args:
    recipe: recipe map
  
  Returns: first found item ID string or nil"
  [recipe]
  (or (some #(get % :item) (vals (:key recipe)))
      (some #(get % :item) (:ingredients recipe))
      (get-in recipe [:ingredient :item])
      (get-in recipe [:result :item])))

(defn unlock-name
  "Generate Minecraft-safe unlock criterion name from item ID.
  
  Transforms item ID format (e.g. 'modid:item_name') to criterion
  identifier format (e.g. 'has_item_name').
  
  Args:
    item-id: item reference string
  
  Returns: criterion identifier string suitable for .unlockedBy()"
  [item-id]
  (let [^String s (str item-id)
        path (or (second (.split s ":" 2))
                 s)]
    (str "has_" (clojure.string/replace path #"[^\w/.-]" "_"))))

(defn criterion-metadata
  "Build criterion metadata for recipe unlock.
  
  Args:
    item-id: item reference string
    resolve-item-fn: function to resolve item ID to ItemLike (for criterion creation)
    criterion-factory-fn: function(item-id) returning CriterionTriggerInstance
  
  Returns: map with :unlock-name and :criterion-instance
  
  Notes:
    - criterion-factory-fn should handle the Minecraft registry lookup and
      InventoryChangeTrigger instantiation, returning a CriterionTriggerInstance
    - Used by platform-specific add-unlock-* builders"
  [item-id criterion-factory-fn]
  (when item-id
    {:unlock-name (unlock-name item-id)
     :criterion-instance (criterion-factory-fn item-id)}))

(defn recipe-result
  "Extract and normalize recipe result specification.
  
  Args:
    recipe: recipe map with :result key
  
  Returns: map with :item and :count (defaulting count to 1)"
  [recipe]
  (let [result-spec (:result recipe)]
    {:item (:item result-spec)
     :count (int (or (:count result-spec) 1))}))

(defn shaped-recipe-metadata
  "Extract metadata for shaped recipe generation.
  
  Args:
    recipe: shaped recipe map with :pattern, :key, :result
  
  Returns: map with normalized recipe metadata for platform builders
  
  Notes:
    - Pattern and key are passed through unchanged
    - Result is normalized with recipe-result helper
    - First item ID extracted for unlock condition"
  [recipe]
  {:pattern (:pattern recipe)
   :key (:key recipe)
   :result (recipe-result recipe)
   :unlock-item-id (first-item-id recipe)})

(defn shapeless-recipe-metadata
  "Extract metadata for shapeless recipe generation.
  
  Args:
    recipe: shapeless recipe map with :ingredients, :result
  
  Returns: map with normalized recipe metadata for platform builders
  
  Notes:
    - Ingredients passed through unchanged
    - Result normalized with recipe-result helper
    - First item ID extracted for unlock condition"
  [recipe]
  {:ingredients (:ingredients recipe)
   :result (recipe-result recipe)
   :unlock-item-id (first-item-id recipe)})

(defn cooking-recipe-metadata
  "Extract metadata for cooking (smelting/smoking/etc) recipe generation.
  
  Args:
    recipe: cooking recipe map with :ingredient, :result, :experience, :cooking-time
  
  Returns: map with normalized recipe metadata for platform builders
  
  Notes:
    - Experience defaults to 0.0, cooking-time defaults to 200 ticks
    - Ingredient passed through unchanged for resolver
    - Used for smelting, blasting, smoking, campfire recipes"
  [recipe]
  {:ingredient (:ingredient recipe)
   :result (recipe-result recipe)
   :experience (float (or (:experience recipe) 0.0))
   :cooking-time (int (or (:cooking-time recipe) 200))
   :unlock-item-id (first-item-id recipe)})

(defn recipe-type-dispatcher
  "Dispatch recipe metadata extraction by recipe type.
  
  Args:
    recipe: recipe map with :type key (:shaped, :shapeless, :smelting, etc.)
  
  Returns: metadata map for the appropriate recipe type
  
  Throws: ex-info for unsupported recipe types"
  [recipe]
  (case (:type recipe)
    :shaped (shaped-recipe-metadata recipe)
    :shapeless (shapeless-recipe-metadata recipe)
    :smelting (cooking-recipe-metadata recipe)
    (throw (ex-info "Unsupported recipe type" {:recipe recipe}))))
