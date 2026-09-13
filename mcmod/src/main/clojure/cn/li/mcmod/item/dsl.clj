(ns cn.li.mcmod.item.dsl
  "Item DSL - Declarative item definition using Clojure macros"
  (:require [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Item Registry - stores all defined items



;; Item specifications
(defrecord ItemSpec [id max-stack-size durability creative-tab rarity
                     food-properties tool-properties armor-properties
                     on-use on-right-click on-finish-using
                     enchantability properties])

;; Creative tabs (version-agnostic)
(def creative-tabs
  {:building-blocks :building-blocks
   :decorations :decorations
   :redstone :redstone
   :transportation :transportation
   :misc :misc
   :food :food
   :tools :tools
   :combat :combat
   :brewing :brewing
   :materials :materials})

;; Rarity levels
(def rarity-levels
  {:common :common
   :uncommon :uncommon
   :rare :rare
   :epic :epic})

;; Tool tiers
(def tool-tiers
  {:wood 0
   :stone 1
   :iron 2
   :diamond 3
   :netherite 4
   :gold 0})

;; Armor materials
(def armor-materials
  {:leather :leather
   :chainmail :chainmail
   :iron :iron
   :gold :gold
   :diamond :diamond
   :netherite :netherite})

;; Default values
(def default-max-stack-size 64)
(def default-creative-tab :misc)

;; Create item specification
(defn create-item-spec
  "Create an item specification from options"
  [item-id options]
  (map->ItemSpec
    {:id item-id
     :max-stack-size (or (:max-stack-size options) default-max-stack-size)
     :durability (:durability options)
     :creative-tab (or (:creative-tab options) default-creative-tab)
     :rarity (or (:rarity options) :common)
     :food-properties (:food-properties options)
     :tool-properties (:tool-properties options)
     :armor-properties (:armor-properties options)
     :on-use (or (:on-use options) (fn [_] nil))
     :on-right-click (or (:on-right-click options) (fn [_] nil))
     :on-finish-using (or (:on-finish-using options) (fn [_] nil))
     :enchantability (or (:enchantability options) 0)
     :properties (or (:properties options) {})}))

;; Validate item specification
(defn validate-item-spec [item-spec]
  (when-not (:id item-spec)
    (throw (ex-info "Item must have an :id" {:spec item-spec})))
  (when-not (string? (:id item-spec))
    (throw (ex-info "Item :id must be a string" {:id (:id item-spec)})))
  (when (and (:durability item-spec) (> (:max-stack-size item-spec) 1))
    (throw (ex-info "Durable items must have max-stack-size of 1"
                    {:id (:id item-spec)
                     :durability (:durability item-spec)
                     :max-stack-size (:max-stack-size item-spec)})))
  true)

;; Register item in registry
(defn register-item! [item-spec]
  (validate-item-spec item-spec)
  (log/debug "Registering item:" (:id item-spec))
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:registry :items (:id item-spec)] item-spec))
  item-spec)

;; Get item from registry
(defn get-item [item-id]
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom [:registry :items item-id])
    nil))

;; List all registered items
(defn list-items []
  (if-let [fw-atom (fw/fw-atom)]
    (keys (get-in @fw-atom [:registry :items]))
    ()))

;; Main macro: defitem
(defmacro defitem
  "Define an item with declarative syntax
  
  Supports two forms:
  1. Map form: (defitem {:id \"item_name\" :max-stack-size 16 ...})
  2. Symbol + options form: (defitem my-item :id \"item_name\" :max-stack-size 16 ...)
  
  Example:
  (defitem my-item
    :id \"my_item\"
    :max-stack-size 16
    :creative-tab :tools
    :durability 500
    :on-use (fn [data] (println \"Used!\")))"
  [item-name & options]
  (if (map? item-name)
    (let [options-map item-name
          item-id (:id options-map)]
      (when-not (string? item-id)
        (throw (ex-info "Map-form defitem requires string :id"
                        {:form options-map})))
      `(register-item!
         (create-item-spec ~item-id ~(dissoc options-map :id))))
    (let [options-map (if (and (= 1 (count options)) (map? (first options)))
                        (first options)
                        (apply hash-map options))
          item-id (or (:id options-map) (name item-name))]
      `(def ~item-name
         (register-item!
           (create-item-spec ~item-id ~(dissoc options-map :id)))))))

;; Helper: food properties
(defn food-properties
  "Create food properties map"
  [nutrition saturation & {:keys [meat fast-to-eat always-edible]
                           :or {meat false fast-to-eat false always-edible false}}]
  {:nutrition nutrition
   :saturation saturation
   :meat meat
   :fast-to-eat fast-to-eat
   :always-edible always-edible})

;; Helper: tool properties
(defn tool-properties
  "Create tool properties map"
  [tier attack-damage attack-speed]
  {:tier tier
   :attack-damage attack-damage
   :attack-speed attack-speed})

;; Helper: armor properties
(defn armor-properties
  "Create armor properties map"
  [material slot protection toughness]
  {:material material
   :slot slot
   :protection protection
   :toughness toughness})

;; Template: basic item
(defn basic-item-template
  "Create a basic item template"
  []
  {:max-stack-size 64
   :creative-tab :misc
   :rarity :common})

;; Template: tool item
(defn tool-template
  "Create a tool item template"
  [tier durability attack-damage attack-speed]
  {:max-stack-size 1
   :creative-tab :tools
   :durability durability
   :tool-properties (tool-properties tier attack-damage attack-speed)
   :enchantability (case tier
                     :wood 15
                     :stone 5
                     :iron 14
                     :diamond 10
                     :netherite 15
                     :gold 22
                     10)})

;; Template: food item
(defn food-template
  "Create a food item template"
  [nutrition saturation]
  {:max-stack-size 64
   :creative-tab :food
   :food-properties (food-properties nutrition saturation)})

;; Template: rare item
(defn rare-item-template
  "Create a rare item template"
  [rarity]
  {:max-stack-size 1
   :creative-tab :misc
   :rarity rarity})

;; Helper: merge templates
(defn merge-templates
  "Merge multiple templates with options"
  [& template-and-options]
  (apply merge template-and-options))

;; Multimethod for version-specific item creation
(def forge-version nil)

(defmulti create-platform-item
  "Create a version-specific item instance"
  (fn [_item-spec] forge-version))

(defmethod create-platform-item :default [item-spec]
  (throw (ex-info "No item implementation for version"
                  {:version forge-version
                   :item-id (:id item-spec)})))

;; Item interaction handlers
(defn handle-use
  "Handle item use"
  [item-spec event-data]
  (when-let [handler (:on-use item-spec)]
    (handler event-data)))

(defn handle-right-click
  "Handle right-click with item"
  [item-spec event-data]
  (when-let [handler (:on-right-click item-spec)]
    (handler event-data)))

(defn handle-finish-using
  "Handle finishing using item (e.g., eating food)"
  [item-spec event-data]
  (when-let [handler (:on-finish-using item-spec)]
    (handler event-data)))

;; Get item properties for platform creation
(defn get-item-properties
  "Get item properties map for platform-specific creation"
  [item-spec]
  {:max-stack-size (:max-stack-size item-spec)
   :durability (:durability item-spec)
   :creative-tab (:creative-tab item-spec)
   :rarity (:rarity item-spec)
   :food-properties (:food-properties item-spec)
   :tool-properties (:tool-properties item-spec)
   :armor-properties (:armor-properties item-spec)
   :enchantability (:enchantability item-spec)})
