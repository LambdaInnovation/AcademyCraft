(ns cn.li.ac.integration.block.energy-converter.base
  (:require [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.ac.integration.block.energy-converter.config :as ec-config]
            [cn.li.ac.integration.block.energy-converter.schema :as ec-schema])
  (:import [cn.li.mcmod.energy IEnergyCapable]))

(defn get-energy
  [be]
  (double (get (or (platform-be/get-custom-state be) (ec-schema/default-state-map)) :energy 0.0)))

(defn set-energy!
  [be energy]
  (let [v (max 0.0 (min (double (ec-config/energy-capacity)) (double energy)))]
    (platform-be/set-custom-state! be (assoc (or (platform-be/get-custom-state be) (ec-schema/default-state-map)) :energy v))
    ;; No TESR reads this block's custom-state (GUI energy bar syncs via DataSlot) —
    ;; NBT-dirty only, no client packet on this per-tick hot path.
    (platform-be/set-changed! be)
    v))

(defn read-nbt-fn
  [tag]
  (merge (ec-schema/default-state-map)
         {:energy (if (sd/has-key-safe? tag "Energy")
                    (sd/get-double tag "Energy")
                    0.0)
          :max-energy (double (ec-config/energy-capacity))
          :wireless-bandwidth (double (ec-config/transfer-bandwidth))}))

(defn write-nbt-fn
  [be tag]
  (let [state (or (platform-be/get-custom-state be) (ec-schema/default-state-map))]
    (sd/set-double! tag "Energy" (double (get-energy be)))
    (sd/set-double! tag "MaxEnergy" (double (ec-config/energy-capacity)))
    (sd/set-double! tag "WirelessBandwidth" (double (ec-config/transfer-bandwidth)))))

(deftype ConverterEnergyImpl [be can-recv? can-ext?]
  IEnergyCapable
  (receiveEnergy [_ max-receive simulate]
    (if-not can-recv?
      0
      (let [cur (get-energy be)
            room (- (double (ec-config/energy-capacity)) cur)
            actual (max 0.0 (min room (double max-receive)))]
        (when (and (not simulate) (pos? actual))
          (set-energy! be (+ cur actual)))
        (int actual))))
  (extractEnergy [_ max-extract simulate]
    (if-not can-ext?
      0
      (let [cur (get-energy be)
        actual (max 0.0 (min cur (double max-extract)))]
        (when (and (not simulate) (pos? actual))
          (set-energy! be (- cur actual)))
        (int actual))))
  (getEnergyStored [_]
    (int (get-energy be)))
  (getMaxEnergyStored [_]
    (int (ec-config/energy-capacity)))
  (canExtract [_] (boolean can-ext?))
  (canReceive [_] (boolean can-recv?)))

(defn make-energy-capability
  [be block-id]
  (case (str block-id)
    "rf-input" (ConverterEnergyImpl. be true false)
    "rf-output" (ConverterEnergyImpl. be false true)
    "eu-input" (ConverterEnergyImpl. be true false)
    "eu-output" (ConverterEnergyImpl. be false true)
    (ConverterEnergyImpl. be false false)))