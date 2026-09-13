(ns cn.li.fabric1201.datagen.setup
  "Fabric 1.20.1 DataGenerator Setup

   Registers all data generators for JSON generation.

   Fabric uses different event system than Forge, so this module
   provides utilities to be called during data generation phase."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.fabric1201.datagen.provider-factory :as provider-factory]
            [cn.li.mcbase.datagen.provider-registration :as provider-registration]
            [cn.li.mc1201.datagen.setup-common :as setup-common]
            [cn.li.platform.target :as target]))

;; One :lang entry — lang-provider-shell emits all merged language files.
(def ^:private providers
  [{:group :lang
    :id :lang
    :label "Lang"
    :summary-label "lang"
    :factory :lang}
   {:group :blockstate
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
   {:group :advancement
    :id :advancement
    :label "Advancement"
    :summary-label "advancement"
    :factory :advancement}
   {:group :recipe
    :id :recipe
    :label "Recipe"
    :summary-label "recipe"
    :factory :recipe}
   {:group :worldgen
    :id :worldgen
    :label "WorldGen"
    :summary-label "worldgen"
    :factory :worldgen}])


(defn register-data-generators!
  "Register all data generators for Fabric

   Call this during data generation phase."
  [generator _exfile-helper]
  (setup-common/ensure-content-loaded!)
  (let [pack (.createPack ^net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator generator)]
    (provider-registration/register-providers!
      {:mod-id modid/mod-id
       :target-label (:id (target/current-target!))
       :providers providers
       :register-provider! (fn [provider]
                             (provider-factory/add-provider! pack provider))})))
