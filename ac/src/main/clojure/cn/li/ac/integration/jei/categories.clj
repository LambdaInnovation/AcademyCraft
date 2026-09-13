(ns cn.li.ac.integration.jei.categories
  "JEI recipe category definitions for AC machines.

  This namespace defines platform-neutral recipe category metadata
  that will be used by the platform-specific JEI implementation."
  (:require [cn.li.ac.block.imag-fusor.recipes :as fusor-recipes]
            [cn.li.ac.block.metal-former.recipes :as former-recipes]
            [cn.li.ac.config.modid :as modid]))

;; Recipe category IDs
(def imag-fusor-category-id (str modid/MOD-ID ":imag_fusor"))
(def metal-former-category-id (str modid/MOD-ID ":metal_former"))

;; Slot positions for recipe display
(defn- slot-pos [x y]
  {:x x :y y})

;; Imag Fusor category metadata
(def imag-fusor-category
  {:id imag-fusor-category-id
   :title-key "tile.ac_imag_fusor.name"
  :block-id (str modid/MOD-ID ":imag_fusor")
  ;; Machine GUI container-type — lets the JEI gui handler resolve which
  ;; category the open screen belongs to.
  :container-type :imag-fusor
  :background {:texture (modid/asset-path "textures" "guis/nei_fusor.png")
                :u 0 :v 0
                :width 120 :height 80}
   :input-slots [(slot-pos 5 36)]
   :output-slots [(slot-pos 93 36)]
   :energy-display {:x 50 :y 10 :width 20 :height 50}
   ;; GUI click area (relative to the gui origin) that opens this category
   ;; in JEI — centered on the output slot.
   :click-area {:x 85 :y 28 :width 24 :height 24}
  :recipe-loader fusor-recipes/recipes-snapshot})

;; Metal Former category metadata
(def metal-former-category
  {:id metal-former-category-id
   :title-key "tile.ac_metal_former.name"
  :block-id (str modid/MOD-ID ":metal_former")
  :container-type :metal-former
  :background {:texture (modid/asset-path "textures" "guis/nei_metalformer.png")
                :u 0 :v 0
                :width 94 :height 57}
   :input-slots [(slot-pos 5 23)]
   :output-slots [(slot-pos 71 23)]
   :energy-display {:x 40 :y 10 :width 14 :height 37}
   :click-area {:x 63 :y 15 :width 24 :height 24}
  :recipe-loader former-recipes/recipes-snapshot})

;; All categories
(def all-categories
  [imag-fusor-category
   metal-former-category])

(defn get-category-by-id
  "Get a category definition by its ID."
  [category-id]
  (first (filter #(= (:id %) category-id) all-categories)))

(defn get-recipes-for-category
  "Get all recipes for a given category."
  [category]
  (when-let [loader (:recipe-loader category)]
    (loader)))

(defn format-recipe-for-jei
  "Format an AC recipe into JEI-compatible structure.

  AC recipe format:
  {:id \"recipe_id\"
   :inputs [{:item \"modid:item_name\" :count 1} ...]
   :output {:item \"modid:item_name\" :count 1}
   :energy 1000.0
   :time 200}

  JEI format:
  {:inputs [item-stack-or-ingredient ...]
   :outputs [item-stack ...]
   :energy 1000.0
   :time 200}"
  [recipe]
  {:inputs (or (:inputs recipe)
               (when-let [input (:input recipe)] [input])
               [])
   :outputs [(or (:output recipe) {})]
   :energy (or (:energy recipe) 0.0)
   :time (or (:time recipe) 0)})
