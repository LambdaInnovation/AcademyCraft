(ns cn.li.neoforgebase.integration.events.loot
  "Shared loot-table load handlers. Version loaders install LootInjectionHelper bridge."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.registry.metadata :as registry-metadata])
  (:import [net.neoforged.neoforge.event LootTableLoadEvent]))

(defonce ^:private add-item-injection-atom
  (atom nil))

(defn install-add-item-injection!
  "Install (fn [evt item-id weight quality min max] ...)."
  [f]
  (reset! add-item-injection-atom f)
  f)

(defn handle-loot-table-load
  [^LootTableLoadEvent evt]
  (try
    (let [add! @add-item-injection-atom]
      (when (nil? add!)
        (throw (IllegalStateException. "loot add-item-injection not installed")))
      (let [table-id (str (.getName evt))
            injections (registry-metadata/get-loot-injections-for-table table-id)]
        (when (seq injections)
          (doseq [spec injections]
            (add!
              evt
              (:item-id spec)
              (int (or (:weight spec) 1))
              (int (or (:quality spec) 0))
              (float (or (:min-count spec) 1.0))
              (float (or (:max-count spec) 1.0)))))))
    (catch Throwable t
      (log/stacktrace "Error handling loot table load event:" t)
      (.printStackTrace t))))
