(ns cn.li.fabricbase.optional-integrations
  "Loader-neutral optional Fabric mod availability reporting."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [cn.li.fabricbase FabricIntegration]))

(defn init! []
  (log/debug "Fabric optional integrations"
            {:energy (FabricIntegration/isModLoaded "team_reborn_energy")
             :jei (FabricIntegration/isModLoaded "jei")})
  nil)
