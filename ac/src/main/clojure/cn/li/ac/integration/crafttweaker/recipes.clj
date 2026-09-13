(ns cn.li.ac.integration.crafttweaker.recipes
  "CraftTweaker recipe modification API for AcademyCraft machines.

  This namespace provides functions to add/remove recipes for Imag Fusor
  and Metal Former machines via CraftTweaker scripts."
  (:require [cn.li.ac.block.imag-fusor.recipes :as fusor-recipes]
            [cn.li.ac.block.metal-former.recipes :as former-recipes]
            [cn.li.mcmod.util.log :as log]
            [clojure.string :as str]))

;; Recipe validation

(defn- valid-item-spec?
  "Check if an item specification is valid.
  Format: {:item 'modid:item_name' :count N}"
  [item-spec]
  (and (map? item-spec)
       (string? (:item item-spec))
       (or (nil? (:count item-spec))
           (pos-int? (:count item-spec)))))

(defn- valid-fusor-recipe?
  "Validate an Imag Fusor recipe structure."
  [recipe]
  (and (map? recipe)
       (string? (:id recipe))
       (valid-item-spec? (:input recipe))
       (valid-item-spec? (:output recipe))
       (number? (:energy recipe))
       (pos? (:energy recipe))))

(defn- valid-former-recipe?
  "Validate a Metal Former recipe structure."
  [recipe]
  (and (map? recipe)
       (string? (:id recipe))
       (valid-item-spec? (:input recipe))
       (valid-item-spec? (:output recipe))
       (contains? #{"etch" "incise" "plate"} (:mode recipe))
       (number? (:energy recipe))
       (pos? (:energy recipe))))

;; Imag Fusor recipe management

(defn add-fusor-recipe!
  "Add a custom Imag Fusor recipe.

  Args:
    input - Input item spec {:item 'modid:item' :count N}
    output - Output item spec {:item 'modid:item' :count N}
    energy - Energy cost in IF

  Returns:
    true if recipe was added, false if validation failed"
  [input output energy]
  (try
    (let [recipe-id (str "crafttweaker_fusor_" (hash [input output energy]))
          recipe {:id recipe-id
                  :input input
                  :output output
                  :energy (double energy)
                  :time 200}]
      (if (valid-fusor-recipe? recipe)
        (do
          (fusor-recipes/register-recipe! recipe)
          (log/info (str "Added Imag Fusor recipe: " (:item input) " -> " (:item output)))
          true)
        (do
          (log/error (str "Invalid Imag Fusor recipe: " recipe))
          false)))
    (catch Exception e
      (log/stacktrace "Failed to add Imag Fusor recipe" e)
      false)))

(defn remove-fusor-recipe!
  "Remove an Imag Fusor recipe by output item.

  Args:
    output-item - Output item ID string (e.g., 'minecraft:diamond')

  Returns:
    Number of recipes removed"
  [output-item]
  (try
    (let [current (fusor-recipes/recipes-snapshot)
          before-count (count current)
          filtered (remove #(= (get-in % [:output :item]) output-item)
                           current)]
      (fusor-recipes/replace-recipes! filtered)
      (let [removed (- before-count (count filtered))]
        (when (pos? removed)
          (log/info (str "Removed " removed " Imag Fusor recipe(s) for " output-item)))
        removed))
    (catch Exception e
      (log/stacktrace "Failed to remove Imag Fusor recipe" e)
      0)))

;; Metal Former recipe management

(defn add-former-recipe!
  "Add a custom Metal Former recipe.

  Args:
    input - Input item spec {:item 'modid:item' :count N}
    output - Output item spec {:item 'modid:item' :count N}
    mode - Mode string: 'etch', 'incise', or 'plate'
    energy - Energy cost in IF

  Returns:
    true if recipe was added, false if validation failed"
  [input output mode energy]
  (try
    (let [recipe-id (str "crafttweaker_former_" (hash [input output mode energy]))
          recipe {:id recipe-id
                  :input input
                  :output output
                  :mode mode
                  :energy (double energy)
                  :time 200}]
      (if (valid-former-recipe? recipe)
        (do
          (former-recipes/register-recipe! recipe)
          (log/info (str "Added Metal Former recipe (" mode "): " (:item input) " -> " (:item output)))
          true)
        (do
          (log/error (str "Invalid Metal Former recipe: " recipe))
          false)))
    (catch Exception e
      (log/stacktrace "Failed to add Metal Former recipe" e)
      false)))

(defn remove-former-recipe!
  "Remove a Metal Former recipe by output item and mode.

  Args:
    output-item - Output item ID string (e.g., 'minecraft:iron_ingot')
    mode - Mode string: 'etch', 'incise', or 'plate' (optional, removes all modes if nil)

  Returns:
    Number of recipes removed"
  [output-item & [mode]]
  (try
    (let [current (former-recipes/recipes-snapshot)
          before-count (count current)
          filtered (remove (fn [recipe]
                            (and (= (get-in recipe [:output :item]) output-item)
                                 (or (nil? mode) (= (:mode recipe) mode))))
                          current)]
      (former-recipes/replace-recipes! filtered)
      (let [removed (- before-count (count filtered))]
        (when (pos? removed)
          (log/info (str "Removed " removed " Metal Former recipe(s) for " output-item
                        (when mode (str " (mode: " mode ")")))))
        removed))
    (catch Exception e
      (log/stacktrace "Failed to remove Metal Former recipe" e)
      0)))

;; Recipe query functions

(defn get-fusor-recipes
  "Get all Imag Fusor recipes (including custom ones)."
  []
  (fusor-recipes/recipes-snapshot))

(defn get-former-recipes
  "Get all Metal Former recipes (including custom ones)."
  []
  (former-recipes/recipes-snapshot))

(defn clear-custom-recipes!
  "Clear all custom recipes added via CraftTweaker.
  This only removes recipes with IDs starting with 'crafttweaker_'."
  []
  (let [fusor-current (fusor-recipes/recipes-snapshot)
        former-current (former-recipes/recipes-snapshot)
        fusor-before (count fusor-current)
        former-before (count former-current)

        fusor-filtered (remove #(str/starts-with? (:id %) "crafttweaker_")
                               fusor-current)
        former-filtered (remove #(str/starts-with? (:id %) "crafttweaker_")
                                former-current)]

    (fusor-recipes/replace-recipes! fusor-filtered)
    (former-recipes/replace-recipes! former-filtered)

    (let [fusor-removed (- fusor-before (count fusor-filtered))
          former-removed (- former-before (count former-filtered))]
      (log/info (str "Cleared " fusor-removed " Imag Fusor and "
                    former-removed " Metal Former custom recipes"))
      {:fusor fusor-removed :former former-removed})))
