(ns cn.li.ac.wireless.data.network-energy-balance
  (:require [cn.li.ac.wireless.domain.transfer :as transfer]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]
            [cn.li.ac.wireless.data.store :as store]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.runtime.effects :as effects]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessMatrix IWirelessNode]))

(defn- node-entry
  [cap-cache world node-vb]
  (when-let [node (resolver/resolve-cap-cached
                    cap-cache resolver/resolve-node-cap world node-vb)]
    {:id (vb/pos-of node-vb)
     :vb node-vb
     :node ^IWirelessNode node
     :energy (double (.getEnergy ^IWirelessNode node))
     :max-energy (double (.getMaxEnergy ^IWirelessNode node))
     :bandwidth (double (.getBandwidth ^IWirelessNode node))}))

(defn- collect-active-nodes!
  "Return active node entries; unlink nodes whose chunk is loaded but whose
  capability no longer resolves (block destroyed)."
  [network world cap-cache]
  (persistent!
   (loop [node-vbs (seq (network-state/get-nodes network))
          acc (transient [])]
     (if-not node-vbs
       acc
       (let [node-vb (first node-vbs)]
         (if (vb/is-chunk-loaded? node-vb world)
           (if-let [entry (node-entry cap-cache world node-vb)]
             (recur (next node-vbs) (conj! acc entry))
             (do
               (try
                 (store/unlink-node! (:world-data network) network node-vb)
                 (catch Exception e
                   (log/warn "Failed to unlink missing node during balance"
                             (vb/vblock-to-string node-vb) (ex-message e))))
               (recur (next node-vbs) acc)))
           (recur (next node-vbs) acc)))))))

(defn balance-energy!
  "Balance energy across linked wireless nodes using a buffer-based model.
  - Total transfer is limited by matrix bandwidth per balance tick.
  - Per-node adjustment is limited by node bandwidth.
  - Buffer is clamped to [0, buffer-max].
  Commits nothing when the plan is a no-op (idle network -> zero setDirty)."
  [network world {:keys [game-time cfg cap-cache]}]
  (let [network (entity-commit/resolve-network (:world-data network) network)]
    (when-let [^IWirelessMatrix matrix
               (resolver/resolve-cap-cached
                 cap-cache resolver/resolve-matrix-cap world (:matrix network))]
      (let [entries (collect-active-nodes! network world cap-cache)]
        (when (seq entries)
          ;; Rotation by PASS number (quot game-time interval), not game-time:
          ;; with an even balance interval the game-time parity at consecutive
          ;; passes never flips, so `game-time mod n` froze the walk order and
          ;; the first node always consumed the whole budget — the take side
          ;; starved the give side forever (upstream shuffles every tick; a
          ;; deterministic rotation must advance per pass to be fair).
          (let [interval (max 1 (long (get cfg :network-update-interval-ticks 1)))
                start (transfer/rotate-start (count entries)
                                             (quot (long game-time) interval))
                matrix-bandwidth (double (.getMatrixBandwidth matrix))
                buffer-max (double (get cfg :network-buffer-max))
                buffer0 (double (network-state/get-buffer network))
                ;; rotate-start + balance-plan arity — no per-tick rotated vector alloc.
                plan (transfer/balance-plan entries matrix-bandwidth buffer0 buffer-max start)]
            (effects/apply-node-energy-plan! entries (:energies plan))
            (when (not= buffer0 (double (:buffer plan)))
              (network-state/set-buffer! network (:buffer plan)))))))))
