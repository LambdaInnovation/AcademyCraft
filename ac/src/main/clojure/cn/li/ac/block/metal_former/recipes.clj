(ns cn.li.ac.block.metal-former.recipes
  "Metal Former recipe system (AcademyCraft parity port)."
  (:require [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.platform.item :as pitem]))

(def mode-order
  [:plate :incise :etch :refine])

(defn normalize-mode
  [mode]
  (let [s (-> (cond
                (keyword? mode) (name mode)
                (string? mode) mode
                :else "plate")
              str/lower-case)]
    (case s
      "plate" :plate
      "incise" :incise
      "etch" :etch
      "refine" :refine
      :plate)))

(defn mode->string
  [mode]
  (name (normalize-mode mode)))

(defn mode->icon-texture
  [mode]
  (modid/asset-path "textures" (str "guis/icons/icon_former_" (mode->string mode) ".png")))

(defn item-id-from-stack
  "Best-effort item id extraction from ItemStack."
  [stack]
  (when stack
    (let [item-obj (try (pitem/object stack) (catch Exception _ nil))
          reg-id (when item-obj
                   (try (pitem/registry-name item-obj)
                        (catch Exception _ nil)))
          desc-id (when item-obj
                    (try (pitem/description-id item-obj)
                         (catch Exception _ nil)))]
      (or reg-id
          (when (string? desc-id)
            (let [tail (last (str/split desc-id #"\\."))]
              (when (and (string? tail) (not (str/blank? tail)))
                (str modid/MOD-ID ":" tail))))))))

(defn- stack-empty?
  [stack]
  (or (nil? stack)
      (try (boolean (pitem/empty? stack))
           (catch Exception _ false))))

(defn- stack-count
  [stack]
  (if (stack-empty? stack)
    0
    (try (int (pitem/stack-count stack))
         (catch Exception _ 0))))

(defn- normalize-item-id
  [item-id]
  (when (string? item-id)
    (if (str/includes? item-id ":")
      item-id
      (str modid/MOD-ID ":" item-id))))

(defn- stack-matches-item?
  "Check if stack matches item-spec.
  Supports both :item (exact ID) and :tag (1.20.1 item tag) matching.
  Only one approach is used — tags are the 1.20.1 recommended way for ore inputs."
  [stack item-spec]
  (and (not (stack-empty? stack))
       item-spec
       (let [min-count (int (or (:count item-spec) 1))]
         (and (>= (stack-count stack) min-count)
              (or (when-let [tag (:tag item-spec)]
                    (pitem/in-item-tag? stack tag))
                  (let [expected-id (normalize-item-id (:item item-spec))
                        actual-id (item-id-from-stack stack)]
                    (and expected-id
                         actual-id
                         (= expected-id actual-id))))))))

(defn- recipe-output-stack
  [recipe]
  (let [output (:output recipe)
        count (int (or (:count output) 1))]
    (when (pos? count)
      (or (when-let [tag (:tag output)]
            (pitem/stack-from-item-tag tag count))
          (when-let [item-id (normalize-item-id (:item output))]
            (pitem/stack-by-id item-id count))))))

(defn- default-recipes
  []
  (let [m modid/MOD-ID]
    [;; Restored AcademyCraft material chain.
     {:id "mf_incise_imag_silicon_ingot"
      :mode "incise"
      :input {:item (str m ":imag_silicon_ingot") :count 1}
      :output {:item (str m ":wafer") :count 2}}
     {:id "mf_incise_wafer"
      :mode "incise"
      :input {:item (str m ":wafer") :count 1}
      :output {:item (str m ":imag_silicon_piece") :count 4}}
     {:id "mf_incise_reinforced_iron_plate"
      :mode "incise"
      :input {:item (str m ":reinforced_iron_plate") :count 1}
      :output {:item (str m ":needle") :count 6}}
     {:id "mf_etch_data_chip"
      :mode "etch"
      :input {:item (str m ":data_chip") :count 1}
      :output {:item (str m ":calc_chip") :count 1}}
     {:id "mf_etch_wafer"
      :mode "etch"
      :input {:item (str m ":wafer") :count 1}
      :output {:item (str m ":silbarn") :count 1}}
     {:id "mf_incise_rail"
      :mode "incise"
      :input {:item "minecraft:rail" :count 1}
      :output {:item (str m ":needle") :count 2}}
     {:id "mf_plate_reinforced_iron_plate"
      :mode "plate"
      :input {:item (str m ":reinforced_iron_plate") :count 2}
      :output {:item (str m ":coin") :count 3}}
     {:id "mf_plate_iron"
      :mode "plate"
      :input {:item "minecraft:iron_ingot" :count 1}
      :output {:item (str m ":reinforced_iron_plate") :count 1}}
     {:id "mf_plate_constraint"
      :mode "plate"
      :input {:item (str m ":constraint_ingot") :count 1}
      :output {:item (str m ":constraint_plate") :count 1}}

     {:id "mf_refine_imagsil_ore"
      :mode "refine"
      :input {:item (str m ":imagsil_ore") :count 1}
      :output {:item (str m ":imag_silicon_ingot") :count 4}}
     {:id "mf_refine_constraint_metal"
      :mode "refine"
      :input {:item (str m ":constraint_metal") :count 1}
      :output {:item (str m ":constraint_ingot") :count 2}}
     {:id "mf_refine_reso_ore"
      :mode "refine"
      :input {:item (str m ":reso_ore") :count 1}
      :output {:item (str m ":reso_crystal") :count 3}}
     {:id "mf_refine_crystal_ore"
      :mode "refine"
      :input {:item (str m ":crystal_ore") :count 1}
      :output {:item (str m ":crystal_low") :count 4}}

     ;; Vanilla ore refine (1.20.1 tag-based — single recipe per ore type, covers all variants).
     {:id "mf_refine_copper_ores"
      :mode "refine"
      :input {:tag "minecraft:copper_ores" :count 1}
      :output {:item "minecraft:copper_ingot" :count 2}}
     {:id "mf_refine_iron_ores"
      :mode "refine"
      :input {:tag "minecraft:iron_ores" :count 1}
      :output {:item "minecraft:iron_ingot" :count 2}}
     {:id "mf_refine_gold_ores"
      :mode "refine"
      :input {:tag "minecraft:gold_ores" :count 1}
      :output {:item "minecraft:gold_ingot" :count 2}}
     {:id "mf_refine_emerald_ores"
      :mode "refine"
      :input {:tag "minecraft:emerald_ores" :count 1}
      :output {:item "minecraft:emerald" :count 2}}
     {:id "mf_refine_diamond_ores"
      :mode "refine"
      :input {:tag "minecraft:diamond_ores" :count 1}
      :output {:item "minecraft:diamond" :count 2}}
     {:id "mf_refine_redstone_ores"
      :mode "refine"
      :input {:tag "minecraft:redstone_ores" :count 1}
      :output {:item "minecraft:redstone_block" :count 1}}
     {:id "mf_refine_lapis_ores"
      :mode "refine"
      :input {:tag "minecraft:lapis_ores" :count 1}
      :output {:item "minecraft:lapis_lazuli" :count 12}}
     {:id "mf_refine_coal_ores"
      :mode "refine"
      :input {:tag "minecraft:coal_ores" :count 1}
      :output {:item "minecraft:coal" :count 2}}
     ;; Nether quartz has no vanilla ores tag — keep as item-based recipe.
     {:id "mf_refine_quartz_ore"
      :mode "refine"
      :input {:item "minecraft:nether_quartz_ore" :count 1}
      :output {:item "minecraft:quartz" :count 2}}
     ;; Mod-compat refine recipes via forge tags (mirrors original OreDictionary presets).
     {:id "mf_refine_tin_ores"
      :mode "refine"
      :input {:tag "forge:ores/tin" :count 1}
      :output {:tag "forge:ingots/tin" :count 2}}
     {:id "mf_refine_silver_ores"
      :mode "refine"
      :input {:tag "forge:ores/silver" :count 1}
      :output {:tag "forge:ingots/silver" :count 2}}
     {:id "mf_refine_lead_ores"
      :mode "refine"
      :input {:tag "forge:ores/lead" :count 1}
      :output {:tag "forge:ingots/lead" :count 2}}
     {:id "mf_refine_aluminum_ores"
      :mode "refine"
      :input {:tag "forge:ores/aluminum" :count 1}
      :output {:tag "forge:ingots/aluminum" :count 2}}
     {:id "mf_refine_nickel_ores"
      :mode "refine"
      :input {:tag "forge:ores/nickel" :count 1}
      :output {:tag "forge:ingots/nickel" :count 2}}
     {:id "mf_refine_platinum_ores"
      :mode "refine"
      :input {:tag "forge:ores/platinum" :count 1}
      :output {:tag "forge:ingots/platinum" :count 2}}
     {:id "mf_refine_iridium_ores"
      :mode "refine"
      :input {:tag "forge:ores/iridium" :count 1}
      :output {:tag "forge:ingots/iridium" :count 2}}
     {:id "mf_refine_mithril_ores"
      :mode "refine"
      :input {:tag "forge:ores/mithril" :count 1}
      :output {:tag "forge:ingots/mithril" :count 2}}]))

(def ^:private recipe-store-lock
  (Object.))

(def ^:private ^:dynamic *recipes*
  (vec (default-recipes)))

(defn- validate-recipe!
  [recipe]
  (when-not (map? recipe)
    (throw (ex-info "Metal Former recipe must be a map" {:recipe recipe})))
  (when-not (and (string? (:id recipe)) (not (str/blank? (:id recipe))))
    (throw (ex-info "Metal Former recipe id must be a non-empty string" {:recipe recipe})))
  recipe)

(defn- assert-no-conflicting-duplicates!
  [recipe-list]
  (doseq [[recipe-id entries] (group-by #(get % :id) recipe-list)
          :when (> (count entries) 1)]
    (when (> (count (distinct entries)) 1)
      (throw (ex-info "Conflicting Metal Former recipe id"
                      {:id recipe-id
                       :recipes entries}))))
  recipe-list)

(defn recipes-snapshot
  []
  (vec (var-get #'*recipes*)))

(defn replace-recipes!
  [recipe-list]
  (let [validated (->> recipe-list
                       (map validate-recipe!)
                       assert-no-conflicting-duplicates!
                       distinct
                       vec)]
    (locking recipe-store-lock
      (alter-var-root #'*recipes* (constantly validated))))
  nil)

(defn register-recipe!
  [recipe]
  (let [recipe (validate-recipe! recipe)]
    (locking recipe-store-lock
      (let [current (var-get #'*recipes*)
            next (if-let [existing (first (filter #(= (:id %) (:id recipe)) current))]
                   (if (= existing recipe)
                     current
                     (throw (ex-info "Conflicting Metal Former recipe id"
                                     {:id (:id recipe)
                                      :existing existing
                                      :new recipe})))
                   (conj (vec current) recipe))]
        (alter-var-root #'*recipes* (constantly next)))))
  nil)

(defn reset-recipes-for-test!
  []
  (locking recipe-store-lock
    (alter-var-root #'*recipes* (constantly (vec (default-recipes)))))
  nil)

(defn get-recipe-by-id
  [recipe-id]
  (first (filter #(= (:id %) recipe-id) (recipes-snapshot))))

(defn find-recipe
  "Find recipe by input stack and machine mode."
  [input-item mode]
  (let [mk (normalize-mode mode)]
    (first
      (filter
        (fn [recipe]
          (and (= mk (normalize-mode (:mode recipe)))
               (stack-matches-item? input-item (:input recipe))))
        (recipes-snapshot)))))

(defn is-valid-input-item?
  "Check if an item stack matches any Metal Former recipe input, regardless of mode.
  Mirrors SlotMFItem.isItemValid() in the original AcademyCraft."
  [stack]
  (boolean
    (some (fn [recipe]
            (stack-matches-item? stack (:input recipe)))
          (recipes-snapshot))))

(defn can-accept-input?
  [recipe input-item mode]
  (and recipe
       (= (normalize-mode (:mode recipe)) (normalize-mode mode))
       (stack-matches-item? input-item (:input recipe))))

(defn can-output?
  [recipe output-slot-item]
  (let [output-spec (:output recipe)
        result-stack (recipe-output-stack recipe)]
    (cond
      (nil? result-stack) false
      (stack-empty? output-slot-item) true
      :else
      (let [items-compatible?
            (or (when-let [tag (:tag output-spec)]
                  ;; Tag-based output: check if existing item also belongs to the same tag
                  (pitem/in-item-tag? output-slot-item tag))
                (try (pitem/same? output-slot-item result-stack)
                     (catch Exception _ false)))]
        (and items-compatible?
             (<= (+ (stack-count output-slot-item) (stack-count result-stack))
                 (int (or (try (pitem/max-stack-size output-slot-item)
                               (catch Exception _ 64))
                          64))))))))

(defn build-output-stack
  [recipe]
  (recipe-output-stack recipe))

(defn can-form?
  "True when this exact recipe can continue crafting in current mode/state."
  [recipe input-item output-item mode]
  (and (can-accept-input? recipe input-item mode)
       (can-output? recipe output-item)))
