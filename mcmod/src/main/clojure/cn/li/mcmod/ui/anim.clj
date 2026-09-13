(ns cn.li.mcmod.ui.anim
  "Animation/time math (pure functions, zero MC deps).
   Ported from overlay/renderer.clj and optimized.
   All state via cell arrays + partial (Iron Rule 13)."
  (:require [cn.li.mcmod.ui.signal :as sig]))

;; ============================================================================
;; smooth-mask-channel
;; ============================================================================

;; NOTE: primitive return hints MUST sit on the argvec (defn f ^double [...]),
;; not on the name — a name-position ^double makes the compiler pick the
;; Object-returning interface (IFn$DDDO) while emitting a double-returning
;; invokePrim, an inconsistent class that throws AbstractMethodError when
;; invoked through the interface.
(defn- smooth-mask-channel ^double [^double from ^double to ^double dt]
  (let [delta (- to from)]
    (if (<= (Math/abs delta) 0.001)
      to
      (+ from (* (Math/signum delta) (min (* 1.0 dt) (Math/abs delta)))))))

;; ============================================================================
;; smoothed — ComputedD wrapper
;; ============================================================================

(defn- smoothed-step ^double [^doubles cell ^double rate ^double target ^double now-ms]
  ;; smooth-toward inlined: a 4-double-arg primitive helper invoked from another
  ;; primitive fn triggers an IFn$DDDDO/invokePrim dispatch mismatch (Clojure
  ;; primitive-return interfaces don't cover 4 double args), so keep the step
  ;; local — all doubles, zero boxing.
  (let [dt (max 0.0 (/ (- now-ms (aget cell 1)) 1000.0))
        current (aget cell 0)
        delta (- target current)
        v (if (<= (Math/abs delta) 0.0005)
            target
            (+ current (* (Math/signum delta) (min (* rate dt) (Math/abs delta)))))]
    (aset cell 0 (double v))
    (aset cell 1 (double now-ms))
    v))

(defn smoothed
  [target-sig clock-ms-sig ^double rate]
  (let [cell (double-array 2)]
    (sig/computed-d [target-sig clock-ms-sig]
                    (partial smoothed-step cell rate))))

;; ============================================================================
;; smoothed-color — ComputedO wrapper (RGBA)
;; ============================================================================

(defn smoothed-color
  [target-sig clock-ms-sig]
  (let [cell (double-array 5)]
    (sig/computed-o [target-sig clock-ms-sig]
                    (fn smooth-color-step [target now-ms]
                      (let [^doubles c cell
                            dt (max 0.0 (/ (- (double now-ms) (aget c 4)) 1000.0))
                            r (smooth-mask-channel (aget c 0) (double (nth target 0 0.0)) dt)
                            g (smooth-mask-channel (aget c 1) (double (nth target 1 0.0)) dt)
                            b (smooth-mask-channel (aget c 2) (double (nth target 2 0.0)) dt)
                            a (smooth-mask-channel (aget c 3) (double (nth target 3 0.0)) dt)]
                        (aset c 0 r)
                        (aset c 1 g)
                        (aset c 2 b)
                        (aset c 3 a)
                        (aset c 4 (double now-ms))
                        [r g b a])))))

;; ============================================================================
;; breathe
;; ============================================================================

(defn- breathe-step ^double [^double period ^double lo ^double hi ^double now-ms]
  (+ lo (* (- hi lo) 0.5 (+ 1.0 (Math/sin (/ (* (double now-ms) 2.0 Math/PI) period))))))

(defn breathe
  [clock-ms-sig ^double period ^double lo ^double hi]
  (sig/computed-d [clock-ms-sig] (partial breathe-step period lo hi)))

;; ============================================================================
;; flicker-alpha
;; ============================================================================

(defn- flicker-alpha-step ^double [^double now-ms]
  (+ 0.725 (* 0.275 (Math/sin (* (double now-ms) 0.003)))))

(defn flicker-alpha
  [clock-ms-sig]
  (sig/computed-d [clock-ms-sig] flicker-alpha-step))

;; ============================================================================
;; interference jitter — faithful port of upstream CPBar's interference display
;; (CPBar.java lines ~114-158: 60 random keyframes built once in the
;; constructor; per frame the offset is the direction of the NEXT keyframe
;; after the quantized time input, and the master alpha flickers along a
;; Catmull-Rom curve over the same keyframe times)
;; ============================================================================

(defn build-interference-keyframes
  "Port of CPBar's interference keyframes: 60 frames, each 80-400ms after the
  previous; direction = (sin θ · n³ · 9 · aspect, cos θ · n³ · 9) with n
  uniform [0,1) CUBED (cubic bias toward small offsets) and θ uniform over the
  full circle. Returns {:frames [{:time :dx :dy} ...] :maxtime}."
  [^double aspect]
  (loop [i 0 sum (long 0) frames []]
    (if (>= i 60)
      {:frames frames :maxtime sum}
      (let [thistime (long (+ 80 (rand-int 321))) ;; 80..400 ms
            theta (* (rand) (* 2 Math/PI))
            n (rand)
            offset-norm (* n n n)                ;; n³ cubic bias
            sum' (long (+ sum thistime))]
        (recur (inc i) sum'
               (conj frames
                     {:time sum'
                      :dx (* (Math/sin theta) offset-norm 9.0 aspect)
                      :dy (* (Math/cos theta) offset-norm 9.0)}))))))

(defn build-interference-alpha-points
  "Port of CPBar's alphaCurve points: (0, 0.2-0.8) then (cumTime_i, 0.4-0.7)
  for each keyframe."
  [keyframes]
  (into [[0.0 (+ 0.2 (rand 0.6))]]
        (map (fn [{:keys [time]}] [time (+ 0.4 (rand 0.3))]) (:frames keyframes))))

(defn- quantized-time-input
  "Upstream: timeInput = (long)(absTime*1000) % maxtime, then /10*10 — the
  lowered precision produces the jagged effect."
  [^double now-ms maxtime]
  (let [ti (mod (long now-ms) (long maxtime))]
    (* (quot ti 10) 10)))

(defn interference-offset
  "Position offset at now-ms: the direction of the first keyframe whose time
  exceeds the quantized time input (upstream int_get) — the offset holds
  constant between keyframe boundaries. Returns [dx dy]."
  [^double now-ms keyframes]
  (let [time-input (quantized-time-input now-ms (:maxtime keyframes))
        frame (first (drop-while #(<= (:time %) time-input) (:frames keyframes)))]
    (if frame [(:dx frame) (:dy frame)] [0.0 0.0])))

(defn- catmull-rom-value
  "Catmull-Rom spline value at t over points [[t a] ...] sorted by t (lambdalib
  CubicCurve semantics; ends clamped)."
  [points t]
  (let [n (count points)
        t0 (first (first points))
        tn (first (last points))]
    (cond
      (<= t t0) (second (first points))
      (>= t tn) (second (last points))
      :else
      (let [idx (dec (count (take-while #(< (first %) t) points)))
            p0 (nth points (max 0 (dec idx)))
            p1 (nth points idx)
            p2 (nth points (min (dec n) (inc idx)))
            p3 (nth points (min (dec n) (+ 2 idx)))
            [t1 a1] p1
            [t2 a2] p2
            u (/ (- t t1) (- t2 t1))
            a0 (second p0)
            a3 (second p3)]
        (* 0.5 (+ (* 2 a1)
                  (* (- a2 a0) u)
                  (* (- (+ (* 2 a0) (* -5 a1) (* 4 a2)) a3) u u)
                  (* (- (+ (* -1 a0) (* 3 a1) (* -3 a2)) a3) u u u)))))))

(defn interference-alpha
  "Master-alpha flicker at now-ms (upstream mAlpha *= alphaCurve.valueAt)."
  [^double now-ms keyframes alpha-points]
  (catmull-rom-value alpha-points (quantized-time-input now-ms (:maxtime keyframes))))

;; ============================================================================
;; interp-color-stops (baked)
;; ============================================================================

(defn bake-color-stops
  [color-stops]
  (when (seq color-stops)
    (let [sorted (sort-by (fn [s] (double (:pct s 0.0))) color-stops)
          first-s (first sorted)
          last-s  (last sorted)
          first-pct (double (:pct first-s 0.0))
          last-pct  (double (:pct last-s 1.0))
          front-padded (if (> first-pct 0.0)
                         (cons {:pct 0.0 :r (:r first-s 1.0) :g (:g first-s 1.0) :b (:b first-s 1.0)} sorted)
                         sorted)
          padded (cond-> front-padded
                   (< last-pct 1.0)
                   (concat [{:pct 1.0 :r (:r last-s 1.0) :g (:g last-s 1.0) :b (:b last-s 1.0)}]))
          final (sort-by (fn [s] (double (:pct s 0.0))) padded)
          cnt (count final)
          arr (double-array (* 4 cnt))]
      (loop [i 0 remaining final]
        (when-let [s (first remaining)]
          (let [base (* i 4)]
            (aset arr base       (double (:pct s)))
            (aset arr (+ base 1) (double (:r s)))
            (aset arr (+ base 2) (double (:g s)))
            (aset arr (+ base 3) (double (:b s)))
            (recur (unchecked-inc-int i) (rest remaining)))))
      arr)))

(defn sample-color-stops
  "Find the segment [i-1,i] containing p and interpolate; clamp to the last
   stop once i reaches n (covers n=1 and p past the last stop)."
  [^doubles baked ^double pct]
  (when baked
    (let [n (quot (alength baked) 4)
          p (max 0.0 (min 1.0 pct))]
      (loop [i 1]
        (if (>= i n)
          (let [base (* (dec n) 4)]
            (doto (double-array 3)
              (aset 0 (aget baked (+ base 1)))
              (aset 1 (aget baked (+ base 2)))
              (aset 2 (aget baked (+ base 3)))))
          (let [stop-pct (aget baked (* i 4))]
            (if (<= p stop-pct)
              (let [i0 (dec i)
                    base0 (* i0 4)
                    base1 (* i 4)
                    p0 (aget baked base0)
                    p1 (aget baked base1)
                    t  (if (== p1 p0) 0.0 (/ (- p p0) (- p1 p0)))]
                (doto (double-array 3)
                  (aset 0 (+ (aget baked (+ base0 1)) (* t (- (aget baked (+ base1 1)) (aget baked (+ base0 1))))))
                  (aset 1 (+ (aget baked (+ base0 2)) (* t (- (aget baked (+ base1 2)) (aget baked (+ base0 2))))))
                  (aset 2 (+ (aget baked (+ base0 3)) (* t (- (aget baked (+ base1 3)) (aget baked (+ base0 3))))))))
              (recur (unchecked-inc-int i)))))))))
