(ns cn.li.ac.ability.client.effects.arc-fx
  "L-system lightning arc generation for beam effects.
  Ported from original AcademyCraft ArcFactory.java + SubArcHandler.java."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]))

;; ---------------------------------------------------------------------------
;; 3D vector helpers — generic ops delegate to the render-pipeline V3 library
;; (rv3, not the map-based util.math.vec3 — see rv3.clj docstring); yaw/pitch
;; rotation is arc_fx-specific, kept local.
;; ---------------------------------------------------------------------------

(def ^:private v+ vec3/v+)
(def ^:private v- vec3/v-)
(def ^:private v* vec3/v*)
(def ^:private v-cross vec3/vcross)
(def ^:private v-length vec3/vlen)
(def ^:private v-normalize vec3/vnorm)

(defn- v-rotate-yaw [^cn.li.mcmod.math.V3 a rad]
  (let [cos (Math/cos rad) sin (Math/sin rad)]
    (vec3/v3 (+ (* (.-x a) cos) (* (.-z a) sin))
             (.-y a)
             (- (* (.-z a) cos) (* (.-x a) sin)))))

(defn- v-rotate-pitch [^cn.li.mcmod.math.V3 a rad]
  (let [cos (Math/cos rad) sin (Math/sin rad)]
    (vec3/v3 (.-x a)
             (- (* (.-y a) cos) (* (.-z a) sin))
             (+ (* (.-z a) cos) (* (.-y a) sin)))))

(defn- random-rotate
  "Randomly rotate a vector within range degrees (matching original randomRotate).

  Upstream: `a = rangef(-range, range) / 180 * PI`. This read
  `(/ deg 180.0 Math/PI)` — dividing by pi where the degree-to-radian
  conversion multiplies — so every rotation came out pi^2 (~9.9x) too small.
  Branch directions barely deviated from their parent, and the random
  perpendicular segment->quads derives for billboarding was nearly parallel
  to the segment it came from, leaving the cross product close to zero."
  [range-deg dir]
  (let [a-rad (* (/ (* range-deg (rand)) 180.0) Math/PI)
        pitched (v-rotate-pitch dir (- (* (rand) 2 a-rad) a-rad))]
    (v-rotate-yaw pitched (- (* (rand) 2 a-rad) a-rad))))

(defn- random-dir-orthogonal
  "Generate a random direction in the plane orthogonal to the given vector."
  [^cn.li.mcmod.math.V3 dir offset]
  (let [theta (* (rand) 2.0 Math/PI)
        sin-theta (Math/sin theta)
        cos-theta (Math/cos theta)
        ;; Find two orthogonal basis vectors
        candidate (if (< (Math/abs (.-x dir)) 0.9) vec3/unit-x vec3/unit-y)
        u (v-normalize (v-cross dir candidate))
        w (v-normalize (v-cross dir u))]
    (v+ (v* u (* offset sin-theta))
        (v* w (* offset cos-theta)))))

;; ---------------------------------------------------------------------------
;; Arc segment data types
;; {:start {:pos V3 :width w} :end {:pos V3 :width w} :alpha}
;; ---------------------------------------------------------------------------

(defn- point [pos w] {:pos pos :width w})
(defn- point-avg [p1 p2]
  {:pos (v* (v+ (:pos p1) (:pos p2)) 0.5)
   :width (* 0.5 (+ (:width p1 0.1) (:width p2 0.1)))})

(defn- segment [start end alpha]
  {:start (if (:pos start) start (point start 0.1))
   :end   (if (:pos end)   end   (point end 0.1))
   :alpha alpha})

;; ---------------------------------------------------------------------------
;; L-system arc generation (matching original ArcFactory)
;; ---------------------------------------------------------------------------

(defn- handle-single-pass
  "Process one L-system pass over every arc: split each segment at a displaced
  midpoint, and fork a new arc with probability branch-factor.
  Returns {:arcs refined-arcs :branches new-arcs}."
  [arcs offset width-shrink alpha-shrink length-shrink branch-factor]
  (let [branches (transient [])
        refined
        (mapv (fn [segments]
                (vec
                 (mapcat (fn [s]
                           (let [ave (point-avg (:start s) (:end s))
                                 dir-vec (v- (:pos (:end s)) (:pos (:start s)))
                                 displace-dir (random-dir-orthogonal
                                               (v-normalize dir-vec)
                                               (* (rand) offset))
                                 ave-pt (point (v+ (:pos ave) displace-dir) (:width ave))
                                 s1 (assoc s :end ave-pt)
                                 s2 (segment ave-pt (:end s) (:alpha s))]
                             ;; Branching
                             (when (< (rand) branch-factor)
                               ;; Upstream forks along `ave.pt - s.start.pt`,
                               ;; i.e. the displaced HALF segment. Using the
                               ;; whole segment made every branch twice as
                               ;; long as intended.
                               (let [dir (v* (v- (:pos ave-pt) (:pos (:start s)))
                                             length-shrink)
                                     rdir (random-rotate 10.0 dir)
                                     w2 (* (:width ave-pt) width-shrink)
                                     bp1 (point (:pos ave-pt) w2)
                                     bp2 (point (v+ (:pos ave-pt) rdir) w2)]
                                 (conj! branches [(segment bp1 bp2 (* (:alpha s) alpha-shrink))])))
                             [s1 s2]))
                         segments)))
              arcs)]
    {:arcs refined :branches (persistent! branches)}))

(defn- generate-arc-segments
  "Generate L-system arc segments for a single arc template.
  Returns a vector of segment-lists (main trunk + all branches).

  Upstream ArcFactory keeps listAll and bufferAll as PARALLEL per-arc
  ping-pong buffers — index j of each is one arc's read/write pair, every arc
  is refined on every pass, and a fork appends a new arc to both. This port
  had collapsed them into two flat lists and, on the passes where it refined
  the branch list, handed the previous *unrefined* branch list back as the
  trunk. The trunk was therefore discarded after the first such pass, and a
  run that happened to fork nothing returned no segments at all — verified:
  with branch-factor 0 it produced zero.

  The ping-pong is a Java allocation trick; refining into fresh vectors needs
  no buffer swap, so passes here just refine every arc and append new forks."
  [length width max-offset passes branch-factor width-shrink]
  (let [v0 (vec3/v3 0.0 0.0 0.0)
        v1 (vec3/v3 length 0.0 0.0)
        init [(segment (point v0 width) (point v1 width) 1.0)]
        alpha-shrink 0.9
        length-shrink 0.7]
    (loop [arcs [init]
           offset max-offset
           pass 0]
      (if (>= pass passes)
        arcs
        (let [{:keys [arcs branches]}
              (handle-single-pass arcs offset width-shrink alpha-shrink
                                  length-shrink branch-factor)]
          (recur (into arcs branches)
                 (/ offset 2.0)
                 (inc pass)))))))

;; ---------------------------------------------------------------------------
;; Arc template cache (generated once, reused each frame)
;; ---------------------------------------------------------------------------

(def ^:private default-num-templates 15)
(def ^:private default-arc-length 1.5)
(def ^:private default-arc-width 0.3)
(def ^:private default-max-offset 0.8)
(def ^:private default-passes 3)
(def ^:private default-branch-factor 0.7)
(def ^:private default-width-shrink 0.9)

(def ^:private template-cache (object-array 1))

(defn- ensure-templates
  "Generate arc templates on first call, cache and return.

  Each entry keeps its generated length: SubArcHandler.drawAll centres a
  template on its placement point (`glTranslated(-length/2, 0, 0)` after the
  0.3 scale), so callers need the length to reproduce that offset."
  []
  (or (aget ^objects template-cache 0)
      (let [templates
            (vec
             (for [_ (range default-num-templates)
                   :let [length (+ 2.0 (rand))]]
               {:length length
                :arcs (generate-arc-segments length
                        default-arc-width default-max-offset
                        default-passes default-branch-factor default-width-shrink)}))]
        (aset ^objects template-cache 0 templates)
        templates)))

(defn reset-arc-templates-for-test!
  "Clear cached templates (for testing)."
  []
  (aset ^objects template-cache 0 nil)
  nil)

;; ---------------------------------------------------------------------------
;; Arc rendering: convert segments to quads
;; ---------------------------------------------------------------------------

(defn- segment->quads
  "Convert a list of arc segments to render ops (textured quads).

  Matches upstream ArcFactory.handleSegment: the width axis is
  cross(segDir, normal) against the template's FIXED normal (local +Z — the
  upstream ArcFactory constant), twisted ±15° per segment, and each quad
  carries the PREVIOUS segment's axis (lastDir) at its start, so adjacent
  quads share one axis at the junction and the strip stays watertight through
  every kink. The fixed normal keeps every width axis in one plane — camera-
  facing per-segment axes can rotate past 90° into anti-parallel, twisting
  the quad into a self-intersecting bowtie whose GL fill tears a visible gap
  (ThunderClap's bold surround and the railgun arcs showed that as the arc
  breaking into separate segments)."
  [segments]
  (let [texture (modid/asset-path "textures" "effects/arc/line_segment.png")
        segs (vec segments)
        ;; One random-rotate per segment, consumed in segment order — the same
        ;; draw order as the original for-loop, so the global rand sequence is
        ;; unchanged for every caller.
        perps (loop [i 0
                     acc []
                     prev (vec3/v3 0.0 1.0 0.0)]
                (if (>= i (count segs))
                  acc
                  (let [{:keys [start end]} (nth segs i)
                        dir-vec (v- (:pos end) (:pos start))
                        ;; Create perpendicular direction for quad width
                        perp-raw (v-cross dir-vec (vec3/v3 0.0 0.0 1.0))
                        perp (if (> (v-length perp-raw) 1.0e-5)
                               (v-normalize (random-rotate 15.0 perp-raw))
                               ;; segment parallel to the normal — keep the
                               ;; carried axis (lastDir semantics).
                               prev)]
                    (recur (inc i) (conj acc perp) perp))))]
    (vec
     (mapv (fn [seg-idx {:keys [start end alpha]}]
             (let [start-pos (:pos start)
                   end-pos (:pos end)
                   start-perp (nth perps (if (zero? seg-idx) 0 (dec seg-idx)))
                   end-perp (nth perps seg-idx)
                   half-w1 (* 0.5 (:width start 0.15))
                   half-w2 (* 0.5 (:width end 0.15))
                   p0 (v+ start-pos (v* start-perp half-w1))
                   p1 (v+ start-pos (v* start-perp (- half-w1)))
                   p2 (v+ end-pos (v* end-perp (- half-w2)))
                   p3 (v+ end-pos (v* end-perp half-w2))
                   r 255 g 255 b 255
                   a (int (min 255 (* 255 alpha)))]
               {:kind :quad
                :texture texture
                :p0 p0 :p1 p1 :p2 p2 :p3 p3
                :u0 0.0 :u1 1.0 :v0 0.0 :v1 1.0
                :color {:r r :g g :b b :a a}}))
           (range (count segs)) segs))))

