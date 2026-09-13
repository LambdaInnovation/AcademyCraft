(ns cn.li.ac.wireless.core.scheduling
  "Zero-state tick scheduling for the wireless runtime.

  Work is paced by gameTime modulo with a per-entity phase offset derived
  from an immutable identity (position tuple), so entities stagger across
  ticks instead of spiking on the same one — no per-entity counters, no
  commits, no persistence.")

(defn due?
  "True when `game-time` hits this entity's slot in an `interval`-tick cycle.
  `seed` must be an immutable identity (matrix-pos / node-pos / keyword)."
  [^long game-time ^long interval seed]
  (if (<= interval 1)
    true
    (zero? (Math/floorMod (+ game-time (long (hash seed))) interval))))

(defn next-due-tick
  "Smallest tick >= game-time at which (due? tick interval seed) is true.
  Pure function of the same inputs due? uses — lets a caller schedule an
  entity's next occurrence once instead of polling due? every tick."
  ^long [^long game-time ^long interval seed]
  (if (<= interval 1)
    game-time
    (let [target (Math/floorMod (- (long (hash seed))) interval)]
      (+ game-time (Math/floorMod (- target game-time) interval)))))
