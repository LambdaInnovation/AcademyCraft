(ns cn.li.neoforge1211.setup.lifecycle-listeners
  "Lifecycle and client listener registration for Forge mod event bus." 
  (:require [cn.li.mc1211.entity.hooks :as entity-hooks]))

(defn register-client-hooks!
  []
  (entity-hooks/register-all-hooks!)
  nil)
