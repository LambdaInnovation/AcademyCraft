(ns cn.li.ac.content.ability.teleporter.penetrate-dest
  "PenetrateTeleport's destination march, upstream PTContext.getDest().

  Upstream runs it on both sides: s_execute recomputes it on the server with
  the distance the client sent, and l_updateMark calls it every client tick to
  move the EntityTPMarking and set its `available` flag (which is what turns
  the marker red). So it lives here on its own, platform-free, taking the block
  probe as an argument.

  The march walks 0.8 at a time through a three-stage machine: stage 0 looks
  for the wall, stage 1 waits to come out the far side, stage 2 counts up to
  four more clear steps and stops. Ending in stage 1 means still inside the
  wall, which is what `available` reports.

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [def-skill-config-ops]]
            [cn.li.ac.ability.skill-config]
            [cn.li.ac.ability.service.skill-effects]))

(def-skill-config-ops :penetrate-teleport)

(def ^:private stage2-clearance-steps 4)

(defn- trunc-int
  "(int) in Java truncates toward zero — negative coordinates included."
  [x]
  (int (double x)))

(defn scan-step []
  (cfg-double :targeting.scan-step))

(defn cp-per-block [exp]
  (cfg-lerp :cost.up.cp-per-block exp))

(defn max-distance [exp]
  (cfg-lerp :targeting.max-distance exp))

(defn clamp-distance-by-cp
  [desired-distance current-cp exp]
  (let [per-block (max 1.0e-6 (cp-per-block exp))]
    (min (double desired-distance)
         (max-distance exp)
         (/ (double current-cp) per-block))))

(defn- has-place?
  "hasPlace: neither the block at the point nor the one above it collides."
  [collidable? x y z]
  (let [ix (trunc-int x)
        iy (trunc-int y)
        iz (trunc-int z)]
    (and (not (collidable? ix iy iz))
         (not (collidable? ix (inc iy) iz)))))

(defn destination
  "getDest(): march `distance` along `look-vec` from (x, y, z), returning the
  stopping point plus whether it is somewhere you can stand."
  [{:keys [x y z look-vec distance collidable?]}]
  (when (and look-vec collidable?)
    (let [step (scan-step)
          dx (double (:x look-vec))
          dy (double (:y look-vec))
          dz (double (:z look-vec))]
      (loop [stage 0
             clear-steps 0
             cx (double x)
             cy (double y)
             cz (double z)
             traveled 0.0]
        (if (> traveled (double distance))
          {:x cx :y cy :z cz :distance traveled :available? (not= stage 1)}
          (let [place? (has-place? collidable? cx cy cz)
                nx (+ cx (* step dx))
                ny (+ cy (* step dy))
                nz (+ cz (* step dz))
                nt (+ traveled step)]
            (cond
              (and (= stage 0) (not place?))
              (recur 1 clear-steps nx ny nz nt)

              (and (= stage 1) place?)
              (recur 2 0 nx ny nz nt)

              (= stage 2)
              (if (or (not place?) (>= clear-steps stage2-clearance-steps))
                {:x cx :y cy :z cz :distance traveled :available? true}
                (recur 2 (inc clear-steps) nx ny nz nt))

              :else
              (recur stage clear-steps nx ny nz nt))))))))
