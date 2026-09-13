(ns cn.li.ac.ability.server.damage.entity
  "Pure AC-side rules for entity damage effects.

  Keeps falloff and reflection-chain selection out of forge adapters."
  (:require [cn.li.ac.ability.config :as cfg]))

(defn- distance-3d
  [{x1 :x y1 :y z1 :z} {x2 :x y2 :y z2 :z}]
  (Math/sqrt (+ (* (- x2 x1) (- x2 x1))
                (* (- y2 y1) (- y2 y1))
                (* (- z2 z1) (- z2 z1)))))

(defn compute-aoe-damage
  [origin-pos target-pos radius base-damage falloff?]
  (let [dist (distance-3d origin-pos target-pos)]
    (if (> dist radius)
      0.0
      (double (if falloff?
                (* base-damage (max 0.0 (- 1.0 (/ dist radius))))
                base-damage)))))

(defn- reflection-remove-self
  "Remove the current entity from candidate list."
  [current-entity-uuid {:keys [entity-uuid]}]
  (= entity-uuid current-entity-uuid))

(defn- reflection-annotate-dist
  "Annotate a candidate with its distance from current-pos."
  [current-pos {:keys [x y z] :as candidate}]
  [candidate (distance-3d current-pos {:x x :y y :z z})])

(defn- reflection-within-radius?
  "True when candidate distance is within max-radius."
  [max-radius [_candidate dist]]
  (<= dist max-radius))

(defn select-next-reflection-target
  [current-entity-uuid current-pos candidates max-radius]
  (->> candidates
       (remove (partial reflection-remove-self current-entity-uuid))
       (map (partial reflection-annotate-dist current-pos))
       (filter (partial reflection-within-radius? max-radius))
       (sort-by second)
       (ffirst)
       :entity-uuid))

(defn compute-reflected-damage
  [current-damage]
  (* current-damage (cfg/reflected-damage-multiplier)))
