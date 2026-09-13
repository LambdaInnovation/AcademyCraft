(ns cn.li.mcbase.datagen.worldgen-provider-core
  "Shared worldgen DataGen provider logic. Builds configured_feature,
  placed_feature, and platform-specific biome_modifier data maps.

  Does NOT do file I/O — returns data so platform providers can write
  via DataProvider/saveStable (Forge) or the Fabric equivalent.

  Delegates ore/liquid descriptor storage to the platform-neutral
  cn.li.mcmod.worldgen registry. Content modules call mcmod.worldgen
  functions directly; this namespace reads them at datagen time."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.worldgen :as mcmod-worldgen]))

;; ============================================================================
;; Content-owned worldgen registries (delegated to mcmod)
;; ============================================================================

(defn register-worldgen-ore!
  "Register a worldgen ore descriptor. Delegates to mcmod platform-neutral registry."
  [descriptor]
  (mcmod-worldgen/register-worldgen-ore! descriptor))

(defn register-worldgen-liquid!
  "Register a worldgen liquid pool descriptor. Delegates to mcmod platform-neutral registry."
  [descriptor]
  (mcmod-worldgen/register-worldgen-liquid! descriptor))

(defn reset-worldgen-registries-for-test!
  "Reset worldgen registries. For test use only."
  []
  (mcmod-worldgen/reset-worldgen-ores-for-test!)
  (mcmod-worldgen/reset-worldgen-liquids-for-test!))

;; ============================================================================
;; JSON data builders (plain Clojure maps)
;; ============================================================================

(defn- ore-configured-feature-data
  [{:keys [id size]}]
  {"type" "minecraft:ore"
   "config" {"size" size
             "discard_chance_on_air_exposure" 0.0
             "targets" [{"target" {"predicate_type" "minecraft:tag_match"
                                   "tag" "minecraft:stone_ore_replaceables"}
                         "state" {"Name" (str modid/mod-id ":" id)}}
                        {"target" {"predicate_type" "minecraft:tag_match"
                                   "tag" "minecraft:deepslate_ore_replaceables"}
                         "state" {"Name" (str modid/mod-id ":" id)}}]}})

(defn- ore-placed-feature-data
  [{:keys [id count]}]
  {"feature" (str modid/mod-id ":" id)
   "placement" [{"type" "minecraft:count" "count" count}
                {"type" "minecraft:in_square"}
                {"type" "minecraft:height_range"
                 "height" {"type" "minecraft:uniform"
                           "min_inclusive" {"absolute" -64}
                           "max_inclusive" {"absolute" 60}}}
                {"type" "minecraft:biome"}]})

(defn- pool-configured-feature-data
  [_def]
  {"type" (str modid/mod-id ":configurable_pool")
   "config" {}})

(defn- pool-placed-feature-data
  [{:keys [id rarity min-y max-y]}]
  {"feature" (str modid/mod-id ":" id)
   "placement" [{"type" "minecraft:rarity_filter" "chance" rarity}
                {"type" "minecraft:in_square"}
                {"type" "minecraft:height_range"
                 "height" {"type" "minecraft:uniform"
                           "min_inclusive" {"absolute" min-y}
                           "max_inclusive" {"absolute" max-y}}}
                {"type" "minecraft:biome"}]})

(defn- forge-biome-modifier-data
  [feature-id step]
  {"type" "forge:add_features"
   "biomes" "#minecraft:is_overworld"
   "features" (str modid/mod-id ":" feature-id)
   "step" step})

;; ============================================================================
;; File-def builder — returns a seq of {:path [segments...] :data map}
;; Platform providers write these via DataProvider/saveStable.
;; ============================================================================

(defn build-worldgen-file-defs
  "Returns a vector of file definitions for worldgen DataGen.
  Each def is {:path [\"dir\" \"subdir\" \"file.json\"] :data {clojure-map}}.
  Path segments are relative to data/<modid>/.

  Reads ores and liquids from the mcmod platform-neutral registry (populated
  by content during datagen init). Descriptors with :enabled? false are skipped.

  Options:
    :platform — :forge (includes biome_modifier) or :fabric (no biome_modifier)"
  [& {:keys [platform]}]
  (let [is-forge? (= platform :forge)
        file-defs (atom [])]
    (doseq [ore (filter #(get % :enabled?) (mcmod-worldgen/list-worldgen-ores))]
      (let [id (:id ore)]
        (swap! file-defs conj
               {:path ["worldgen" "configured_feature" (str id ".json")]
                :data (ore-configured-feature-data ore)}
               {:path ["worldgen" "placed_feature" (str id ".json")]
                :data (ore-placed-feature-data ore)})
        (when is-forge?
          (swap! file-defs conj
                 {:path ["forge" "biome_modifier" (str "add_" id ".json")]
                  :data (forge-biome-modifier-data id "underground_ores")}))))
    (doseq [liq (filter #(get % :enabled?) (mcmod-worldgen/list-worldgen-liquids))]
      (let [id (:id liq)]
        (swap! file-defs conj
               {:path ["worldgen" "configured_feature" (str id ".json")]
                :data (pool-configured-feature-data liq)}
               {:path ["worldgen" "placed_feature" (str id ".json")]
                :data (pool-placed-feature-data liq)})
        (when is-forge?
          (swap! file-defs conj
                 {:path ["forge" "biome_modifier" (str "add_" id ".json")]
                  :data (forge-biome-modifier-data id "underground_decoration")}))))
    @file-defs))
