(ns cn.li.mc262.entity.hooks
  "Versioned entry: install hook class package prefix then register shared hooks."
  (:require [cn.li.mcbase.entity.hook-registry-core :as hook-core]
            [cn.li.mcbase.entity.hooks :as shared-hooks]))

(defn register-all-hooks!
  []
  (hook-core/install-hook-class-prefix! "cn.li.mc262")
  (shared-hooks/register-all-hooks!))