;; ---------------------------------------------------------------------------
;; Public API: descending strike bolt
;;
;; Vanilla's LightningBoltRenderer walks 8 fixed 16-block segments straight up
;; from the entity, jittering each joint by at most ±5. Over 128 blocks that
;; is a very shallow wander, which is why a vanilla bolt reads as a bright
;; vertical column rather than a forked strike. The generator below reuses the
;; same L-system as the small arcs but tuned for a tall descending channel:
;; many more joints, a displacement budget that halves each pass (fractal
;; midpoint displacement), and forks that branch off and taper out on the way
;; down.
;; ---------------------------------------------------------------------------

(def ^:private strike-defaults
  {:height 64.0          ;; tall enough to leave the top out of frame
   :width 0.55
   :max-offset 5.0       ;; halved per pass: 5, 2.5, 1.25, 0.625, 0.3125
   :passes 5             ;; 2^5 joints on the trunk — ~2 blocks per segment
   :branch-factor 0.16
   :branch-length 0.28   ;; fraction of the remaining drop
   :width-shrink 0.55})

(defn- midpoint-displace
  "One fractal midpoint-displacement pass: split every span and push the new
  joint sideways by up to `offset`."
  [pts offset]
  (conj (into []
              (mapcat (fn [[a b]]
                        (let [dir (v- b a)
                              mid (v* (v+ a b) 0.5)
                              push (random-dir-orthogonal (v-normalize dir)
                                                          (* offset (rand)))]
                          [a (v+ mid push)])))
              (partition 2 1 pts))
        (last pts)))

