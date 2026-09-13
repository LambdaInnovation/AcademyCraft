(ns cn.li.ac.wireless.runtime.node-transfer
  "Per-tick node-connection energy transfer and tick entry point.

  Transfer runs every tick within the node's bandwidth budget; integrity
  validation runs on the connection's stagger slot
  (`:validate-interval-ticks`, phase-seeded by node position)."
  (:require [cn.li.ac.wireless.core.scheduling :as sched]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.node-conn-validation :as validation]
            [cn.li.ac.wireless.domain.transfer :as transfer]
            [cn.li.ac.wireless.runtime.effects :as effects])
  (:import [cn.li.acapi.wireless
            IWirelessGenerator
            IWirelessNode
            IWirelessReceiver]))

(defn- transfer-from-generators!
  "Collect energy from generators to node.

  Index-order walk from a rotated start (`transfer/rotate-start`) instead of
  `(seq (transfer/rotated ...))`: same long-run fair ordering, zero vector
  allocation on the hot per-tick path (no subvec+into materialization)."
  [conn node bandwidth world {:keys [game-time cap-cache]}]
  (when (pos? (double bandwidth))
    (let [generators (node-conn/get-generators conn)
          n (long (count generators))]
      (when (pos? n)
        (let [start (transfer/rotate-start n (long game-time))]
          (loop [i 0
                 transfer-left (double bandwidth)]
            (when (and (< i n) (pos? transfer-left))
              (let [gen-vb (nth generators (rem (+ start i) n))]
                (if (vb/is-chunk-loaded? gen-vb world)
                  (if-let [gen-cap (resolver/resolve-cap-cached
                                     cap-cache resolver/resolve-generator-cap world gen-vb)]
                    (let [node-max (double (.getMaxEnergy ^IWirelessNode node))
                          node-energy (double (.getEnergy ^IWirelessNode node))
                          gen-bandwidth (double (.getGeneratorBandwidth ^IWirelessGenerator gen-cap))
                          node-space (- node-max node-energy)
                          required (min transfer-left gen-bandwidth node-space)
                          provided (double (.getProvidedEnergy ^IWirelessGenerator gen-cap required))
                          step (transfer/collect-from-generator-step
                                transfer-left node-energy node-max gen-bandwidth provided)]
                      (if step
                        (do
                          (effects/apply-generator-collect-step!
                            node (assoc step :provided provided) gen-cap)
                          (recur (inc i) (double (:transfer-left step))))
                        (recur (inc i) transfer-left)))

                    ;; gen-cap nil — skip, don't remove; validation owns removal.
                    (recur (inc i) transfer-left))

                  (recur (inc i) transfer-left))))))))))

(defn- transfer-to-receivers!
  "Distribute energy from node to receivers.

  Index-order walk from a rotated start — see `transfer-from-generators!`."
  [conn node bandwidth world {:keys [game-time cap-cache]}]
  (when (pos? (double bandwidth))
    (let [receivers (node-conn/get-receivers conn)
          n (long (count receivers))]
      (when (pos? n)
        (let [start (transfer/rotate-start n (long game-time))]
          (loop [i 0
                 transfer-left (double bandwidth)]
            (when (and (< i n) (pos? transfer-left))
              (let [rec-vb (nth receivers (rem (+ start i) n))]
                (if (vb/is-chunk-loaded? rec-vb world)
                  (if-let [rec-cap (resolver/resolve-cap-cached
                                     cap-cache resolver/resolve-receiver-cap world rec-vb)]
                    (let [node-energy (double (.getEnergy ^IWirelessNode node))
                          step (transfer/distribute-to-receiver-step
                                transfer-left
                                node-energy
                                (double (.getReceiverBandwidth ^IWirelessReceiver rec-cap))
                                (double (.getRequiredEnergy ^IWirelessReceiver rec-cap)))]
                      (if step
                        (let [actual (effects/apply-receiver-distribute-step! node step rec-cap)]
                          (recur (inc i) (- transfer-left actual)))
                        (recur (inc i) transfer-left)))

                    ;; rec-cap nil — skip this tick; validation owns removal.
                    (recur (inc i) transfer-left))

                  ;; chunk not loaded — skip; re-validated when chunk loads again.
                  (recur (inc i) transfer-left))))))))))

(defn tick-node-conn!
  "Tick the node connection: validate on the stagger slot, transfer every tick."
  [conn world ctx]
  (let [{:keys [game-time cfg cap-cache]} ctx
        world-data (:world-data conn)
        node-pos (vb/pos-of (:node conn))
        valid? (if (sched/due? (long game-time)
                               (long (get cfg :validate-interval-ticks))
                               node-pos)
                 (validation/validate! conn world ctx)
                 (not (node-conn/is-disposed? conn)))]
    (when valid?
      (let [conn (entity-commit/resolve-connection world-data conn)
            node-vb (:node conn)]
        (when (vb/is-chunk-loaded? node-vb world)
          (when-let [node (resolver/resolve-cap-cached
                            cap-cache resolver/resolve-node-cap world node-vb)]
            (let [bandwidth (.getBandwidth ^IWirelessNode node)]
              (transfer-from-generators! conn node bandwidth world ctx)
              (transfer-to-receivers! conn node bandwidth world ctx))))))))
