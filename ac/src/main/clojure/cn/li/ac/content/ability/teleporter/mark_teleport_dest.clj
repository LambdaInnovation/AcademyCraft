(ns cn.li.ac.content.ability.teleporter.mark-teleport-dest
  "MarkTeleport's destination solve, upstream MTContext.getMaxDist/getDest.

  Upstream runs this on BOTH sides: s_execute calls getDest on the server to
  decide where the player actually lands, and MTContextC.l_update calls the
  very same method on the client every tick to place the aim marker. So it
  lives here on its own, platform-free, and both callers hand it the world
  lookups it needs — a raycast result and a head-block predicate — rather than
  each growing its own copy of the six-face table.

  No Minecraft imports."
  ;; skill-config and skill-effects back the cfg-*/skill-exp fns
  ;; def-skill-config-ops emits fully qualified.
  (:require [cn.li.ac.ability.dsl :refer [def-skill-config-ops]]
            [cn.li.ac.ability.skill-config]
            [cn.li.ac.ability.service.skill-effects]))

(def-skill-config-ops :mark-teleport)

(defn max-distance
  "getMaxDist: min((ticks + 1) * 2, min(lerpf(25, 60, exp), cp / cpb))."
  [exp cp hold-ticks]
  (let [cpb (cfg-lerp :cost.up.cp-per-block exp)
        max-range (cfg-lerp :targeting.range exp)
        cp-limit (if (pos? cpb) (/ (double cp) cpb) max-range)]
    (min (* (cfg-double :targeting.range-per-hold-tick)
            (inc (long hold-ticks)))
         (min max-range cp-limit))))

(defn cp-per-block
  [exp]
  (cfg-lerp :cost.up.cp-per-block exp))

(defn min-distance
  []
  (cfg-double :targeting.min-distance))

(defn hit-destination
  "getDest's hit branch. `head-blocked?` is called as (f x y z) for the side
  faces only, matching upstream's `mop.sideHit.getIndex() > 1` guard; it
  answers whether the block one above the candidate spot is solid."
  [hit head-blocked?]
  (let [hit-x (double (or (:hit-x hit) (:x hit) 0.0))
        hit-y (double (or (:hit-y hit) (:y hit) 0.0))
        hit-z (double (or (:hit-z hit) (:z hit) 0.0))
        block-y (double (or (:y hit) 0.0))]
    (if (= (:hit-type hit) :entity)
      ;; LambdaLib's entity raycast builds the result via
      ;; new RayTraceResult(entity) — hitVec is the entity's FEET position,
      ;; not the intersection — so dest = entity pos + eye height.
      {:target-x (double (or (:x hit) hit-x))
       :target-y (+ (double (or (:y hit) hit-y))
                    (double (or (:eye-height hit) 1.6)))
       :target-z (double (or (:z hit) hit-z))}
      (let [face (:face hit)
            resolved (case face
                       :down {:target-x hit-x :target-y (- hit-y 1.0) :target-z hit-z}
                       :up {:target-x hit-x :target-y (+ hit-y 1.8) :target-z hit-z}
                       :north {:target-x hit-x :target-y (+ block-y 1.7) :target-z (- hit-z 0.6)}
                       :south {:target-x hit-x :target-y (+ block-y 1.7) :target-z (+ hit-z 0.6)}
                       :west {:target-x (- hit-x 0.6) :target-y (+ block-y 1.7) :target-z hit-z}
                       :east {:target-x (+ hit-x 0.6) :target-y (+ block-y 1.7) :target-z hit-z}
                       {:target-x hit-x :target-y hit-y :target-z hit-z})]
        (if (and (#{:north :south :west :east} face)
                 head-blocked?
                 (head-blocked? (:target-x resolved)
                                (:target-y resolved)
                                (:target-z resolved)))
          (update resolved :target-y - 1.25)
          resolved)))))

(defn miss-destination
  "getDest's miss branch: getPositionEyes(1f) + look * dist. The player's x/z
  are their eye's, so only y differs from the feet."
  [x eye-y z look-vec dist]
  {:target-x (+ (double x) (* (double (:x look-vec)) (double dist)))
   :target-y (+ (double eye-y) (* (double (:y look-vec)) (double dist)))
   :target-z (+ (double z) (* (double (:z look-vec)) (double dist)))})

(defn destination
  "getDest: the hit point when the eye-forward trace lands on something,
  otherwise the free-flight endpoint."
  [{:keys [hit head-blocked? x eye-y z look-vec dist]}]
  (if hit
    (hit-destination hit head-blocked?)
    (miss-destination x eye-y z look-vec dist)))

(defn distance-from
  [x y z {:keys [target-x target-y target-z]}]
  (let [dx (- (double target-x) (double x))
        dy (- (double target-y) (double y))
        dz (- (double target-z) (double z))]
    (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))))
