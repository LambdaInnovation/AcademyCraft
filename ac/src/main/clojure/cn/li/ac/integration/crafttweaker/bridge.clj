(ns cn.li.ac.integration.crafttweaker.bridge
  "CraftTweaker type conversion bridge.

  This namespace provides utilities to convert between CraftTweaker types
  and AcademyCraft internal types. It's platform-neutral and doesn't import
  any CraftTweaker classes directly - the platform layer handles that."
  (:require [cn.li.mcmod.util.log :as log]
            [clojure.string :as str]))

;; Type conversion protocols — map-based
;;
;; Expected map keys:
;; - ICraftTweakerItemStack: :to-item-spec (fn [] -> {:item 'id' :count N})
;; - IACItemSpec: :to-minecraft-stack (fn [] -> ItemStack)

(defn to-item-spec
  "Convert CraftTweaker IItemStack to AC item spec {:item 'id' :count N}."
  [crafttweaker-stack]
  ((:to-item-spec crafttweaker-stack)))

(defn to-minecraft-stack
  "Convert AC item spec to Minecraft ItemStack."
  [ac-item-spec]
  ((:to-minecraft-stack ac-item-spec)))

;; Item spec conversion

(defn parse-item-id
  "Parse an item ID string into namespace and path.

  Args:
    item-id - String like 'minecraft:diamond' or 'ac:imag_silicon'

  Returns:
    {:namespace 'minecraft' :path 'diamond'} or nil if invalid"
  [item-id]
  (when (string? item-id)
    (let [parts (str/split item-id #":" 2)]
      (when (= 2 (count parts))
        {:namespace (first parts)
         :path (second parts)}))))

(defn format-item-id
  "Format namespace and path into an item ID string.

  Args:
    namespace - Mod namespace (e.g., 'minecraft')
    path - Item path (e.g., 'diamond')

  Returns:
    String like 'minecraft:diamond'"
  [namespace path]
  (str namespace ":" path))

(defn normalize-item-spec
  "Normalize an item spec to ensure it has all required fields.

  Args:
    item-spec - Map with :item and optionally :count

  Returns:
    Normalized spec with :item, :count, :namespace, :path"
  [item-spec]
  (when (and (map? item-spec) (:item item-spec))
    (let [parsed (parse-item-id (:item item-spec))
          count (or (:count item-spec) 1)]
      (when parsed
        (assoc item-spec
               :count count
               :namespace (:namespace parsed)
               :path (:path parsed))))))

;; Recipe conversion

(defn crafttweaker-to-fusor-recipe
  "Convert CraftTweaker parameters to Imag Fusor recipe format.

  Args:
    input - Input item spec
    output - Output item spec
    energy - Energy cost

  Returns:
    Recipe map or nil if invalid"
  [input output energy]
  (try
    (let [input-spec (normalize-item-spec input)
          output-spec (normalize-item-spec output)]
      (when (and input-spec output-spec (number? energy) (pos? energy))
        {:input input-spec
         :output output-spec
         :energy (double energy)}))
    (catch Exception e
      (log/stacktrace "Failed to convert CraftTweaker Fusor recipe" e)
      nil)))

(defn crafttweaker-to-former-recipe
  "Convert CraftTweaker parameters to Metal Former recipe format.

  Args:
    input - Input item spec
    output - Output item spec
    mode - Mode string ('etch', 'incise', or 'plate')
    energy - Energy cost

  Returns:
    Recipe map or nil if invalid"
  [input output mode energy]
  (try
    (let [input-spec (normalize-item-spec input)
          output-spec (normalize-item-spec output)]
      (when (and input-spec output-spec
                 (contains? #{"etch" "incise" "plate"} mode)
                 (number? energy) (pos? energy))
        {:input input-spec
         :output output-spec
         :mode mode
         :energy (double energy)}))
    (catch Exception e
      (log/stacktrace "Failed to convert CraftTweaker Former recipe" e)
      nil)))

;; Validation helpers

(defn valid-crafttweaker-input?
  "Validate CraftTweaker input parameters.

  Args:
    input - Input item (should be convertible to item spec)
    output - Output item (should be convertible to item spec)
    energy - Energy cost (should be positive number)

  Returns:
    true if valid, false otherwise"
  [input output energy]
  (and (some? input)
       (some? output)
       (number? energy)
       (pos? energy)))

(defn describe-recipe
  "Create a human-readable description of a recipe for logging.

  Args:
    recipe - Recipe map

  Returns:
    String description"
  [recipe]
  (let [input-item (get-in recipe [:input :item])
        output-item (get-in recipe [:output :item])
        energy (:energy recipe)
        mode (:mode recipe)]
    (str input-item " -> " output-item
         " (energy: " energy " IF"
         (when mode (str ", mode: " mode))
         ")")))
