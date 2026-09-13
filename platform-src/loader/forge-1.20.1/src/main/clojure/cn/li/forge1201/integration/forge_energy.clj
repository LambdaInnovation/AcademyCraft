(ns cn.li.forge1201.integration.forge-energy
  "Forge Energy integration for descriptor-declared content endpoints."
  (:require [cn.li.mcmod.capability.registry :as cap-registry]
            [cn.li.platform.neutral.integration-runtime :as energy-hooks]
            [cn.li.mcmod.content.registry :as content-registry]
            [cn.li.platform.neutral.block-runtime :as tdsl]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.forge1201.capability CapabilityRegistry ForgeEnergyAdapter]
           [cn.li.mcmod.energy IEnergyCapable]
           [net.minecraftforge.common.capabilities ForgeCapabilities]
           [net.minecraftforge.energy IEnergyStorage]))

(defn- fe-conversion-rate
  []
  (double (energy-hooks/forge-energy-conversion-rate)))

(defn- create-forge-energy-adapter
  [^IEnergyCapable energy-capable conversion-rate]
  (ForgeEnergyAdapter. energy-capable (double conversion-rate)))

(defn- forge-energy-descriptors
  []
  (filter #(= :forge-energy-capability (:kind %))
          (content-registry/list-descriptors :integration)))

(defn- source-capability-key
  [descriptor]
  (get-in descriptor [:source :capability-key]))

(defn- target-capability-key
  [descriptor]
  (or (get-in descriptor [:target :capability-key]) :forge-energy))

(defn- target-tile-ids
  [descriptor]
  (vec (or (get-in descriptor [:target :tile-ids]) [])))

(defn- content-energy-capability
  [be descriptor]
  (when-let [capability-key (source-capability-key descriptor)]
    (when-let [content-energy-cap (cap-registry/get-capability be capability-key nil)]
      (when (cap-registry/is-present? content-energy-cap)
        (cap-registry/or-else content-energy-cap nil)))))

(defn- get-forge-energy-capability
  [be _side]
  (try
    (some (fn [descriptor]
            (when-let [content-energy (content-energy-capability be descriptor)]
              (create-forge-energy-adapter content-energy (fe-conversion-rate))))
          (forge-energy-descriptors))
    (catch Exception e
      (log/stacktrace "Error creating Forge Energy capability:" e)
      nil)))

(defn register-forge-energy-capability!
  []
  (let [descriptors (vec (forge-energy-descriptors))]
    (doseq [capability-key (distinct (map target-capability-key descriptors))]
      (CapabilityRegistry/register (name capability-key) ForgeCapabilities/ENERGY)
      (cap-registry/declare-capability! capability-key IEnergyStorage get-forge-energy-capability))
    (doseq [descriptor descriptors
            tile-id (target-tile-ids descriptor)]
      (tdsl/register-tile-capability-keys! tile-id (target-capability-key descriptor)))
    (log/debug "Forge Energy descriptor bridge enabled" {:descriptor-count (count descriptors)}))
  true)

(defn init-forge-energy!
  []
  (log/debug "Initializing Forge Energy integration...")
  (register-forge-energy-capability!)
  (log/debug "Forge Energy integration initialized"))
