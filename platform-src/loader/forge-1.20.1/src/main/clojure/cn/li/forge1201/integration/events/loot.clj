(ns cn.li.forge1201.integration.events.loot
  "Forge loot-table load event handlers." 
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.registry.metadata :as registry-metadata])
  (:import [cn.li.forge1201.loot LootInjectionHelper]
           [net.minecraftforge.event LootTableLoadEvent]))

(defn handle-loot-table-load
  [^LootTableLoadEvent evt]
  (try
    (let [table-id (str (.getName evt))
          injections (registry-metadata/get-loot-injections-for-table table-id)]
      (when (seq injections)
        (doseq [spec injections]
          (LootInjectionHelper/addItemInjection
            evt
            (:item-id spec)
            (int (or (:weight spec) 1))
            (int (or (:quality spec) 0))
            (float (or (:min-count spec) 1.0))
            (float (or (:max-count spec) 1.0))))))
    (catch Throwable t
      (log/stacktrace "Error handling loot table load event:" t)
      (.printStackTrace t))))
