(ns cn.li.ac.content.ability.teleporter.flashing-dest
  "Flashing's destination solve, upstream MainContext.getDest(keyid).

  Upstream runs it on both sides: serverPerform recomputes it to decide where
  the player lands, and localTick calls it every client tick to move the
  EntityTPMarking. So it lives here on its own, platform-free, taking the world
  lookups as arguments.

  It is NOT MarkTeleport's getDest even though the six-face table looks alike:
  the side faces here offset from the HIT's y (`mop.hitVec.y + 1.7`) where
  MarkTeleport uses the BLOCK's (`mop.getBlockPos().getY() + 1.7`), and the
  trace runs from the player's feet to an eye-based endpoint rather than
  eye-forward. Keeping them separate is deliberate.

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [def-skill-config-ops]]
            [cn.li.ac.ability.skill-config]
            [cn.li.ac.ability.service.skill-effects]))

(def-skill-config-ops :flashing)

(def block-side-faces #{:north :south :west :east})

(defn normalize-3d
  [x y z]
  (let [len (Math/sqrt (+ (* x x) (* y y) (* z z)))]
    (if (< len 1.0e-6)
      [0.0 0.0 1.0]
      [(/ x len) (/ y len) (/ z len)])))

(defn direction-vector
  "dirs[keyid] rotated by rotateAroundZ(pitch) then rotateYaw(-90 - yaw).

  Rotating a pure-z vector about Z leaves it alone, so the two strafes come
  out horizontal while forward/back keep the pitch — and every one of them
  stays UNIT length, which is why the strafes are normalized here rather than
  left as the raw horizontal perpendicular (that has length cos(pitch))."
  [look-vec direction]
  (let [[fx fy fz] (normalize-3d (double (:x look-vec))
                                 (double (:y look-vec))
                                 (double (:z look-vec)))]
    (case direction
      :forward [fx fy fz]
      :back [(- fx) (- fy) (- fz)]
      :left (normalize-3d fz 0.0 (- fx))
      :right (normalize-3d (- fz) 0.0 fx)
      [fx fy fz])))

(defn blink-distance
  [exp]
  (cfg-lerp :movement.blink-distance exp))

(defn hit-destination
  "getDest's hit branch. `head-blocked?` is called as (f x y z) for the side
  faces only, matching upstream's `mop.sideHit.getIndex() > 1` guard."
  [hit fallback-end head-blocked?]
  (let [hit-x (double (or (:hit-x hit) (:x hit) (:x fallback-end) 0.0))
        hit-y (double (or (:hit-y hit) (:y hit) (:y fallback-end) 0.0))
        hit-z (double (or (:hit-z hit) (:z hit) (:z fallback-end) 0.0))]
    (if (= (:hit-type hit) :entity)
      {:to-x hit-x
       :to-y (+ hit-y (double (or (:eye-height hit) 1.6)))
       :to-z hit-z}
      (let [face (:face hit)
            resolved (case face
                       :down {:to-x hit-x :to-y (- hit-y 1.0) :to-z hit-z}
                       :up {:to-x hit-x :to-y (+ hit-y 1.8) :to-z hit-z}
                       :north {:to-x hit-x :to-y (+ hit-y 1.7) :to-z (- hit-z 0.6)}
                       :south {:to-x hit-x :to-y (+ hit-y 1.7) :to-z (+ hit-z 0.6)}
                       :west {:to-x (- hit-x 0.6) :to-y (+ hit-y 1.7) :to-z hit-z}
                       :east {:to-x (+ hit-x 0.6) :to-y (+ hit-y 1.7) :to-z hit-z}
                       {:to-x hit-x :to-y hit-y :to-z hit-z})]
        (if (and (contains? block-side-faces face)
                 head-blocked?
                 (head-blocked? (:to-x resolved) (:to-y resolved) (:to-z resolved)))
          (update resolved :to-y - 1.25)
          resolved)))))

(defn destination
  "getDest(keyid): dst is the player's EYE plus dir * dist, the trace runs from
  their FEET to it, and a miss leaves the answer at dst — in mid-air if that is
  where it lands."
  [{:keys [x y z eye-y look-vec direction dist raycast head-blocked?]}]
  (let [[dx dy dz] (direction-vector look-vec direction)
        start-x (double x)
        start-y (double y)
        start-z (double z)
        end-x (+ start-x (* (double dist) dx))
        end-y (+ (double eye-y) (* (double dist) dy))
        end-z (+ start-z (* (double dist) dz))
        ray-dx (- end-x start-x)
        ray-dy (- end-y start-y)
        ray-dz (- end-z start-z)
        ray-distance (Math/sqrt (+ (* ray-dx ray-dx) (* ray-dy ray-dy) (* ray-dz ray-dz)))
        [ndx ndy ndz] (normalize-3d ray-dx ray-dy ray-dz)
        hit (when raycast (raycast start-x start-y start-z ndx ndy ndz ray-distance))]
    (assoc (if hit
             (hit-destination hit {:x end-x :y end-y :z end-z} head-blocked?)
             {:to-x end-x :to-y end-y :to-z end-z})
           :from-x start-x :from-y start-y :from-z start-z)))
