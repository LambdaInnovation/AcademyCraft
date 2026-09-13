(ns cn.li.ac.util.math.vec3
  "Shared 3-D vector math helpers operating on {:x :y :z} maps.")

(defn v+
  [a b]
  {:x (+ (double (:x a)) (double (:x b)))
   :y (+ (double (:y a)) (double (:y b)))
   :z (+ (double (:z a)) (double (:z b)))})

(defn v-
  [a b]
  {:x (- (double (:x a)) (double (:x b)))
   :y (- (double (:y a)) (double (:y b)))
   :z (- (double (:z a)) (double (:z b)))})

(defn v*
  [a scale]
  {:x (* (double (:x a)) (double scale))
   :y (* (double (:y a)) (double scale))
   :z (* (double (:z a)) (double scale))})

(defn vdot
  ^double [a b]
  (+ (* (double (:x a)) (double (:x b)))
     (* (double (:y a)) (double (:y b)))
     (* (double (:z a)) (double (:z b)))))

(defn vlen
  ^double [a]
  (Math/sqrt (vdot a a)))

(defn vnorm
  [a]
  (let [len (max 1.0e-6 (vlen a))]
    (v* a (/ 1.0 len))))

(defn vcross
  [a b]
  {:x (- (* (double (:y a)) (double (:z b))) (* (double (:z a)) (double (:y b))))
   :y (- (* (double (:z a)) (double (:x b))) (* (double (:x a)) (double (:z b))))
   :z (- (* (double (:x a)) (double (:y b))) (* (double (:y a)) (double (:x b))))})

(defn orthonormal-basis
  "Return [right up] orthonormal vectors perpendicular to dir."
  [dir]
  (let [up-axis (if (> (Math/abs (double (:y dir))) 0.95)
                  {:x 1.0 :y 0.0 :z 0.0}
                  {:x 0.0 :y 1.0 :z 0.0})
        right (vnorm (vcross dir up-axis))
        up (vnorm (vcross right dir))]
    [right up]))

(defn vdist
  [a b]
  (vlen (v- a b)))

(defn vdist-sq
  [a b]
  (let [dx (- (double (:x a)) (double (:x b)))
        dy (- (double (:y a)) (double (:y b)))
        dz (- (double (:z a)) (double (:z b)))]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn rotate-around-axis
  "Rotate vec around axis by degrees."
  [v axis degrees]
  (let [axis-unit (vnorm axis)
        theta (Math/toRadians (double degrees))
        cos-t (Math/cos theta)
        sin-t (Math/sin theta)
        term1 (v* v cos-t)
        term2 (v* (vcross axis-unit v) sin-t)
        term3 (v* axis-unit (* (vdot axis-unit v) (- 1.0 cos-t)))]
    (vnorm (v+ (v+ term1 term2) term3))))

(defn euclidean-distance-sq
  [x1 y1 z1 x2 y2 z2]
  (let [dx (- (double x2) (double x1))
        dy (- (double y2) (double y1))
        dz (- (double z2) (double z1))]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn euclidean-distance
  [x1 y1 z1 x2 y2 z2]
  (Math/sqrt (euclidean-distance-sq x1 y1 z1 x2 y2 z2)))
