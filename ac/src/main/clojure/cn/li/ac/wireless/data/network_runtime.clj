(ns cn.li.ac.wireless.data.network-runtime
  "Runtime ticking for wireless networks."
  (:require [cn.li.ac.wireless.core.scheduling :as sched]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.network-validation :as validation]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.network-energy-balance :as energy-balance]))

(defn tick-wireless-net!
  "Advance one network for one server tick. Off-slot ticks cost an `active?`
  check only; validation and balancing run on the network's stagger slot
  (`:network-update-interval-ticks`, phase-seeded by matrix position)."
  [network world ctx]
  (let [{:keys [game-time cfg cap-cache]} ctx]
    (when (and (network-state/active? network)
               (sched/due? (long game-time)
                           (long (get cfg :network-update-interval-ticks))
                           (vb/pos-of (:matrix network)))
               (validation/validate! network world cap-cache))
      (energy-balance/balance-energy! network world ctx))))
