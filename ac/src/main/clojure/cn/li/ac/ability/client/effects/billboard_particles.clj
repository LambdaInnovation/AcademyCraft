(ns cn.li.ac.ability.client.effects.billboard-particles
  "Stateful billboard particle machinery shared by effects that render
  LambdaLib-style textured particles (upstream Particle): per-particle state
  with drift velocity and the upstream fade-in / life / fade-out alpha
  curve, emitted as camera-facing textured quads through the level-effect
  renderer (:quad ops)."

  (:require [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.render-util :as ru]))

(defn particle-alpha
  "Upstream Particle.onUpdate alpha curve: fade-in over fadeInTime, full until
  life, then linear fade-out over fadeTime (dead past life + fadeTime)."
  [{:keys [age life fade-in fade-out start-alpha]}]
  (if (< age fade-in)
    (long (* start-alpha (/ (double age) (double fade-in))))
    (if (<= age life)
      start-alpha
      (long (* start-alpha
               (max 0.0 (- 1.0 (/ (double (- age life)) (double fade-out)))))))))

(defn tick-particles!
  "Advance one burst: drift by velocity (upstream Rigidbody), age; drop
  particles past life + fade-out."
  [particles]
  (reduce (fn [acc p]
            (let [age (inc (long (:age p)))]
              (if (>= age (+ (long (:life p)) (long (:fade-out p))))
                acc
                (conj acc (-> p
                              (assoc :age age)
                              (update :x + (double (:vx p)))
                              (update :y + (double (:vy p)))
                              (update :z + (double (:vz p))))))))
          []
          particles))

(defn particle-op
  "Camera-facing billboard quad (upstream ISpriteEntity: cullFace=false,
  always faces the player) carrying the particle's texture."
  [cam-pos p]
  (let [center (rv3/v3 (double (:x p)) (double (:y p)) (double (:z p)))
        right (ru/camera-facing-right-axis center cam-pos)
        up (ru/billboard-up-axis center cam-pos right)
        half (* 0.5 (double (:size p)))
        ro (rv3/v* right half)
        uo (rv3/v* up half)
        c+ro (rv3/v+ center ro)
        c-ro (rv3/v- center ro)]
    (ru/quad-op (:texture p)
                (rv3/v+ c-ro uo) (rv3/v- c-ro uo)
                (rv3/v- c+ro uo) (rv3/v+ c+ro uo)
                (assoc (or (:color p) {:r 255 :g 255 :b 255})
                       :a (particle-alpha p)))))

(defn particle-ops
  "Billboard quads for a whole burst."
  [cam-pos particles]
  (mapv #(particle-op cam-pos %) particles))
