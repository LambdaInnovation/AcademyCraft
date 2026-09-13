(ns cn.li.ac.tutorial.init
  "Tutorial system initialization — registers network handlers, builds
  condition index.  Follows terminal/init.clj pattern."
  (:require [cn.li.ac.registry.hooks :as hooks]
            [cn.li.ac.tutorial.network :as network]
            [cn.li.ac.tutorial.registry :as tut-registry]
            [cn.li.ac.tutorial.conditions :as conds]
            [cn.li.mcmod.util.log :as log]))

(defn init-tutorial!
  "Initialize the tutorial system:
   - Register network handlers
   - Build global condition index for condition-based unlock"
  []
  (log/debug "Initializing tutorial system...")
  (hooks/register-network-handler! network/register-handlers!)
  ;; Build condition index for Phase 5 condition-based unlock
  (conds/ensure-condition-index! (tut-registry/all-tutorials))
  (log/debug "Tutorial system initialized successfully"))
