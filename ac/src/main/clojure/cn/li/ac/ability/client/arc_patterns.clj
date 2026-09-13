(ns cn.li.ac.ability.client.arc-patterns
  "Zigzag lightning arc patterns + wiggle animation matching original
  AcademyCraft ArcPatterns (weakArc, strongArc, aoeArc, chargingArc,
  thinContiniousArc) and EntityArc texWiggle/showWiggle/hideWiggle.

  Each pattern defines the look of one arc type:
    :segments     — zigzag subdivision count (more = smoother but heavier)
    :amplitude    — max perpendicular deviation from straight line (0-1)
    :width        — total arc billboard width in world units
    :core-ratio   — inner core width / total width
    :tex-wiggle   — texture UV wiggle amplitude (sin-based)
    :show-wiggle  — apperance-phase wiggle start value
    :hide-wiggle  — disappearance-phase wiggle end value
    :life-ticks   — default lifetime in ticks
    :color-outer  — outer glow color {:r :g :b}
    :color-inner  — inner core color {:r :g :b}
    :color-line   — center line color {:r :g :b}
    :flicker      — probability arc is visible this frame (1.0 = always)
    :fork-count   — max number of side branches
    :fork-length  — fraction of main beam for fork reach
    :fork-angle   — radians, max deviation from beam axis"
  (:require [cn.li.ac.ability.client.effects.rv3 :as v]))

;; ============================================================================
;; ArcPattern presets — matching original AcademyCraft ArcPatterns constants
;; ============================================================================

(def arc-patterns
  {:weak
   {:name          "weakArc"
    :segments      24        ;; dense zigzag for lightning look (was 8)
    :amplitude     0.12      ;; matching original offset ratio
    :width         0.1       ;; matching original ArcFactory.width = 0.1
    :core-ratio    0.45
    ;; EntityArc defaults (showWiggle/hideWiggle 0.2, texWiggle 0.5) — the
    ;; ~50% duty flicker + per-tick template re-roll are the upstream bolt's
    ;; crackle; the port's earlier guesses (0.1/0.4, 0.7) froze the shape.
    :tex-wiggle    0.5
    :show-wiggle   0.2
    :hide-wiggle   0.2
    :life-ticks    10
    ;; Original uses glColor4d(1,1,1,alpha) — pure white
    :color-outer   {:r 255 :g 255 :b 255}
    :color-inner   {:r 255 :g 255 :b 255}
    :color-line    {:r 255 :g 255 :b 255}
    :flicker       1.0
    :fork-count    0
    :fork-length   0.5
    :fork-angle    0.5}

   ;; Original ArcPatterns.strongArc: branchFactor 0.3, passes 5, width 0.3,
   ;; maxOffset 1.4, generated over a 20-block reference length. maxOffset is
   ;; an absolute offset there and :amplitude here is a FRACTION of the arc's
   ;; length, so 1.4/20 = 0.07; segments 20 gives ceil(log2 20) = 5 passes.
   ;; The port's own numbers (width 0.07, amplitude 0.25) made thunder bolt's
   ;; main arc a quarter as thick and three times as scattered as the original.
   :strong
   {:name          "strongArc"
    :segments      20
    :amplitude     0.07
    :width         0.3
    :core-ratio    0.4
    :tex-wiggle    0.5
    :show-wiggle   0.2
    :hide-wiggle   0.2
    :life-ticks    20
    :color-outer   {:r 130 :g 210 :b 255}
    :color-inner   {:r 230 :g 245 :b 255}
    :color-line    {:r 200 :g 240 :b 255}
    :flicker       0.95
    ;; ArcFactory branches are short offshoots of a local segment, not
    ;; secondary bolts spanning half the arc (:fork-length is a fraction of
    ;; the whole beam) — same correction already applied to :charging.
    :fork-count    2
    :fork-length   0.08
    :fork-angle    0.17}

   ;; Original ArcPatterns.aoeArc: branchFactor 0.28, passes 5, width 0.13,
   ;; maxOffset 1.2 over the same 20-block reference — 1.2/20 = 0.06.
   :aoe
   {:name          "aoeArc"
    :segments      20
    :amplitude     0.06
    :width         0.13
    :core-ratio    0.4
    :tex-wiggle    0.5
    :show-wiggle   0.2
    :hide-wiggle   0.2
    :life-ticks    20                      ;; ranged 15-25 in original
    :color-outer   {:r 120 :g 200 :b 255}
    :color-inner   {:r 220 :g 240 :b 255}
    :color-line    {:r 190 :g 230 :b 255}
    :flicker       0.9
    :fork-count    2
    :fork-length   0.08
    :fork-angle    0.17}

   :charging
   {:name          "chargingArc"
    :segments      16
    :amplitude     0.3
    :width         0.06
    :core-ratio    0.4
    :tex-wiggle    0.8
    :show-wiggle   0.2
    :hide-wiggle   0.8
    :life-ticks    100000                   ;; effectively infinite (manual stop)
    :color-outer   {:r 140 :g 220 :b 255}
    :color-inner   {:r 240 :g 250 :b 255}
    :color-line    {:r 210 :g 245 :b 255}
    :flicker       0.85
    :fork-count    2
    :fork-length   0.4
    :fork-angle    0.4}

   :thin-continuous
   {:name          "thinContiniousArc"
    :segments      20
    :amplitude     0.1
    :width         0.03
    :core-ratio    0.45
    :tex-wiggle    1.0
    :show-wiggle   0.1
    :hide-wiggle   0.6
    :life-ticks    100000                   ;; effectively infinite
    :color-outer   {:r 100 :g 180 :b 255}
    :color-inner   {:r 200 :g 230 :b 255}
    :color-line    {:r 170 :g 215 :b 255}
    :flicker       0.9
    :fork-count    0
    :fork-length   0.4
    :fork-angle    0.3}

   :railgun
   {:name          "railgunArc"
    :segments      12
    :amplitude     0.35
    :width         0.10
    :core-ratio    0.35
    :tex-wiggle    0.4
    :show-wiggle   0.2
    :hide-wiggle   0.2
    :life-ticks    30
    :color-outer   {:r 160 :g 230 :b 255}
    :color-inner   {:r 255 :g 255 :b 255}
    :color-line    {:r 220 :g 250 :b 255}
    :flicker       0.98
    :fork-count    4
    :fork-length   0.55
    :fork-angle    0.7}})

