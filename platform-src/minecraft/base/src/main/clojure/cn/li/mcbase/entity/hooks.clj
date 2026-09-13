(ns cn.li.mcbase.entity.hooks
  "Shared scripted entity hook registration (prefix must be installed first)."
  (:require [cn.li.mcbase.entity.hook-registry-core :as hook-core]))

(defn register-all-hooks!
  []
  (hook-core/register-all-scripted-hooks!))
