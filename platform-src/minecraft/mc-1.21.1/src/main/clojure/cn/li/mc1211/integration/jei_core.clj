(ns cn.li.mc1211.integration.jei-core
  "JEI integration core - platform-agnostic recipe registration logic.

  This namespace provides ~75% of JEI integration that can be shared between
  Forge and Fabric. Platform-specific parts (plugin registration) remain
  in loader-specific platform layers."
  (:require [cn.li.platform.neutral.integration-runtime :as integration-hooks]
            [cn.li.mcmod.util.log :as log]
            [clojure.string :as str])
  (:import [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.resources ResourceLocation]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.world.item Item ItemStack]))

;; ============================================================================
;; Item Parsing (Platform-Agnostic)
;; ============================================================================

(defn parse-item-id
  "Parse item ID string to ItemStack.
  Format: 'modid:item_name' or 'modid:item_name#count'
  
  This uses standard Minecraft APIs that are identical between Forge and Fabric.
  
  Args:
    item-id: String item identifier
  
  Returns:
    ItemStack or nil if not found"
  ^ItemStack [^String item-id]
  (try
    (let [[id-part count-str] (str/split item-id #"#")
          count (if count-str (Integer/parseInt count-str) 1)
          res-loc (ResourceLocations/parse id-part)
          ^Item item (.get BuiltInRegistries/ITEM res-loc)]
      (when item
        (ItemStack. item (int count))))
    (catch Exception e
      (log/warn (str "Failed to parse item ID: " item-id " - " (ex-message e)))
      nil)))

;; ============================================================================
;; Recipe Category Creation (Platform-Agnostic)
;; ============================================================================

(defn create-recipe-category-spec
  "Create a recipe category specification from metadata.
  
  This returns the category spec; platform-specific code (Forge/Fabric)
  implements IRecipeCategory or equivalent.
  
  Args:
    category-meta: Category metadata from integration-runtime
  
  Returns:
    Map with {:id, :title-key, :background, :input-slots, :output-slots, :block-id}"
  [category-meta]
  (let [{:keys [id title-key background input-slots output-slots block-id]} category-meta]
    (when (and id title-key background)
      {:id id
       :title-key title-key
       :background background
       :input-slots input-slots
       :output-slots output-slots
       :block-id block-id
       :icon-item-stack (when block-id (parse-item-id block-id))})))

;; ============================================================================
;; Recipe Registration Helpers (Platform-Agnostic)
;; ============================================================================

(defn get-all-categories
  "Get all JEI recipe categories with validation.
  
  Returns:
    Vector of category metadata maps"
  []
  (try
    (->> (integration-hooks/jei-get-all-categories)
         (filter identity)
         vec)
    (catch Exception e
      (log/stacktrace "Failed to get JEI categories:" e)
      [])))

(defn get-recipes-for-category
  "Get all recipes for a specific category with validation.
  
  Args:
    category-meta: Category metadata
  
  Returns:
    Vector of recipe maps"
  [category-meta]
  (try
    (let [recipes (integration-hooks/jei-get-recipes category-meta)
          formatted (mapv integration-hooks/jei-format-recipe recipes)]
      (vec formatted))
    (catch Exception e
      (log/stacktrace (str "Failed to get recipes for category " (:id category-meta)) e)
      [])))

;; ============================================================================
;; JEI Registration Pipeline (Platform-Agnostic)
;; ============================================================================

(defn build-registration-data
  "Build complete registration data for all categories and recipes.
  
  This aggregates all categories and recipes into a single data structure
  that platform-specific code can use to register with JEI.
  
  Returns:
    Map with {:categories, :recipes-by-category, :catalysts}"
  []
  (try
    (let [categories (get-all-categories)
          recipes-by-category (into {}
                                    (map (fn [cat]
                                           [(:id cat) (get-recipes-for-category cat)])
                                         categories))
          catalysts (into {}
                          (map (fn [cat]
                                 [(:id cat) {:item-id (:block-id cat)
                                           :item-stack (parse-item-id (:block-id cat))}])
                               categories))]
      {:categories categories
       :recipes-by-category recipes-by-category
       :catalysts catalysts})
    (catch Exception e
      (log/stacktrace "Failed to build JEI registration data:" e)
      {:categories [] :recipes-by-category {} :catalysts {}})))

(defn validate-category-for-registration
  "Validate that a category has required metadata for registration.
  
  Args:
    category-meta: Category metadata
  
  Returns:
    Boolean - true if valid, false otherwise"
  [category-meta]
  (try
    (let [{:keys [id title-key background block-id]} category-meta]
      (and (string? id)
           (string? title-key)
           (map? background)
           (string? block-id)))
    (catch Exception _
      false)))
