(ns cn.li.ac.integration.block.energy-converter.schema
  "Pure data schema for converter state."
  (:require [cn.li.ac.integration.block.energy-converter.config :as ec-config]))

(def default-state
  {:energy 0.0
   :max-energy (double ec-config/default-energy-capacity)
   :wireless-bandwidth (double ec-config/default-transfer-bandwidth)
   :gen-speed (double ec-config/default-transfer-bandwidth)})

(defn default-state-map []
  (assoc default-state
         :max-energy (double (ec-config/energy-capacity))
         :wireless-bandwidth (double (ec-config/transfer-bandwidth))
         :gen-speed (double (ec-config/transfer-bandwidth))))

(def energy-converter-gui-schema
  [{:key :energy
    :type :double
    :default 0.0
    :persist? false
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :max-energy
    :type :double
    :default (double ec-config/default-energy-capacity)
    :persist? false
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}])