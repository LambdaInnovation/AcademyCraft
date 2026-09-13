(ns cn.li.fabric262.datagen.provider-factory
  "Fabric datagen provider factory adapter.

  This namespace is the Fabric-specific shell between the shared provider
  manifest and FabricDataGenerator Pack APIs."
  (:require [cn.li.fabric262.datagen.item-model-provider :as item-model-provider]
            [cn.li.fabric262.datagen.recipe-provider :as recipe-provider]
            [cn.li.fabric262.datagen.worldgen-provider :as worldgen-provider]
            [cn.li.mc262.datagen.advancement-provider-shell :as advancement-shell]
            [cn.li.mc262.datagen.blockstate-provider-shell :as blockstate-shell]
            [cn.li.mc262.datagen.block-loot-provider-shell :as block-loot]
            [cn.li.mc262.datagen.block-tag-provider-shell :as block-tags]
            [cn.li.mc262.datagen.lang-provider-shell :as lang-shell])
  (:import [net.fabricmc.fabric.api.datagen.v1 FabricDataGenerator$Pack FabricDataGenerator$Pack$Factory FabricDataGenerator$Pack$RegistryDependentFactory]))

(def ^:private blockstate-provider-name
  "Fabric Blockstate Provider")

(defn- create-provider
  [provider output]
  (case (:factory provider)
    ;; Shared shell emits all merged langs; ignore per-entry :language when present.
    :lang (lang-shell/create output)
    :blockstate (blockstate-shell/create-provider output blockstate-provider-name)
    :block-loot (block-loot/create output)
    :block-tags (block-tags/create output)
    :item-model (item-model-provider/create-provider output)
    :advancement (advancement-shell/create output)
    :recipe (throw (ex-info "Recipe providers require Fabric registries" {:provider provider}))
    :worldgen (worldgen-provider/create-provider output)
    (throw (ex-info "Unknown Fabric datagen provider factory"
                    {:provider provider}))))

(defn provider-pack-factory
  "Create a Fabric pack provider factory for one shared manifest entry."
  [provider]
  (reify FabricDataGenerator$Pack$Factory
    (create [_ output]
      (create-provider provider output))))

(defn- recipe-provider-pack-factory [provider]
  (reify FabricDataGenerator$Pack$RegistryDependentFactory
    (create [_ output registries]
      (recipe-provider/create-provider output registries))))

(defn add-provider!
  "Register one shared provider manifest entry with a FabricDataGenerator pack."
  [^FabricDataGenerator$Pack pack provider]
  (if (= :recipe (:factory provider))
    (.addProvider pack ^FabricDataGenerator$Pack$RegistryDependentFactory
                  (recipe-provider-pack-factory provider))
    (let [^FabricDataGenerator$Pack$Factory factory (provider-pack-factory provider)]
      (.addProvider pack factory))))