(defn- refine-channel
  [pts max-offset passes]
  (loop [pts pts
         offset (double max-offset)
         pass 0]
    (if (>= pass passes)
      pts
      (recur (midpoint-displace pts offset) (/ offset 2.0) (inc pass)))))

(defn- polyline->segments
  [pts width alpha]
  (mapv (fn [[a b]] (segment (point a width) (point b width) alpha))
        (partition 2 1 pts)))

(defn strike-bolt-segments
  "World-space segments for a bolt descending onto `impact` out of the sky.

  Fractal midpoint displacement on a sky-to-impact channel, plus forks that
  veer off and taper out on the way down.

  Deliberately not `generate-arc-segments`: that one alternates which list it
  refines and hands the unprocessed branch list back as the trunk, so the
  trunk is dropped after the first pass and a run with no branches returns
  nothing at all. That behaviour is load-bearing for the bushy little railgun
  arcs it was ported for, but a strike needs its channel to survive.

  Generate once per strike and keep the result — the displacement is
  randomised, so rebuilding per frame would make the channel crawl instead of
  holding its shape while it flickers."
  ([impact] (strike-bolt-segments impact {}))
  ([impact opts]
   (let [{:keys [height width max-offset passes branch-factor branch-length
                 width-shrink]}
         (merge strike-defaults opts)
         impact-v (vec3/v3 (double (:x impact)) (double (:y impact)) (double (:z impact)))
         top (v+ impact-v (vec3/v3 0.0 (double height) 0.0))
         trunk (refine-channel [top impact-v] max-offset passes)
         ;; Forks start at a trunk joint, keep heading roughly downward, and
         ;; get their own (shorter, thinner, dimmer) displaced channel.
         branches
         (into []
               (comp
                 (map-indexed vector)
                 (filter (fn [[idx _]] (and (pos? idx) (< (rand) branch-factor))))
                 (mapcat
                   (fn [[idx joint]]
                     (let [remaining (- (count trunk) idx)
                           span (* (double branch-length)
                                   (/ (double remaining) (count trunk))
                                   (double height))]
                       (when (> span 1.0)
                         (let [dir (random-rotate 35.0
                                                  (v* (vec3/v3 0.0 -1.0 0.0) span))
                               tip (v+ joint dir)]
                           (polyline->segments
                             (refine-channel [joint tip] (/ max-offset 2.0)
                                             (max 1 (dec passes)))
                             (* width width-shrink)
                             0.6)))))))
               trunk)]
     (into (polyline->segments trunk width 1.0) branches))))

