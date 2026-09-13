(ns cn.li.ac.ability.effects.geom
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.util.math.vec3 :as vec3]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- runtime-player-state
  [player-id]
  (store/get-player-state (runtime-hooks/require-player-state-session-id "geom") player-id))

(defn- runtime-player-state-in-session
  [session-id player-id]
  (store/get-player-state session-id player-id))

(declare world-id-of-in-session
         eye-pos-in-session
         body-pos-in-session)

;; ---------------------------------------------------------------------------
;; Player helpers
;; ---------------------------------------------------------------------------

(defn world-id-of
  "Return world-id (dimension string) for `player-id` from live Minecraft entity."
  [player-id]
  (if (raycast/available?)
    (if-let [pos (raycast/player-position player-id)]
      (or (:world-id pos) "minecraft:overworld")
      "minecraft:overworld")
    (world-id-of-in-session (runtime-hooks/require-player-state-session-id "geom") player-id)))

(defn world-id-of-in-session
  [session-id player-id]
  (or (get-in (runtime-player-state-in-session session-id player-id) [:position :world-id])
      "minecraft:overworld"))

(defn eye-pos
  "Return {:x :y :z} eye position from live Minecraft player entity."
  [player-id]
  (if (raycast/available?)
    (if-let [pos (raycast/player-position player-id)]
      {:x (double (:x pos))
       :y (double (:eye-y pos))
       :z (double (:z pos))}
      {:x 0.0 :y 65.62 :z 0.0})
    (eye-pos-in-session (runtime-hooks/require-player-state-session-id "geom") player-id)))

(defn eye-pos-in-session
  [session-id player-id]
  (let [pos (get-in (runtime-player-state-in-session session-id player-id) [:position])]
    {:x (double (or (:x pos) 0.0))
     :y (+ (double (or (:y pos) 64.0)) 1.62)
     :z (double (or (:z pos) 0.0))}))

(defn body-pos
  "Return {:x :y :z} body (feet) position from live Minecraft player entity."
  [player-id]
  (if (raycast/available?)
    (if-let [pos (raycast/player-position player-id)]
      {:x (double (:x pos))
       :y (double (:y pos))
       :z (double (:z pos))}
      {:x 0.0 :y 64.0 :z 0.0})
    (body-pos-in-session (runtime-hooks/require-player-state-session-id "geom") player-id)))

(defn body-pos-in-session
  [session-id player-id]
  (let [pos (get-in (runtime-player-state-in-session session-id player-id) [:position])]
    {:x (double (or (:x pos) 0.0))
     :y (double (or (:y pos) 64.0))
     :z (double (or (:z pos) 0.0))}))

;; ---------------------------------------------------------------------------
;; 3-D vector math re-exported from shared vec3 helpers
;; ---------------------------------------------------------------------------

(def v+ vec3/v+)
(def v- vec3/v-)
(def v* vec3/v*)
(def vdot vec3/vdot)
(def vlen vec3/vlen)
(def vnorm vec3/vnorm)
(def vcross vec3/vcross)
(def orthonormal-basis vec3/orthonormal-basis)
(def vdist vec3/vdist)
(def vdist-sq vec3/vdist-sq)

(defn floor-int
  [value]
  (int (Math/floor (double value))))

(def rotate-around-axis vec3/rotate-around-axis)



