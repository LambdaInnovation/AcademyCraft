(ns cn.li.ac.ability.client.effects.cubic-curve
  "Port of LambdaLib's cn.lambdalib2.vis.curve.CubicCurve.

  A cubic Hermite through the control points, with finite-difference tangents:
  interior points take the average of the two neighbouring secants, the ends
  take their single secant, and past the last point it extrapolates on the
  final secant. Several upstream effects shape themselves with one of these
  (WaveEffectRenderer's alpha and size ramps), and a piecewise-linear stand-in
  reads visibly flatter through the middle.

  No Minecraft imports.")

(defn curve
  "Build a curve from [[x y] ...] control points. Points are sorted by x, as
  addPoint does."
  [points]
  (vec (sort-by first (map (fn [[x y]] [(double x) (double y)]) points))))

(defn- secant
  ^double [pts i1 i2]
  (let [[x1 y1] (nth pts i1)
        [x2 y2] (nth pts i2)]
    (/ (- (double y2) (double y1))
       (- (double x2) (double x1)))))

(defn- tangent
  "k(i, l): the finite-difference slope at point i, scaled by the segment
  length l — Hermite tangents are expressed per unit of t, not of x."
  ^double [pts i ^double l]
  (let [n (count pts)
        raw (cond
              (zero? i) (if (= n 1) 0.0 (secant pts i (inc i)))
              (= i (dec n)) (secant pts i (dec i))
              :else (* 0.5 (+ (secant pts (inc i) i)
                              (secant pts i (dec i)))))]
    (* raw l)))

(defn value-at
  ^double [pts ^double x]
  (let [n (count pts)]
    (if (zero? n)
      0.0
      (let [index (loop [i 0]
                    (if (and (< i n) (< (double (first (nth pts i))) x))
                      (recur (inc i))
                      i))]
        (cond
          (= index n)
          (let [[px py] (nth pts (dec n))
                k (if (>= n 2) (secant pts (dec n) (- n 2)) 0.0)]
            (+ (double py) (* (- x (double px)) k)))

          (zero? index)
          (let [[px py] (nth pts 0)]
            (+ (double py) (* (tangent pts 0 1.0) (- x (double px)))))

          :else
          (let [[x0 y0] (nth pts (dec index))
                [x1 y1] (nth pts index)
                l (- (double x1) (double x0))
                t (/ (- x (double x0)) l)
                t2 (* t t)
                t3 (* t2 t)
                m0 (tangent pts (dec index) l)
                m1 (tangent pts index l)]
            (+ (* t3 (+ m0 m1 (* 2.0 (double y0)) (* -2.0 (double y1))))
               (* t2 (+ (* -2.0 m0) (- m1) (* -3.0 (double y0)) (* 3.0 (double y1))))
               (* t m0)
               (double y0))))))))