(defn bolt-segments->ops
  "Render pre-generated bolt segments, scaling every segment's alpha by
  `alpha-scale` so the caller can drive the flash envelope."
  [segments alpha-scale]
  (let [scale (double alpha-scale)]
    (if-not (pos? scale)
      []
      (segment->quads (mapv #(update % :alpha * scale) segments)))))

;; ---------------------------------------------------------------------------
;; Public API: EntitySurroundArc BOLD support (thunder-clap charge surround)
;; ---------------------------------------------------------------------------

(def ^:private surround-bold-width 0.35)
(def ^:private surround-bold-max-offset 1.2)
(def ^:private surround-bold-branch-factor 0.45)
(def ^:private surround-bold-passes 3)
(def ^:private surround-bold-width-shrink 0.7)
(def ^:private surround-bold-template-count 10)

(defn surround-arc-templates
  "Generate BOLD surround-arc templates — the static ArcType.BOLD ArcFactory
  config in upstream EntitySurroundArc: 10 templates, length 3.5–4.5, width
  0.35, maxOffset 1.2, passes 3, branchFactor 0.45, widthShrink 0.7. Each
  entry is {:length double :segments [segment-list ...]} (length is needed by
  SubArcHandler.drawAll to center the template on its spawn point)."
  []
  (vec
   (for [_ (range surround-bold-template-count)
         :let [length (+ 3.5 (rand))]]
     {:length length
      :segments (generate-arc-segments
                 length surround-bold-width surround-bold-max-offset
                 surround-bold-passes surround-bold-branch-factor
                 surround-bold-width-shrink)})))

(defn- v-rotate-x [^cn.li.mcmod.math.V3 a rad]
  (let [cos (Math/cos rad) sin (Math/sin rad)]
    (vec3/v3 (.-x a)
             (- (* (.-y a) cos) (* (.-z a) sin))
             (+ (* (.-z a) cos) (* (.-y a) sin)))))

(defn- v-rotate-z [^cn.li.mcmod.math.V3 a rad]
  (let [cos (Math/cos rad) sin (Math/sin rad)]
    (vec3/v3 (+ (* (.-x a) cos) (* (.-y a) sin))
             (- (* (.-y a) cos) (* (.-x a) sin))
             (.-z a))))

(defn- rotate-xyz [p rx ry rz]
  ;; OpenGL applies the last transformation call first: SubArcHandler.drawAll
  ;; calls glRotate(rotZ) then (rotY) then (rotX), so vertices pass through
  ;; Rx, then Ry, then Rz.
  (v-rotate-z (v-rotate-yaw (v-rotate-x p rx) ry) rz))

(defn surround-arc-ops
  "Render one SubArc (matching SubArcHandler.drawAll): template drawn at
  world `pos`, rotated rot-x/rot-y/rot-z degrees around it, scaled by
  `scale` (EntitySurroundArc uses 0.3), centered on `pos`."
  [template pos rot-x rot-y rot-z scale]
  (let [length (:length template)
        rx (* (double rot-x) (/ Math/PI 180.0))
        ry (* (double rot-y) (/ Math/PI 180.0))
        rz (* (double rot-z) (/ Math/PI 180.0))
        center (v* (vec3/v3 length 0.0 0.0) 0.5)
        to-world (fn [p] (v+ pos (rotate-xyz (v* (v- p center) (double scale)) rx ry rz)))]
    (mapv (fn [op]
            (-> op
                (update :p0 to-world)
                (update :p1 to-world)
                (update :p2 to-world)
                (update :p3 to-world)))
          ;; segment->quads expects a FLAT segment list — generate-arc-segments
          ;; returns segment-LISTS (trunk + branches), so flatten first (the
          ;; railgun-arc-ops caller does the same).
          (segment->quads (vec (mapcat identity (:segments template)))))))

;; ---------------------------------------------------------------------------
;; Public API: arc ops for railgun beam
;; ---------------------------------------------------------------------------

;; SubArcHandler.drawAll: every sub-arc is drawn at `glScaled(0.3, 0.3, 0.3)`,
;; so a 2-3 unit template covers 0.6-0.9 blocks, not 2-3.
(def ^:private sub-arc-scale 0.3)

(defn- rotate-zyx
  "SubArcHandler.drawAll's `glRotated(rotZ,z) glRotated(rotY,y) glRotated(rotX,x)`
  applied to a template point: each sub-arc sits at a random orientation in
  world axes rather than aligned with whatever spawned it."
  [^cn.li.mcmod.math.V3 p sin-x cos-x sin-y cos-y sin-z cos-z]
  (let [x (.-x p) y (.-y p) z (.-z p)
        ;; Rx
        y1 (- (* y cos-x) (* z sin-x))
        z1 (+ (* y sin-x) (* z cos-x))
        ;; Ry
        x2 (+ (* x cos-y) (* z1 sin-y))
        z2 (- (* z1 cos-y) (* x sin-y))
        ;; Rz
        x3 (- (* x2 cos-z) (* y1 sin-z))
        y3 (+ (* x2 sin-z) (* y1 cos-z))]
    (vec3/v3 x3 y3 z2)))

(defn railgun-arc-ops
  "Build arc render ops placed along a railgun beam.

  beam: {:keys [start end ttl max-ttl arc-placements]} — start/end are V3.
  Each placement carries the sub-arc state SubArc owns upstream: which
  template it currently shows, whether it is visible this frame, and its fixed
  random orientation."
  [_camera-pos beam _style]
  (let [templates (ensure-templates)
        {:keys [start end ttl]} beam
        alive? (pos? (long (or ttl 0)))
        num-templates (count templates)
        beam-length (v-length (v- end start))]
    (when (and alive? (seq templates) (> beam-length 1.0e-5))
      (let [direction (v-normalize (v- end start))
            candidate (if (< (Math/abs (.-y ^cn.li.mcmod.math.V3 direction)) 0.9)
                        vec3/unit-y
                        vec3/unit-x)
            right (v-normalize (v-cross candidate direction))
            up (v-normalize (v-cross direction right))
            placements (or (:arc-placements beam)
                           [{:distance 1.0 :theta 0.0 :radius 0.1}])]
        (mapcat (fn [idx {:keys [distance theta radius tex-id draw? rot-x rot-y rot-z]}]
                  ;; SubArc starts invisible and flickers on; a placement with
                  ;; no sub-arc state at all (older payload) simply draws.
                  (when-not (false? draw?)
                    (let [radial (v+ (v* right (* radius (Math/sin theta)))
                                     (v* up (* radius (Math/cos theta))))
                          world-pos (v+ start
                                       (v+ (v* direction distance) radial))
                          {:keys [length arcs]} (nth templates (mod (long (or tex-id idx))
                                                                    num-templates))
                          ;; glTranslated(-length/2) runs after the scale, so the
                          ;; centring offset is in scaled units too.
                          half (* 0.5 (double length))
                          sin-x (Math/sin (double (or rot-x 0.0)))
                          cos-x (Math/cos (double (or rot-x 0.0)))
                          sin-y (Math/sin (double (or rot-y 0.0)))
                          cos-y (Math/cos (double (or rot-y 0.0)))
                          sin-z (Math/sin (double (or rot-z 0.0)))
                          cos-z (Math/cos (double (or rot-z 0.0)))
                          to-world
                          (fn [^cn.li.mcmod.math.V3 p]
                            (let [local (vec3/v3 (* sub-arc-scale (- (.-x p) half))
                                                 (* sub-arc-scale (.-y p))
                                                 (* sub-arc-scale (.-z p)))
                                  r (rotate-zyx local sin-x cos-x sin-y cos-y sin-z cos-z)]
                              (v+ world-pos r)))]
                      (map (fn [quad]
                             (-> quad
                                 (assoc :p0 (to-world (:p0 quad)))
                                 (assoc :p1 (to-world (:p1 quad)))
                                 (assoc :p2 (to-world (:p2 quad)))
                                 (assoc :p3 (to-world (:p3 quad)))))
                           (segment->quads (apply concat arcs))))))
                (range)
                placements)))))
