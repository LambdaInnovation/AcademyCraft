(ns cn.li.ac.wireless.runtime.effects
  "Runtime side-effect helpers for applying transfer plans."
  (:import [cn.li.acapi.wireless IWirelessGenerator IWirelessNode IWirelessReceiver])
  (:require [cn.li.mcmod.util.log :as log]))

(defn apply-node-energy-plan!
  [entries energies]
  (doseq [{:keys [id node energy max-energy]} entries]
    (when-let [next-energy (get energies id)]
      (.setEnergy ^IWirelessNode node (-> (double next-energy)
                                          (max 0.0)
                                          (min (double max-energy)))))))

(defn apply-generator-collect-step!
  [node {:keys [required actual-transfer provided]} gen-cap]
  (when (> (double provided) (double required))
    (log/warn "Energy input overflow for generator" (str gen-cap)
              "provided=" provided "required=" required))
  (.setEnergy ^IWirelessNode node (+ (.getEnergy ^IWirelessNode node) (double actual-transfer))))

(defn apply-receiver-distribute-step!
  [node {:keys [give]} rec-cap]
  (let [leftover (double (.injectEnergy ^IWirelessReceiver rec-cap (double give)))
        actual (- (double give) leftover)]
    (.setEnergy ^IWirelessNode node (- (.getEnergy ^IWirelessNode node) actual))
    actual))
