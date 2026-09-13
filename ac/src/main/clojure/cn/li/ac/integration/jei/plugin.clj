(ns cn.li.ac.integration.jei.plugin
  "JEI integration plugin for AcademyCraft.

  This namespace provides the entry point for JEI (Just Enough Items) integration.
  JEI is a recipe viewing mod that displays crafting recipes in-game.

  Architecture:
  - This namespace (ac/integration/jei/plugin.clj) - Platform-neutral API
  - ac/integration/jei/categories.clj - Recipe category metadata
  - Forge loader JEI implementation
  - Forge loader @JEIPlugin annotation wrapper

  The JEI plugin is automatically discovered by JEI via the @JEIPlugin annotation
  on JEIPluginWrapper.java, which delegates to the Clojure implementation."
  (:require [cn.li.ac.integration.jei.categories :as categories]
            [cn.li.mcmod.runtime.install :as install]))

(defn get-all-categories
  "Get all JEI recipe categories for AcademyCraft machines.

  Returns a sequence of category metadata maps, each containing:
  - :id - Category ID (e.g., '<modid>:imag_fusor')
  - :title-key - Translation key for category title
  - :block-id - Block ID that performs these recipes
  - :background - Background texture info
  - :input-slots - Input slot positions
  - :output-slots - Output slot positions
  - :recipe-loader - Function to load recipes"
  []
  categories/all-categories)

(defn get-category
  "Get a specific category by ID."
  [category-id]
  (categories/get-category-by-id category-id))

(defn get-recipes
  "Get all recipes for a specific category."
  [category]
  (categories/get-recipes-for-category category))

(defn format-recipe
  "Format an AC recipe for JEI display."
  [recipe]
  (categories/format-recipe-for-jei recipe))

;; JEI integration status
(def ^:private jei-available?
  false)

(defn jei-loaded?
  "Check if JEI is loaded and available."
  []
  jei-available?)

(defn mark-jei-loaded!
  "Mark JEI as loaded. Called by platform implementation."
  []
  (install/install-root! #'jei-available? true))
