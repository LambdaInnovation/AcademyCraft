(ns cn.li.neoforge1211.datagen.provider-factory
  "NeoForge datagen provider factory adapter.

  This namespace is the NeoForge-specific shell between the shared provider manifest
  and NeoForge/Minecraft DataGenerator APIs."
  (:require [cn.li.mc1211.datagen.advancement-provider-shell :as adv]
            [cn.li.neoforge1211.datagen.item-model-provider :as imp]
            [cn.li.mc1211.datagen.lang-provider-shell :as lang]
            [cn.li.neoforge1211.datagen.recipe-provider :as rp]
            [cn.li.mc1211.datagen.worldgen-provider-shell :as worldgen]
            [cn.li.mc1211.datagen.blockstate-provider-shell :as blockstate-shell]
            [cn.li.mc1211.datagen.block-loot-provider-shell :as block-loot]
            [cn.li.mc1211.datagen.block-tag-provider-shell :as block-tags])
  (:import [java.util.concurrent CompletableFuture]
           [net.minecraft.data DataGenerator DataProvider$Factory]
           [net.neoforged.neoforge.common.data ExistingFileHelper]))

(def ^:private blockstate-provider-name
  "NeoForge Blockstate Provider")

(def ^:private provider-factories
  {:blockstate (fn [pack-output _lookup _exfile-helper]
                 (blockstate-shell/create-provider pack-output blockstate-provider-name))
   :block-loot (fn [pack-output _lookup _exfile-helper]
                 (block-loot/create pack-output))
   :block-tags (fn [pack-output _lookup _exfile-helper]
                 (block-tags/create pack-output))
   :item-model (fn [pack-output _lookup exfile-helper]
                 (imp/create pack-output exfile-helper))
   :lang (fn [pack-output _lookup exfile-helper]
           (lang/create pack-output exfile-helper))
   :recipe (fn [pack-output lookup exfile-helper]
             (rp/create pack-output lookup exfile-helper))
   :advancement (fn [pack-output _lookup exfile-helper]
                  (adv/create pack-output exfile-helper))
   :worldgen (fn [pack-output _lookup exfile-helper]
               (worldgen/create pack-output :forge exfile-helper))})

(defn- provider-factory
  [{:keys [factory] :as provider}]
  (or (get provider-factories factory)
      (throw (ex-info "Unknown NeoForge datagen provider factory"
                      {:provider provider
                       :known-factories (sort (keys provider-factories))}))))

(defn add-provider!
  "Register one shared provider manifest entry with NeoForge's DataGenerator."
  [^DataGenerator generator
   ^CompletableFuture lookup-provider
   ^ExistingFileHelper exfile-helper
   provider]
  (let [create-fn (provider-factory provider)]
    (.addProvider generator true
      (reify DataProvider$Factory
        (create [_ pack-output]
          (create-fn pack-output lookup-provider exfile-helper))))))
