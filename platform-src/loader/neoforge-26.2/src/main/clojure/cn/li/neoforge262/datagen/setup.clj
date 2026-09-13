(ns cn.li.neoforge262.datagen.setup
  "NeoForge 26.2 GatherDataEvent wiring.

  26.x splits GatherDataEvent into Client/Server subclasses. Registering
  providers across both events and writing to the same --output directory
  causes HashCache on the second run to delete the first run's files.
  NeoForge guidance for a single aggregate :platform:runData is therefore:
  put every provider on GatherDataEvent.Client and use clientData() only."
  (:require [cn.li.mc262.datagen.advancement-provider-shell :as adv-shell]
            [cn.li.mc262.datagen.blockstate-provider-shell :as blockstate-shell]
            [cn.li.mc262.datagen.block-loot-provider-shell :as block-loot]
            [cn.li.mc262.datagen.block-tag-provider-shell :as block-tags]
            [cn.li.mc262.datagen.lang-provider-shell :as lang-shell]
            [cn.li.mc262.datagen.setup-common :as setup-common]
            [cn.li.mc262.datagen.worldgen-provider-shell :as worldgen-shell]
            [cn.li.neoforge262.datagen.item-model-provider :as item-model-provider]
            [cn.li.neoforge262.datagen.recipe-provider :as recipe-provider])
  (:import [net.neoforged.neoforge.data.event GatherDataEvent
            GatherDataEvent$Client]))

(def ^:private blockstate-provider-name
  "NeoForge Blockstate Provider")

(defn -gatherData
  "Event handler for GatherDataEvent (Client subclass only)."
  [^GatherDataEvent event]
  (try
    (when (instance? GatherDataEvent$Client event)
      (setup-common/ensure-content-loaded!)
      (let [pack-output (.getPackOutput (.getGenerator event))
            lookup (.getLookupProvider event)]
        (.addProvider event (recipe-provider/create pack-output lookup))
        (.addProvider event (adv-shell/create pack-output))
        (.addProvider event (worldgen-shell/create pack-output :forge))
        (.addProvider event (lang-shell/create pack-output))
        (.addProvider event (blockstate-shell/create-provider pack-output blockstate-provider-name))
        (.addProvider event (block-loot/create pack-output))
        (.addProvider event (block-tags/create pack-output))
        (.addProvider event (item-model-provider/create pack-output))))
    (catch Throwable e
      (println (str "Error handling GatherDataEvent: " e))
      (.printStackTrace e)
      (throw e))))

(defn static-gather-data
  "Static entry point used by Java listener."
  [^GatherDataEvent event]
  (-gatherData event))
