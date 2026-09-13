(ns cn.li.neoforge1211.datagen.setup
  "NeoForge 1.21.1 DataGenerator Event Handler
   
   Registers all data generators for the mod.
   Triggered during setup phase when running:
     ./gradlew :platform:runData -PplatformTarget=<target-id>"
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.neoforge1211.datagen.provider-factory :as provider-factory]
            [cn.li.mcbase.datagen.provider-registration :as provider-registration]
            [cn.li.mc1211.datagen.setup-common :as setup-common]
            [cn.li.platform.target :as target])
  (:import [net.neoforged.neoforge.data.event GatherDataEvent]))

(def ^:private providers
  [{:group :blockstate
    :id :blockstate
    :label "BlockState"
    :summary-label "blockstate"
    :factory :blockstate}
   {:group :block-loot
    :id :block-loot
    :label "Block Loot"
    :summary-label "block-loot"
    :factory :block-loot}
   {:group :block-tags
    :id :block-tags
    :label "Block Tags"
    :summary-label "block-tags"
    :factory :block-tags}
   {:group :item-model
    :id :item-model
    :label "Item Model"
    :summary-label "item-model"
    :factory :item-model}
   {:group :lang
    :id :lang
    :label "Lang"
    :summary-label "lang"
    :factory :lang}
   {:group :recipe
    :id :recipe
    :label "Recipe"
    :summary-label "recipe"
    :factory :recipe}
   {:group :advancement
    :id :advancement
    :label "Advancement"
    :summary-label "advancement"
    :factory :advancement}
   {:group :worldgen
    :id :worldgen
    :label "WorldGen"
    :summary-label "worldgen"
    :factory :worldgen}])

;; ============================================================================
;; EventBusSubscriber Configuration
;; ============================================================================

;; This metadata will be processed by Clojure's gen-class to configure the class
;; equivalent to: @Mod.EventBusSubscriber(modid = AcademyCraft1211.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)

;; ============================================================================
;; DataGenerator Event Handler
;; ============================================================================

(defn -gatherData
  "Event handler for GatherDataEvent
   
   Called by Forge when DataGenerator is ready to accept providers.
   
   Args:
     event: GatherDataEvent containing generator and file helper"
  [^GatherDataEvent event]
  (let [generator (.getGenerator event)
        exfile-helper (.getExistingFileHelper event)
        lookup-provider (.getLookupProvider event)]
    ;; Ensure discovered content registries are loaded before providers query metadata.
    (setup-common/ensure-content-loaded!)

    (provider-registration/register-providers!
      {:mod-id modid/mod-id
       :target-label (:id (target/current-target!))
       :providers providers
       :register-provider! (fn [provider]
                             (provider-factory/add-provider!
                              generator lookup-provider exfile-helper provider))})))

(defn static-gather-data
  "Static entry point used by Java annotation wrapper."
  [^GatherDataEvent event]
  (try
    (println (str "[" modid/mod-id "] Gathering data generators..."))
    (-gatherData event)
    (println (str "[" modid/mod-id "] DataGenerator event processed!"))
    (catch Exception e
      (println (str "Error handling GatherDataEvent: " e))
      (.printStackTrace e))))
