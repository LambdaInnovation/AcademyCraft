(ns cn.li.fabric1201.integration.events.loot
  "Fabric loot-table handlers extracted from monolithic events namespace."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.registry.metadata :as registry-metadata])
  (:import [cn.li.fabric1201.loot FabricLootInjectionHelper]))

(defn handle-loot-table-modify
  [id table-builder]
  (try
    (let [table-id (str id)
          injections (registry-metadata/get-loot-injections-for-table table-id)]
      (when (seq injections)
        (doseq [spec injections]
          (FabricLootInjectionHelper/addItemInjection
            table-builder
            (:item-id spec)
            (int (or (:weight spec) 1))
            (int (or (:quality spec) 0))
            (float (or (:min-count spec) 1.0))
            (float (or (:max-count spec) 1.0))))))
    (catch Throwable t
      (log/stacktrace "Error handling loot table modify event:" t)
      (.printStackTrace t))))
