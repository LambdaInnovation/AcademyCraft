(ns cn.li.mcbase.datagen.recipe-core
  "Shared recipe datagen helpers used by Forge/Fabric providers."
  (:require [clojure.string :as str]
            [cn.li.mcbase.datagen.recipe-patterns :as recipe-patterns]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.datagen.metadata :as datagen-metadata]))

(defn load-recipes
  []
  (vec (datagen-metadata/get-recipes)))

(defn normalize-recipe-id
  [id]
  (let [sid (str id)]
    (if (str/includes? sid ":")
      (second (str/split sid #":" 2))
      sid)))

(defn ingredient-json
  [spec]
  (cond
    (:item spec) {:item (str (:item spec))}
    (:tag spec) {:tag (str (:tag spec))}
    :else (throw (ex-info "Invalid ingredient spec" {:spec spec}))))

(defn shaped-json
  [recipe]
  (let [{:keys [pattern key result]} (recipe-patterns/shaped-recipe-metadata recipe)]
    {:type "minecraft:crafting_shaped"
     :pattern (vec pattern)
     :key (into {}
                (for [[k spec] key]
                  [(str (if (char? k) k (first (str k))))
                   (ingredient-json spec)]))
     :result (cond-> {:item (str (:item result))}
               (> (int (:count result)) 1) (assoc :count (int (:count result))))}))

(defn shapeless-json
  [recipe]
  (let [{:keys [ingredients result]} (recipe-patterns/shapeless-recipe-metadata recipe)]
    {:type "minecraft:crafting_shapeless"
     :ingredients (mapv ingredient-json ingredients)
     :result (cond-> {:item (str (:item result))}
               (> (int (:count result)) 1) (assoc :count (int (:count result))))}))

(defn cooking-type
  [t]
  (case t
    :smelting "minecraft:smelting"
    :blasting "minecraft:blasting"
    :smoking "minecraft:smoking"
    :campfire-cooking "minecraft:campfire_cooking"
    (throw (ex-info "Unsupported cooking recipe type" {:type t}))))

(defn cooking-json
  [recipe]
  (let [{:keys [ingredient result experience cooking-time]} (recipe-patterns/cooking-recipe-metadata recipe)]
    {:type (cooking-type (:type recipe))
     :ingredient (ingredient-json ingredient)
     :result (str (:item result))
     :experience (float experience)
     :cookingtime (int cooking-time)}))

(defn- machine-output-json
  "Convert machine recipe output to JSON. Returns {:item ... :count ...} map."
  [output]
  (cond-> {:item (str (:item output))}
    (> (int (:count output 1)) 1)
    (assoc :count (int (:count output 1)))))

(defn process-recipe-json
  [recipe]
  {:type (str modid/mod-id ":content_process")
   :input (ingredient-json (:input recipe))
   :output (machine-output-json (:output recipe))
   :consume_liquid (int (or (:consume-liquid recipe) 0))
   :craft_time (int (or (:time recipe) 200))})

(defn mode-recipe-json
  [recipe]
  {:type (str modid/mod-id ":content_mode")
   :input (ingredient-json (:input recipe))
   :output (machine-output-json (:output recipe))
   :mode (:mode recipe)})

(defn recipe-json
  [recipe]
  (case (:type recipe)
    :shaped (shaped-json recipe)
    :shapeless (shapeless-json recipe)
    (:smelting :blasting :smoking :campfire-cooking) (cooking-json recipe)
    :custom-process (process-recipe-json recipe)
    :custom-mode (mode-recipe-json recipe)
    (throw (ex-info "Unsupported recipe type" {:type (:type recipe) :recipe recipe}))))

(defn emit-recipes!
  "Emit recipes that have a matching emitter.  Recipes whose type has no
  emitter are silently skipped (they belong to a different emission pass,
  e.g. custom types handled by the loader-specific provider).

  Returns the count of recipes that were actually emitted."
  [recipes emitters]
  (let [emitted (atom 0)]
    (doseq [recipe recipes]
      (if-let [emit-fn (get emitters (:type recipe))]
        (try
          (emit-fn recipe)
          (swap! emitted inc)
          (catch Exception e
            (throw (ex-info (str "Failed to emit recipe: " (pr-str (:id recipe)))
                            {:recipe-id (:id recipe)
                             :recipe-type (:type recipe)
                             :result (:result recipe)}
                            e))))
        nil))  ;; skip — handled by custom-emitters pass in loader provider
    @emitted))
