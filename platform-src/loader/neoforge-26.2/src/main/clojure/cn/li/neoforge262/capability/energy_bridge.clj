(ns cn.li.neoforge262.capability.energy-bridge
  "IoC bridge: reads ac-layer pure functions from Framework [:registry :tiles],
   injects them into the native 26.2 UniversalEnergyStorage handler.

   ac layer only provides {:energy-logic {:receive-fn ...}} in Framework.
   This Forge loader namespace is the custom bridge that instantiates the Java skeleton."
  (:require [cn.li.mcmod.framework :as fw])
  (:import [cn.li.neoforge262.shim UniversalEnergyStorage]
           [net.neoforged.neoforge.transfer.energy EnergyHandler]))

(defn create-energy-storage
  "Create an EnergyHandler backed by ac-layer pure functions from Framework.
   block-id: the DSL block id (e.g. 'example-block-id').
   Returns nil if no energy-logic registered for this block."
  ^EnergyHandler
  [block-id]
  (when-let [fw-atom (fw/fw-atom)]
    (when-let [energy-logic (get-in @fw-atom [:registry :tiles block-id :energy-logic])]
      (UniversalEnergyStorage.
        (:receive-fn energy-logic)
        (:extract-fn energy-logic)
        (:get-stored-fn energy-logic)
        (:get-max-fn energy-logic)
        (:can-extract-fn energy-logic)
        (:can-receive-fn energy-logic)))))

(defn create-energy-storage-with-defaults
  "Create EnergyHandler with sensible defaults for missing functions.
   Useful when ac only provides partial logic."
  ^EnergyHandler
  [block-id]
  (when-let [^UniversalEnergyStorage storage (create-energy-storage block-id)]
    storage))
