(ns cn.li.mcbase.datagen.block-loot-provider-core
  "Version-neutral block loot DataProvider.

  The provider consumes the block DSL metadata and emits modern Minecraft loot
  tables during datagen.  Version shells only select the version-specific loot
  directory and enchantment predicate shape."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcbase.datagen.gson-util :as gson-util])
  (:import [net.minecraft.data CachedOutput DataProvider PackOutput]
           [com.google.gson Gson]
           [java.nio.file Path]
           [java.util.concurrent CompletableFuture]))

(def ^:private ^Gson gson (gson-util/create-pretty-gson))

(defn- namespaced [id]
  (if (re-find #":" (str id))
    (str id)
    (str modid/mod-id ":" id)))

(defn- silk-touch-condition [modern?]
  {:condition "minecraft:match_tool"
   :predicate (if modern?
               {:predicates {"minecraft:enchantments"
                             [{:enchantments "minecraft:silk_touch"
                               :levels {:min 1}}]}}
               {:enchantments [{:enchantment "minecraft:silk_touch"
                                :levels {:min 1}}]})})

(defn- item-entry
  ([item-id] (item-entry item-id nil))
  ([item-id conditions]
   (cond-> {:type "minecraft:item" :name (namespaced item-id)}
     (seq conditions) (assoc :conditions conditions))))

(defn- pool [entry]
  {:rolls 1 :bonus_rolls 0 :entries [entry]})

(defn- self-table [registry-name]
  {:type "minecraft:block"
   :pools [(pool (item-entry registry-name))]})

(defn- ore-table [registry-name {:keys [drop-item min-count max-count]} modern?]
  {:type "minecraft:block"
   :pools [(pool
            {:type "minecraft:alternatives"
             :children [(item-entry registry-name [(silk-touch-condition modern?)])
                        {:type "minecraft:item"
                         :name (namespaced drop-item)
                         :functions [{:function "minecraft:set_count"
                                      :count {:type "minecraft:uniform"
                                              :min (or min-count 1)
                                              :max (or max-count 2)}}
                                     {:function "minecraft:apply_bonus"
                                      :enchantment "minecraft:fortune"
                                       :formula "minecraft:ore_drops"}]}]})]})

(defn- table-for-block [block-id modern?]
  (let [spec (registry-metadata/get-block-spec block-id)
        registry-name (registry-metadata/get-block-registry-name block-id)
        has-item? (registry-metadata/should-create-block-item? block-id)
        loot (:loot spec)]
    (cond
      (not has-item?) {:type "minecraft:block" :pools []}
      (= :ore (:type loot)) (ore-table registry-name loot modern?)
      :else (self-table registry-name))))

(defn create
  "Create a block loot DataProvider.

  Options:
  - :loot-path: `loot_tables` for 1.20/1.21 or `loot_table` for 26.2
  - :modern?: use the 1.21+ enchantment predicate shape"
  [^PackOutput pack-output {:keys [loot-path modern?]
                            :or {loot-path "loot_tables" modern? true}}]
  (let [root (.getOutputFolder pack-output)
        base (.resolve ^Path root (str "data/" modid/mod-id "/" loot-path "/blocks"))]
    (reify DataProvider
      (^CompletableFuture run [_ ^CachedOutput cached]
        (let [writes (for [block-id (registry-metadata/get-all-block-ids)
                           :let [registry-name (registry-metadata/get-block-registry-name block-id)
                                 payload (table-for-block block-id modern?)
                                 target (.resolve base (str registry-name ".json"))]]
                       (DataProvider/saveStable cached
                         (.toJsonTree gson (gson-util/normalize-json payload))
                         target))]
          (CompletableFuture/allOf (into-array CompletableFuture writes))))
      (getName [_] (str modid/mod-id " Block Loot Provider")))))
