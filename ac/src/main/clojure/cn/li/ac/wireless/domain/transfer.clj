(ns cn.li.ac.wireless.domain.transfer
  "Pure transfer planning helpers."
  (:require [cn.li.ac.wireless.domain.model :as model]))

(defn clamp
  [x lo hi]
  (-> x (max lo) (min hi)))

(defn rotated
  "Rotate vector `v` by a game-time-derived offset — long-run fair ordering
  for bandwidth-budgeted loops without per-tick shuffle allocation."
  [v ^long game-time]
  (let [n (count v)]
    (if (< n 2)
      v
      (let [k (Math/floorMod game-time n)]
        (if (zero? k)
          v
          (into (subvec v k) (subvec v 0 k)))))))

(defn rotate-start
  "Starting index for a game-time-rotated index-order walk of a vector of
  length `n` — same rotation semantics as `rotated`, but for hot per-tick
  loops that read (`nth v (mod (+ start i) n)`) in order rather than needing
  a materialized rotated vector (see wireless.runtime.node-transfer)."
  ^long [n ^long game-time]
  (if (< (long n) 2) 0 (Math/floorMod game-time (long n))))

(defn balance-plan
  "Build a pure balancing plan.

  Inputs:
  - entries: [{:id <stable-key> :energy :max-energy :bandwidth}]
  - matrix-bandwidth: transfer budget per balance tick
  - buffer0: current buffer
  - buffer-max: max buffer
  - start (optional): rotated walk start index — same fairness as `rotated`
    without allocating a rotated copy (hot tick path).

  Output:
  {:energies {id new-energy}
   :buffer new-buffer}"
  ([entries matrix-bandwidth buffer0 buffer-max]
   (balance-plan entries matrix-bandwidth buffer0 buffer-max 0))
  ([entries matrix-bandwidth buffer0 buffer-max start]
   (let [active (into [] (filter #(pos? (double (:max-energy %)))) entries)
         n (long (count active))
         ^double max-sum (reduce (fn [^double s e]
                                   (+ s (double (:max-energy e))))
                                 0.0
                                 active)
         buffer0 (clamp (double buffer0) 0.0 (double buffer-max))
         start (long start)]
     (if (or (zero? n) (not (pos? max-sum)))
       {:energies {} :buffer buffer0}
       (let [^double sum (+ (reduce (fn [^double s e]
                                      (+ s (double (:energy e))))
                                    0.0
                                    active)
                            buffer0)
             percent (clamp (/ sum max-sum) 0.0 1.0)
             start' (if (pos? n) (Math/floorMod start n) 0)]
         (loop [i 0
                transfer-left (double matrix-bandwidth)
                buffer buffer0
                out (transient {})]
           (if (or (>= i n) (not (pos? transfer-left)))
             {:energies (persistent! out)
              :buffer (clamp buffer 0.0 (double buffer-max))}
             (let [{:keys [id energy max-energy bandwidth]} (nth active (rem (+ start' i) n))
                   energy (double energy)
                   max-energy (double max-energy)
                   bandwidth (max 0.0 (double bandwidth))
                   target (* max-energy percent)
                   delta (- target energy)
                   room-in-buffer (- (double buffer-max) buffer)]
               (cond
                 (and (pos? delta) (pos? buffer))
                 (let [give (min delta bandwidth transfer-left buffer)
                       next-energy (min max-energy (+ energy give))]
                   (recur (inc i)
                          (- transfer-left give)
                          (- buffer give)
                          (assoc! out id next-energy)))

                 (and (neg? delta) (pos? room-in-buffer))
                 (let [take (min (- delta) bandwidth transfer-left room-in-buffer)
                       next-energy (max 0.0 (- energy take))]
                   (recur (inc i)
                          (- transfer-left take)
                          (+ buffer take)
                          (assoc! out id next-energy)))

                 :else
                 (recur (inc i) transfer-left buffer (assoc! out id energy)))))))))))

(defn node-connection-capacity?
  [connection capacity]
  (model/connection-has-capacity? connection capacity))

(defn- actual-provided-transfer
  [provided required]
  (let [provided (double provided)
        required (double required)]
    (if (> provided required) required provided)))

(defn collect-from-generator-step
  "Pure pull step for one generator slot.

  Returns `nil` when no transfer should occur."
  [transfer-left node-energy node-max gen-bandwidth provided]
  (when (and (pos? (double transfer-left))
             (pos? (double gen-bandwidth)))
    (let [node-space (- (double node-max) (double node-energy))
          required (min (double transfer-left)
                        (double gen-bandwidth)
                        node-space)]
      (when (pos? required)
        (let [actual (actual-provided-transfer provided required)]
          {:required required
           :actual-transfer actual
           :transfer-left (- transfer-left actual)
           :node-energy (+ node-energy actual)})))))

(defn distribute-to-receiver-step
  "Pure push step for one receiver slot.

  Returns `{:give ...}` when a transfer should be attempted.
  Actual energy moved is determined at apply time via `injectEnergy`."
  [transfer-left node-energy rec-bandwidth required-energy]
  (when (and (pos? (double transfer-left))
             (pos? (double node-energy)))
    (let [give0 (min (double node-energy)
                     (double transfer-left)
                     (double rec-bandwidth))
          give (min give0 (double required-energy))]
      (when (pos? give) {:give give}))))
