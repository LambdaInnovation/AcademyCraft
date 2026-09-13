(ns cn.li.fabric1211.setup.lifecycle-init
  "Fabric lifecycle coordinator extracted from mod entry.

  Keeps the loader entry thin and makes phase ordering explicit."
  (:require [cn.li.fabricbase.lifecycle :as shared-lifecycle]))

(defn init-lifecycle!
  [action-map]
  (shared-lifecycle/init! action-map))
