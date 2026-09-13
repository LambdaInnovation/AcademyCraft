(ns cn.li.ac.ability.client.effects.wave-effect
  "Upstream WaveEffect + WaveEffectRenderer, shared by VecReflection and
  VecDeviation.

  Both skills spawn the same entity and differ only in how many rings it has
  and how big they are — new WaveEffect(world, 2, 1.1) for a reflection,
  new WaveEffect(world, 1, 0.6) for a deviation. The constructor rolls each
  ring its own life, a depth offset along the effect's normal, a size jitter
  and a start delay; the renderer orients the whole thing to the CASTER's
  rotation frozen at spawn, pushes the group forward along that normal by
  ticksExisted / 40, and draws every ring on two cubic curves with depth test
  and cull off, white, at 0.7 of the alpha curve.

  No Minecraft imports."
  (:require [cn.li.ac.ability.client.effects.cubic-curve :as curve]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]))

(def life-ticks 15)

(def ^:private glow-texture
  (modid/asset-path "textures" "effects/glow_circle.png"))

(def ^:private alpha-curve
  (curve/curve [[0.0 0.0] [0.2 1.0] [0.5 1.0] [0.8 1.0] [1.0 0.0]]))

(def ^:private size-curve
  (curve/curve [[0.0 0.4] [0.2 0.8] [2.5 1.5]]))

(defn- ranged
  ^double [^double from ^double to]
  (+ from (rand (- to from))))

(defn- rangei
  [from to]
  (+ (long from) (rand-int (- (long to) (long from)))))

(defn build-rings
  "The WaveEffect constructor's per-ring roll."
  [rings size]
  (mapv (fn [idx]
          {:life (rangei 8 12)
           :offset (+ (* idx 1.5) (ranged -0.3 0.3))
           :size (* (double size) (ranged 0.8 1.2))
           :time-offset (+ (* idx 2) (rangei -1 1))})
        (range (long rings))))

(defn view-basis
  "The effect frame: glRotated(-yaw) then glRotated(pitch) sends local +z along
  the caster's look, which is what the rings stack and drift along."
  [^double yaw ^double pitch]
  (let [sy (Math/sin yaw) cy (Math/cos yaw)
        sp (Math/sin pitch) cp (Math/cos pitch)]
    {:right (vec3/v3 cy 0.0 sy)
     :up (vec3/v3 (* (- sp) sy) cp (* sp cy))
     :fwd (vec3/v3 (* (- cp) sy) (- sp) (* cp cy))}))

(defn ops
  "Render ops for one live wave: `{:x :y :z :ttl :max-ttl :rings :yaw-rad
  :pitch-rad}`."
  [{:keys [x y z ttl max-ttl rings yaw-rad pitch-rad]}]
  (let [max-ttl (double (max 1 (or max-ttl life-ticks)))
        age (- max-ttl (double (or ttl 0)))
        max-alpha (max 0.0 (min 1.0 (curve/value-at alpha-curve (/ age max-ttl))))
        {:keys [right up fwd]} (view-basis (double (or yaw-rad 0.0))
                                           (double (or pitch-rad 0.0)))
        origin (vec3/v3 (double x) (double y) (double z))
        drift (vec3/v+ origin (vec3/v* fwd (/ age 40.0)))
        size-scale (curve/value-at size-curve (max 0.0 (min 1.62 (/ age 20.0))))]
    (keep (fn [{:keys [life offset size time-offset]}]
            (let [ring-alpha (max 0.0 (min 1.0 (curve/value-at
                                                 alpha-curve
                                                 (/ (- age (double time-offset))
                                                    (double (max 1 life))))))
                  real-alpha (min max-alpha ring-alpha)]
              (when (pos? real-alpha)
                (let [center (vec3/v+ drift (vec3/v* fwd (double offset)))
                      ;; createBillboard(-.5, -.5, 1, 1) is a 1x1 quad, so the
                      ;; half-extent is 0.5 before the ring's own scale.
                      half (* 0.5 (double size) size-scale)
                      side (vec3/v* right half)
                      lift (vec3/v* up half)
                      p0 (vec3/v+ (vec3/v- center side) lift)
                      p1 (vec3/v+ (vec3/v+ center side) lift)
                      p2 (vec3/v- (vec3/v+ center side) lift)
                      p3 (vec3/v- (vec3/v- center side) lift)]
                  (assoc
                    (ru/quad-op glow-texture p0 p1 p2 p3
                                {:r 255 :g 255 :b 255
                                 :a (int (max 0 (min 255 (* 255.0 real-alpha 0.7))))})
                    :no-depth-test? true)))))
          rings)))
