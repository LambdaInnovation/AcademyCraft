(ns cn.li.ac.config.modid
  "AC-layer mod-id configuration. Delegates to cn.li.mcmod.ModId (generated
   from gradle.properties). Change mod_id in gradle.properties and rebuild."
  (:require [cn.li.mcmod.config :as mcmod-config]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:const MOD-ID
  "The primary mod identifier used across all resource locations, registries,
   and mod identification. Sourced from cn.li.mcmod.ModId (generated from gradle.properties).
   Change mod_id in gradle.properties and rebuild to update."
  cn.li.mcmod.ModId/ID)

(defn install-modid!
  "Install AC's mod id into the shared mcmod config namespace.

  This is intentionally explicit: requiring this namespace must not mutate
  shared configuration state."
  []
  (install/install-root! #'mcmod-config/mod-id MOD-ID)
  nil)

(def resource-location mcmod-config/resource-location)
(def namespaced-path mcmod-config/namespaced-path)
(def asset-path mcmod-config/asset-path)
