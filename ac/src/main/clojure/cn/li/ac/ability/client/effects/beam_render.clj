(ns cn.li.ac.ability.client.effects.beam-render
  "Shared client-side beam render operation builders for ability FX."
  (:require [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid])
  (:import [cn.li.mcmod.math V3]))

(def ^:private default-beam-texture
  (modid/asset-path "textures" "effects/glow_line.png"))

(defn cylinder-beam-ops
  "Build a tube beam from start to end as longitudinal quads around the axis.

  Upstream RendererRayCylinder (DIV=12): unlike a flat billboard, a tube keeps
  visible width from every angle — a billboard is edge-on (zero area) for the
  caster's own first-person view, which sits exactly on the beam axis, so the
  main ray vanished while off-axis forks still showed.

  Defaults: 16 segments and the soft glow_line texture (a gentle falloff
  across the quad's width) — a coarse 8-segment tube with the jagged arc.png
  texture read as a hollow pipe with a ring of bright seam lines."
  [start end {:keys [texture radius color segments]
              :or {segments 16}}]
  (let [dir (vec3/vnorm (vec3/v- end start))
        candidate (if (< (Math/abs (.-y ^V3 dir)) 0.9) vec3/unit-y vec3/unit-x)
        right (vec3/vnorm (vec3/vcross candidate dir))
        up (vec3/vnorm (vec3/vcross dir right))
        r (double (or radius 0.1))
        dtheta (/ (* 2.0 Math/PI) (double segments))
        tex (or texture default-beam-texture)]
    (vec
      (for [i (range segments)]
        (let [a (* (double i) dtheta)
              b (* (double (inc i)) dtheta)
              ua (vec3/v+ (vec3/v* right (Math/cos a))
                          (vec3/v* up (Math/sin a)))
              ub (vec3/v+ (vec3/v* right (Math/cos b))
                          (vec3/v* up (Math/sin b)))
              p0 (vec3/v+ start (vec3/v* ua r))
              p1 (vec3/v+ start (vec3/v* ub r))
              p2 (vec3/v+ end (vec3/v* ub r))
              p3 (vec3/v+ end (vec3/v* ua r))]
          (ru/quad-op tex p0 p1 p2 p3 color))))))

(defn life-ratio
  "Return ttl/max-ttl clamped only by denominator safety."
  [ttl max-ttl]
  (/ (double ttl) (double (max 1 max-ttl))))

(defn beam-ops
  "Build standard billboard beam ops from explicit render options."
  [cam-pos start end {:keys [width core-width core-ratio outer-color inner-color line-color texture]}]
  (ru/billboard-beam-ops cam-pos start end
    {:texture texture
     :width width
     :core-width core-width
     :core-ratio core-ratio
     :outer-color outer-color
     :inner-color inner-color
     :line-color line-color}))

(defn fading-beam-ops
  "Build beam ops from a beam state carrying :start/:end/:ttl/:max-ttl.

  Config callbacks receive `[beam life]` and return values for the corresponding
  billboard beam option. Constant values are also accepted."
  [cam-pos {:keys [start end ttl max-ttl] :as beam} {:keys [width core-width core-ratio outer-color inner-color line-color texture]}]
  (let [life (life-ratio ttl max-ttl)
        resolve-value (fn [value]
                        (if (fn? value) (value beam life) value))]
    (beam-ops cam-pos start end
      {:texture (resolve-value texture)
       :width (resolve-value width)
       :core-width (resolve-value core-width)
       :core-ratio (resolve-value core-ratio)
       :outer-color (resolve-value outer-color)
       :inner-color (resolve-value inner-color)
       :line-color (resolve-value line-color)})))
