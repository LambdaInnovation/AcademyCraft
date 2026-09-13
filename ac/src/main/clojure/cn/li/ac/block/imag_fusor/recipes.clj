(ns cn.li.ac.block.imag-fusor.recipes
  "Imaginary Fusor recipe system (ported from AcademyCraft MFIF recipes)."
  (:require [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.block.imag-fusor.config :as cfg]
            [cn.li.mcmod.platform.item :as pitem]))

(defn item-id-from-stack
  "Best-effort item id extraction from an ItemStack.
   Returns namespaced id string when available, else nil."
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
  "True when the slot holds nothing: nil or an empty ItemStack (upstream
   ItemStack.isEmpty()). Slots can hold ItemStack.EMPTY rather than nil —
   a nil-only check treated it as a real item and broke output acceptance."
  [stack]
  (or (nil? stack)
      (try (boolean (pitem/empty? stack)) (catch Exception _ false))))

(defn- stack-count
  [stack]
  (if stack
    (try (int (pitem/stack-count stack)) (catch Exception _ 0))
    0))

(defn- normalize-item-id
  [item-id]
  (when (string? item-id)
    (if (str/includes? item-id ":")
      item-id
      (str modid/MOD-ID ":" item-id))))

(defn- stack-matches-item?
  [stack item-spec]
  (and stack
       item-spec
       (let [expected-id (normalize-item-id (:item item-spec))
             actual-id (item-id-from-stack stack)
             min-count (int (or (:count item-spec) 1))]
         (and expected-id
              actual-id
              (= expected-id actual-id)
              (>= (stack-count stack) min-count)))))

(defn- recipe-output-stack
  [recipe]
  (let [output (:output recipe)
        item-id (normalize-item-id (:item output))
        count (int (or (:count output) 1))]
    (when (and item-id (pos? count))
      (pitem/stack-by-id item-id count))))

(defn- default-recipes
  []
  [{:id "if_crystal_low_to_normal"
    :input {:item (str modid/MOD-ID ":crystal_low") :count 1}
    :output {:item (str modid/MOD-ID ":crystal_normal") :count 1}
    :consume-liquid 3000
    :time cfg/craft-time-ticks}
   {:id "if_crystal_normal_to_pure"
    :input {:item (str modid/MOD-ID ":crystal_normal") :count 1}
    :output {:item (str modid/MOD-ID ":crystal_pure") :count 1}
    :consume-liquid 8000
    :time cfg/craft-time-ticks}])

(def ^:private recipe-store-lock
  (Object.))

(def ^:private ^:dynamic *recipes*
  (vec (default-recipes)))

(defn- validate-recipe!
  [recipe]
  (when-not (map? recipe)
    (throw (ex-info "Imaginary Fusor recipe must be a map" {:recipe recipe})))
  (when-not (and (string? (:id recipe)) (not (str/blank? (:id recipe))))
    (throw (ex-info "Imaginary Fusor recipe id must be a non-empty string" {:recipe recipe})))
  recipe)

(defn- assert-no-conflicting-duplicates!
  [recipe-list]
  (doseq [[recipe-id entries] (group-by #(get % :id) recipe-list)
          :when (> (count entries) 1)]
    (when (> (count (distinct entries)) 1)
      (throw (ex-info "Conflicting Imaginary Fusor recipe id"
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
                     (throw (ex-info "Conflicting Imaginary Fusor recipe id"
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

(defn find-recipe
  "Find a recipe matching the crystal input stack."
  [input-item]
  (first (filter #(stack-matches-item? input-item (:input %)) (recipes-snapshot))))

(defn get-recipe-by-id
  "Get a recipe by its ID"
  [recipe-id]
  (first (filter #(= (:id %) recipe-id) (recipes-snapshot))))

(defn can-output?
  "Check if recipe output can be inserted into current output slot stack."
  [recipe output-slot-item]
  (let [result-stack (recipe-output-stack recipe)]
    (cond
      (nil? result-stack) false
      (stack-empty? output-slot-item) true
      :else
      (and (try (pitem/same? output-slot-item result-stack) (catch Exception _ false))
           (<= (+ (stack-count output-slot-item) (stack-count result-stack))
               (int (or (try (pitem/max-stack-size output-slot-item)
                             (catch Exception _ 64))
                        64)))))))

(defn build-output-stack
  "Build the output stack for a completed recipe."
  [recipe]
  (recipe-output-stack recipe))

(defn can-craft?
  "Check whether crafting can proceed this tick.
   Mirrors original checks: item still valid, enough energy+liquid, output accepts result."
  [recipe input-item output-item energy liquid-amount]
  (and recipe
       (stack-matches-item? input-item (:input recipe))
       (>= (double energy) cfg/energy-per-tick)
       (>= (int liquid-amount) (int (:consume-liquid recipe 0)))
       (can-output? recipe output-item)))

;; --- Two-level crafting check (original AcademyCraft parity) ---

(defn can-continue-crafting?
  "Hard-abort guard: return false when crafting must stop and reset progress.
   Mirrors original TileImagFusor.updateWork() hard-fail conditions:
   input type changed, energy/liquid insufficient, output item type mismatch."
  [recipe input-item output-item energy liquid-amount]
  (and recipe
       input-item
       (stack-matches-item? input-item (:input recipe))
       (>= (double energy) cfg/energy-per-tick)
       (>= (int liquid-amount) (int (:consume-liquid recipe 0)))
       (let [result-stack (recipe-output-stack recipe)]
         (or (stack-empty? output-item)
             (and result-stack
                  (try (= (item-id-from-stack output-item)
                          (item-id-from-stack result-stack))
                       (catch Exception _ false)))))))

(defn is-action-blocked?
  "Soft-block check: return true when progress should pause but NOT reset.
   Mirrors original TileImagFusor.isActionBlocked() conditions:
   output slot full or NBT mismatch (same item type but different data)."
  [recipe input-item output-item]
  (or (nil? recipe)
      (let [input-count (stack-count input-item)
            consume-count (int (get-in recipe [:input :count] 1))]
        (< input-count consume-count))
      (when-let [result-stack (recipe-output-stack recipe)]
        (and (not (stack-empty? output-item))
             (try (= (item-id-from-stack output-item)
                     (item-id-from-stack result-stack))
                  (catch Exception _ false))
             (or (not (try (pitem/same? output-item result-stack)
                           (catch Exception _ false)))
                 (let [output-count (stack-count output-item)
                       result-count (stack-count result-stack)
                       max-size (int (or (try (pitem/max-stack-size output-item)
                                              (catch Exception _ 64))
                                         64))]
                   (> (+ output-count result-count) max-size)))))))
