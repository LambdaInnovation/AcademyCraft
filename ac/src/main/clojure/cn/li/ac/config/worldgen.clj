(ns cn.li.ac.config.worldgen
  "World generation toggles owned by AC (ores, phase liquid)."
  (:require [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.util.log :as log]))

(def default-values
  {:gen-ores true
   :gen-phase-liquid true})

(def descriptors
  [{:key :gen-ores
    :path "worldgen.gen-ores"
    :section :worldgen
    :type :boolean
    :default (:gen-ores default-values)
    :comment "Whether AcademyCraft ores (reso, constraint, crystal, imaginary) generate in the world."}
   {:key :gen-phase-liquid
    :path "worldgen.gen-phase-liquid"
    :section :worldgen
    :type :boolean
    :default (:gen-phase-liquid default-values)
    :comment "Whether Imaginary Phase Liquid lakes generate underground."}])

(defn init-config!
  []
  (config-reg/register-config-descriptors! config-common/worldgen-domain descriptors)
  (config-reg/ensure-default-values! config-common/worldgen-domain default-values)
  (log/debug "Initialized worldgen config descriptors" {:domain config-common/worldgen-domain})
  nil)

(defn- value [k]
  (get (config-common/worldgen-config) k (get default-values k)))

(defn gen-ores-enabled? []
  (boolean (value :gen-ores)))

(defn gen-phase-liquid-enabled? []
  (boolean (value :gen-phase-liquid)))