(defn get-pattern
  "Look up a named arc pattern. Falls back to :weak."
  [pattern-key]
  (get arc-patterns pattern-key (get arc-patterns :weak)))

;; ============================================================================
;; Zigzag path generation
;; ============================================================================

(defn generate-zigzag-segments
  "Generate zigzag vertex path from `start` to `end` (both ^V3).
  Returns vector of {:pos ^V3 :u :v} entries forming the arc path.

  Uses the original ArcFactory algorithm — recursive midpoint displacement.
  Each pass splits every segment at its midpoint and pushes that midpoint by a
  random distance (up to the current pass offset) in the plane perpendicular
  to the beam, then halves the offset for the next pass. Both endpoints stay
  anchored, so the bolt leaves the caster exactly at `start` and converges on
  `end`, with the deviation concentrated in the middle.

  This is what makes the result read as one bolt: sampling every vertex from
  an independent full-amplitude distribution instead (the previous
  implementation) produces uncorrelated neighbours — a hairball rather than a
  path — and, because the deviation near `start` is then just as large as in
  the middle, the first segments land far off the beam axis. Fired from the
  eye that puts them outside the field of view entirely, leaving only the far
  end of the arc (near the target) on screen.

  Deterministic given (start, end, pattern segments/amplitude, seed) — no
  camera/time dependence — so callers precompute this once when an arc is
  enqueued (its shape is fixed for the arc's lifetime) rather than every
  frame.

  `segments` is a target subdivision count, rounded up to the next power of
  two to give the pass count. `amplitude` is the pass-0 midpoint offset as a
  fraction of beam length (original: maxOffset 1.1 on its 20-block reference
  arc). Each vertex has UV coords (u=0.0-1.0 along beam, v across width)."
  [^cn.li.mcmod.math.V3 start ^cn.li.mcmod.math.V3 end
   {:keys [segments amplitude seed]
    :or {segments 8 amplitude 0.15 seed 42}}]
  (let [end-start (v/v- end start)
        dir (v/vnorm end-start)
        len (v/vlen end-start)
        ;; Compute perpendicular axis (any axis perpendicular to dir works)
        up (if (< (Math/abs (.-y ^cn.li.mcmod.math.V3 dir)) 0.99) v/unit-y v/unit-x)
        perp (v/vnorm (v/vcross dir up))
        perp2 (v/vnorm (v/vcross dir perp))
        passes (max 1 (int (Math/ceil (/ (Math/log (max 2.0 (double segments)))
                                         (Math/log 2.0)))))
        ;; One Random drawn in a fixed traversal order keeps the whole path
        ;; reproducible from `seed` alone.
        rng (java.util.Random. (long seed))
        displace (fn [magnitude]
                   ;; Same draw as the original: random direction across the
                   ;; plane perpendicular to the beam, random radius within
                   ;; this pass's offset.
                   (let [theta (* 2.0 Math/PI (.nextDouble rng))
                         r (* (double magnitude) (.nextDouble rng))]
                     (v/v+ (v/v* perp (* r (Math/sin theta)))
                           (v/v* perp2 (* r (Math/cos theta))))))]
    ;; Points carry [t offset]: `t` walks the straight beam, `offset` stays
    ;; perpendicular to it, so the path never doubles back along the axis.
    (loop [pass 0
           pts [[0.0 v/zero] [1.0 v/zero]]]
      (if (>= pass passes)
        (mapv (fn [[t off]]
                {:pos (v/v+ (v/v+ start (v/v* end-start (double t))) off)
                 :u (double t)
                 :v 0.0})
              pts)
        (let [magnitude (* (double amplitude) len (Math/pow 0.5 pass))]
          (recur (inc pass)
                 (into [(first pts)]
                       (mapcat (fn [[[t0 off0] [t1 off1]]]
                                 (let [t-mid (* 0.5 (+ (double t0) (double t1)))
                                       off-mid (v/v+ (v/v* (v/v+ off0 off1) 0.5)
                                                     (displace magnitude))]
                                   [[t-mid off-mid] [t1 off1]]))
                               (partition 2 1 pts)))))))))

;; ============================================================================
;; L-system recursive arc generation (matching original ArcFactory)
;; ============================================================================

(defn- rand-perp-offset
  "Random offset vector in the plane perpendicular to the beam direction."
  [^cn.li.mcmod.math.V3 perp ^cn.li.mcmod.math.V3 perp2 magnitude]
  (let [theta (* 2.0 Math/PI (rand))
        r (* magnitude (rand))]
    (v/v+ (v/v* perp (* r (Math/sin theta)))
          (v/v* perp2 (* r (Math/cos theta))))))

(defn- random-rotate-small
  "Rotate a direction vector by a small random angle (radians), matching
  original ArcFactory.randomRotate(10, dir) — ±10° ≈ ±0.17 rad."
  [^cn.li.mcmod.math.V3 dir perp perp2 max-angle]
  (let [angle (* max-angle (- (* 2.0 (rand)) 1.0))
        theta (* 2.0 Math/PI (rand))
        rot-vec (v/v+ (v/v* perp (Math/sin theta))
                      (v/v* perp2 (Math/cos theta)))]
    (v/vnorm (v/v+ dir (v/v* rot-vec (Math/sin angle))))))

(defn generate-lsystem-segments
  "Generate arc segments using L-system recursive subdivision.

  Parameters are SCALED by arc length to match the original's visual density
  (original generated for 20-block reference length):
    - passes: scaled so segment density ≈ 3.2 segments/block
    - max-offset: scaled proportionally (original 1.1 / 20 length)
    - branch length: proportional to parent segment (not total arc)

  Returns vector of segment maps:
    {:start-pos ^V3  :end-pos ^V3
     :start-width double  :end-width double
     :alpha double}"
  [^cn.li.mcmod.math.V3 start ^cn.li.mcmod.math.V3 end
   {:keys [width lsystem-passes lsystem-max-offset
           lsystem-branch-factor lsystem-width-shrink
           lsystem-alpha-shrink]
    :or {width 0.1 lsystem-passes 6 lsystem-max-offset 1.1
         lsystem-branch-factor 0.15 lsystem-width-shrink 0.7
         lsystem-alpha-shrink 0.9}}]
  (let [dir (v/vnorm (v/v- end start))
        total-len (v/vlen (v/v- end start))
        ;; Scale parameters to actual arc length.
        ;; Original reference: 20 blocks, 6 passes, offset 1.1.
        ref-len 20.0
        len-ratio (/ (max 1.0 total-len) ref-len)
        scaled-offset (* lsystem-max-offset len-ratio)
        ;; Segment density: original 64 seg / 20 blk = 3.2/blk
        seg-target (max 2.0 (* total-len 3.2))
        effective-passes (max 2 (min lsystem-passes
                                     (int (Math/ceil (/ (Math/log seg-target)
                                                       (Math/log 2.0))))))
        ;; Local coordinate frame
        up (if (< (Math/abs (.-y ^cn.li.mcmod.math.V3 dir)) 0.99)
             v/unit-y v/unit-x)
        perp (v/vnorm (v/vcross dir up))
        perp2 (v/vnorm (v/vcross dir perp))
        segments (java.util.ArrayList.)]
    (letfn [(subdivide [^cn.li.mcmod.math.V3 s ^cn.li.mcmod.math.V3 e
                        sw ew alpha depth
                        s-perp s-perp2]
              (if (>= depth effective-passes)
                (.add segments {:start-pos s :end-pos e
                                :start-width sw :end-width ew
                                :alpha alpha})
                (let [seg-dir (v/vnorm (v/v- e s))
                      seg-len (v/vlen (v/v- e s))
                      mid (v/v* (v/v+ s e) 0.5)
                      mw (/ (+ sw ew) 2.0)
                      ;; Offset per pass (matching original: offset /= 2 each pass,
                      ;; independent of current segment length)
                      offset-scale (* scaled-offset
                                     (Math/pow 0.5 (double depth)))
                      offset (rand-perp-offset perp perp2 offset-scale)
                      mid' (v/v+ mid offset)
                      ;; Local perpendiculars for this segment
                      seg-up (if (< (Math/abs (.-y ^cn.li.mcmod.math.V3 seg-dir)) 0.99)
                               v/unit-y v/unit-x)
                      seg-perp (v/vnorm (v/vcross seg-dir seg-up))
                      seg-perp2 (v/vnorm (v/vcross seg-dir seg-perp))]
                  ;; Two child segments
                  (subdivide s mid' sw mw alpha (inc depth) seg-perp seg-perp2)
                  (subdivide mid' e mw ew alpha (inc depth) seg-perp seg-perp2)
                  ;; Branch: direction ≈ parent segment direction, ±10°
                  ;; length proportional to parent segment (matching original:
                  ;; multiply(subtract(ave, s.start), lengthShrink))
                  (when (< (rand) lsystem-branch-factor)
                    (let [br-dir (random-rotate-small seg-dir seg-perp seg-perp2 0.17)
                          br-len (* seg-len 0.5 lsystem-width-shrink)
                          br-end (v/v+ mid' (v/v* br-dir br-len))
                          bw (* mw lsystem-width-shrink)
                          ba (* alpha lsystem-alpha-shrink)]
                      (subdivide mid' br-end mw bw ba (inc depth)
                                seg-perp seg-perp2))))))]
      (subdivide start end width width 1.0 0 perp perp2))
    (vec segments)))

(defn generate-lsystem-variants
  "Pre-generate N L-system arc variants (different random seeds → different
  lightning shapes).  Matching original ArcPatterns static GEN = 20."
  [n start end pattern]
  (mapv (fn [_] (generate-lsystem-segments start end pattern))
        (range n)))

;; ============================================================================
;; Wiggle animation state
;; ============================================================================

(defn- current-wiggle-time []
  (/ (double (System/currentTimeMillis)) 1000.0))

(defn wiggle-phase
  "Current global wiggle phase value, computed from wall-clock time.
  Integral of: 0.15 * (1 + 0.5*sin(3t))
  P(t) = 0.15*t + 0.025*(1 - cos(3t))"
  []
  (let [t (current-wiggle-time)]
    (+ (* 0.15 t)
       (* 0.025 (- 1.0 (Math/cos (* 3.0 t)))))))

(defn- lerp [a b t]
  (+ a (* t (- b a))))

(defn effective-wiggle-amount
  "Wiggle amplitude for `pattern` at `life-ratio` (the showWiggle/hideWiggle
  transition envelope, matching original EntityArc):
    - First 15% of life: ramps from showWiggle → texWiggle
    - Middle 55%: stays at texWiggle
    - Last 30%: ramps from texWiggle → hideWiggle

  Depends only on life-ratio (tick-rate), not on wall-clock time — callers
  should compute this once per arc per plan build, not once per segment."
  [pattern life-ratio]
  (let [show-phase  (min 1.0 (/ (max 0.0 (double life-ratio)) 0.15))
        hide-phase  (max 0.0 (/ (- (max 0.0 (double life-ratio)) 0.7) 0.3))
        tex-wiggle  (double (or (:tex-wiggle pattern) 0.5))
        show-wiggle (double (or (:show-wiggle pattern) 0.1))
        hide-wiggle (double (or (:hide-wiggle pattern) 0.4))]
    (cond
      (> hide-phase 0.0) (lerp tex-wiggle hide-wiggle (min 1.0 hide-phase))
      (< show-phase 1.0) (lerp show-wiggle tex-wiggle show-phase)
      :else tex-wiggle)))

(defn current-wiggle-offset
  "Compute UV wiggle offset for an arc segment based on pattern and life ratio.

  life-ratio: 0.0 (just spawned) to 1.0 (about to die).

  Returns wiggle offset value to add to UV u-coordinate."
  [pattern life-ratio]
  (let [effective-wiggle (effective-wiggle-amount pattern life-ratio)
        phase            (wiggle-phase)
        segment-idx      0]  ;; caller provides this per-segment
    (* effective-wiggle (Math/sin (+ phase (* segment-idx 0.5))))))

(defn segment-wiggle-offset
  "UV wiggle offset for a specific segment along the arc.
  segment-t: position along arc 0.0-1.0
  pattern: arc pattern map
  life-ratio: 0.0-1.0"
  [segment-t pattern life-ratio]
  (let [effective-wiggle (effective-wiggle-amount pattern life-ratio)
        phase            (wiggle-phase)
        ;; Each segment along the arc has a different wiggle phase
        seg-phase        (+ phase (* segment-t 3.0))]
    (* effective-wiggle (Math/sin seg-phase))))

;; ============================================================================
;; Color helpers
;; ============================================================================

(defn pattern-color
  "Get a color from pattern with alpha applied.
  Returns RGBA integer suitable for render ops."
  [pattern color-key alpha]
  (let [{:keys [r g b]} (get pattern color-key {:r 255 :g 255 :b 255})
        a (int (max 0 (min 255 (long alpha))))]
    (unchecked-int (bit-or (bit-shift-left a 24)
                           (bit-shift-left (int r) 16)
                           (bit-shift-left (int g) 8)
                           (int b)))))

(defn life-fade-alpha
  "Compute fade alpha based on life ratio.
  Matching original EntityArc alpha curve:
    - Sharp fade-in over first 20% of life
    - Full brightness for middle 60%
    - Gradual fade-out over last 20%"
  [base-alpha life-ratio]
  (let [lr (max 0.0 (min 1.0 (double life-ratio)))
        fade-in  (min 1.0 (/ lr 0.2))
        fade-out (min 1.0 (/ (- 1.0 lr) 0.2))
        alpha-factor (* fade-in fade-out)
        raw-alpha (* (double base-alpha) alpha-factor)]
    (int (max 0 (min 255 (long raw-alpha))))))
