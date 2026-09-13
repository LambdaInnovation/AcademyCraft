(ns cn.li.ac.ability.client.render-util
  "Client-side rendering primitives for ability visual effects.

  Provides vector math, render-op constructors, camera-basis helpers,
  zigzag arc ops (matching original EntityArc/ArcPatterns), and
  billboard-beam ops that are shared across all skill effect renderers.

  Uses V3 (rv3), not the map-based cn.li.ac.util.math.vec3 — this is the
  per-frame render hot path; vec3.clj stays map-based for its other 28
  non-render consumers (see rv3.clj docstring)."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.arc-patterns :as arc])
  (:import [cn.li.mcmod.math V3]))

;; ---------------------------------------------------------------------------
;; Color helpers
;; ---------------------------------------------------------------------------

(defn with-alpha
  "Assoc :a onto a color map, clamped to [0, 255]."
  [color alpha]
  (assoc color :a (int (max 0 (min 255 (long alpha))))))

;; ---------------------------------------------------------------------------
;; Render op constructors
;; ---------------------------------------------------------------------------

(defn quad-op
  "Build a textured-quad render op. 6-arity uses full [0,1] UVs; 10-arity
  overrides them directly (avoids a second map allocation via assoc for
  UV-scrolling callers like zigzag-arc-ops)."
  ([texture p0 p1 p2 p3 color]
   (quad-op texture p0 p1 p2 p3 0.0 1.0 0.0 1.0 color))
  ([texture p0 p1 p2 p3 u0 u1 v0 v1 color]
   {:kind :quad
    :texture texture
    :p0 p0 :p1 p1 :p2 p2 :p3 p3
    :u0 u0 :u1 u1 :v0 v0 :v1 v1
    :color color}))

(defn line-op
  "Build a line-segment render op."
  [p1 p2 color]
  {:kind :line :p1 p1 :p2 p2 :color color})

(def ^:private default-beam-texture
  (modid/asset-path "textures" "effects/arc.png"))

;; Upstream EntityArc renders every segment with arc/line_segment.png — a
;; bright white core with a faint blue halo — tinted white (glColor4d(1,1,1,
;; alpha)). The port's arc.png is a soft blue band that read as a washed-out
;; ribbon next to upstream's bright bolt.
(def ^:private default-zigzag-texture
  (modid/asset-path "textures" "effects/arc/line_segment.png"))

(defn- white-argb
  "White tint at alpha — upstream glColor4d(1,1,1,alpha): the texture carries
  the colour."
  [alpha]
  (let [a (int (max 0 (min 255 (long alpha))))]
    (unchecked-int (bit-or (bit-shift-left a 24) 0x00FFFFFF))))

(declare beam-right-axis camera-facing-right-axis)

(defn- rand-offset
  "Random vec3 offset within magnitude."
  ^V3 [magnitude]
  (when (pos? (double magnitude))
    (vec3/v3 (* magnitude (- (* 2.0 (rand)) 1.0))
             (* magnitude (- (* 2.0 (rand)) 1.0))
             (* magnitude (- (* 2.0 (rand)) 1.0)))))

(defn zigzag-arc-ops
  "Generate zigzag lightning arc render ops matching original AcademyCraft
  EntityArc + ArcPatterns visual style.

  `vertices` is the zigzag path (arc-patterns/generate-zigzag-segments).
  `pattern` is the resolved arc-patterns/get-pattern map.

  Faithful to upstream ArcFactory.handleSegment:
    - ONE textured quad per segment, tinted WHITE at the segment alpha
      (glColor4d(1,1,1,alpha)) — the line_segment texture carries the colour
      (bright core + blue halo). The port's earlier three-layer stack (outer
      shell + inner core + line) read as a soft blue band, not a bright bolt.
    - u runs 0..1 across EACH segment, so every segment displays the full
      texture — a chain of bright blobs, not one texture stretched over the
      whole arc. The corner order [start+right, start-right, end-right,
      end+right] is upstream's addVert layout (u along the beam, v across the
      width) — the swapped order would sample the texture sideways (the
      railgun glow-board bug, b199c8b7b).
    - Width axis cross(segDir, normal) against the arc's FIXED normal,
      carried across segments as lastDir (watertight strip, no bowties).
    - Forks derived from the arc's :seed — upstream bakes branches into the
      template at generation; per-frame rand made the forks jitter.

  Params:
    :life-ratio        ??0.0 (just spawned) to 1.0 (about to die)
    :texture           ??override texture path (default: effects/arc/line_segment.png)
    :origin-offset     ??rigid world-space translation applied to the whole arc
                         (the caller's ViewOptimize-style hand offset)
    :seed              ??per-arc seed for the fork layout (default 0)
    :alpha-scale       ??multiplier on the segment alpha (branches render at
                         upstream's 0.9)"
  [cam-pos vertices pattern {:keys [life-ratio texture origin-offset seed alpha-scale]
                             :or {life-ratio 0.5 seed 0 alpha-scale 1.0}}]
  (let [texture     (or texture default-zigzag-texture)
        shift       (if origin-offset
                      (fn [^V3 p] (vec3/v+ p origin-offset))
                      identity)
        lr          (double life-ratio)
        base-alpha  (* (double alpha-scale) (arc/life-fade-alpha 255 lr))
        color       (white-argb base-alpha)
        width       (double (or (:width pattern) 0.1))
        segment-count (dec (count vertices))
        start (shift (:pos (first vertices)))
        end (shift (:pos (peek vertices)))
        ;; Upstream ArcFactory.handleSegment: the width axis is
        ;; cross(segDir, normal) against the arc's FIXED normal (template
        ;; local +Z — the world right axis of a horizontal beam), and each
        ;; quad carries the previous segment's axis (lastDir) at its start, so
        ;; adjacent quads share one axis at the junction and the strip stays
        ;; watertight through every kink.
        ;;
        ;; The fixed normal is the load-bearing half: every width axis lies
        ;; in one plane, so consecutive axes can never rotate past 90° into
        ;; anti-parallel. Camera-facing per-segment axes COULD — segments
        ;; straddling the camera get opposite to-cam vectors, the quad's two
        ;; width edges flip and the GL fill of the self-intersecting bowtie
        ;; covers the wrong half, tearing a visible gap — worst on the wide
        ;; outer layer (ThunderBolt's strongArc light-blue shell).
        forward (vec3/vnorm (vec3/v- end start))
        normal-raw (vec3/vcross forward vec3/unit-y)
        normal (if (> (vec3/vlen normal-raw) 1.0e-5)
                 (vec3/vnorm normal-raw)
                 vec3/unit-x)
        laterals (loop [i 0
                        acc []
                        prev vec3/unit-x]
                   (if (>= i segment-count)
                     acc
                     (let [v0 (nth vertices i)
                           v1 (nth vertices (inc i))
                           dir-vec (vec3/v- (shift (:pos v1))
                                            (shift (:pos v0)))
                           raw (vec3/vcross dir-vec normal)
                           lateral (if (> (vec3/vlen raw) 1.0e-5)
                                     (vec3/vnorm raw)
                                     ;; segment parallel to the normal — keep
                                     ;; the carried axis (lastDir semantics).
                                     prev)]
                       (recur (inc i) (conj acc lateral) lateral))))
        seg-quads
        (mapcat (fn [i]
                  (let [v0 (nth vertices i)
                        v1 (nth vertices (inc i))
                        seg-start (shift (:pos v0))
                        seg-end   (shift (:pos v1))
                        right-start (nth laterals (if (zero? i) 0 (dec i)))
                        right-end (nth laterals i)
                        outer-s (vec3/v* right-start width)
                        outer-e (vec3/v* right-end width)
                        p0 (vec3/v+ seg-start outer-s)
                        p1 (vec3/v- seg-start outer-s)
                        p2 (vec3/v- seg-end outer-e)
                        p3 (vec3/v+ seg-end outer-e)]
                    [(quad-op texture p0 p1 p2 p3 0.0 1.0 0.0 1.0 color)]))
                (range segment-count))
        fork-count   (int (or (:fork-count pattern) 0))
        fork-length  (double (or (:fork-length pattern) 0.5))
        fork-angle   (double (or (:fork-angle pattern) 0.5))
        fork-quads
        (when (pos? fork-count)
          (let [beam-vec (vec3/v- end start)
                beam-len (vec3/vlen beam-vec)
                beam-dir (vec3/vnorm beam-vec)
                perp1 (beam-right-axis start end cam-pos)
                perp2 (if (> (vec3/vlen perp1) 0.01)
                        (vec3/vnorm (vec3/vcross beam-dir perp1))
                        vec3/unit-x)
                fork-rng (java.util.Random. (long (hash [seed :forks])))
                n (inc (.nextInt fork-rng fork-count))
                branch-length (* beam-len fork-length)
                ;; Upstream branches (ArcFactory.handleSingle) run roughly
                ;; ALONG the bolt — dir = (displaced midpoint - segment start)
                ;; x lengthShrink — and are refined into jagged sub-arcs. The
                ;; port's straight sticks in the perpendicular plane read as
                ;; stray horizontal beams sticking out of the bolt; re-render
                ;; each fork as a small meandering bolt instead (same
                ;; single-quad strip, width 0.7x and alpha 0.9x like
                ;; widthShrink/alphaShrink).
                branch-pattern (assoc pattern
                                      :fork-count 0
                                      :width (* width 0.7))]
            (mapcat (fn [_]
                      (let [t (.nextDouble fork-rng)
                            mid (vec3/v+ start (vec3/v* beam-vec t))
                            angle (* fork-angle (- (* 2.0 (.nextDouble fork-rng)) 1.0))
                            phi (* 2.0 Math/PI (.nextDouble fork-rng))
                            tilt (vec3/v+ (vec3/v* perp1 (Math/cos phi))
                                          (vec3/v* perp2 (Math/sin phi)))
                            dir (vec3/vnorm (vec3/v+ (vec3/v* beam-dir (Math/cos angle))
                                                     (vec3/v* tilt (Math/sin angle))))
                            fork-end (vec3/v+ mid (vec3/v* dir branch-length))
                            branch-seed (.nextLong fork-rng)
                            vertices (arc/generate-zigzag-segments
                                      mid fork-end
                                      {:segments 6
                                       :amplitude 0.3
                                       :seed (long branch-seed)})]
                        (zigzag-arc-ops cam-pos vertices branch-pattern
                                        {:life-ratio lr
                                         :seed (long branch-seed)
                                         :alpha-scale 0.9})))
                    (range n))))]
    (vec (concat seg-quads fork-quads))))

(defn billboard-beam-ops
  "Build ability beam primitives matching original ArcPatterns visual style:
  - Textured quad (arc.png) with jitter and flicker for lightning effect
  - Inner core quad for intensity
  - Optional center line
  - Optional forked side branches for branched lightning look

  New params:
    :jitter-amount     ??world-space random endpoint offset (default 0, no jitter)
    :flicker-threshold ??0-1 probability arc is visible this frame (default nil, always visible)
    :fork-count        ??max number of side branches to generate (default 0)
    :fork-length       ??fraction [0-1] of main beam length for fork reach (default 0.5)
    :fork-angle        ??radians, max deviation from beam axis for forks (default 0.5)
    :fork-width-frac   ??fraction of main width for fork beams (default 0.5)

  Flicker is resolved first: when the roll comes up invisible this frame
  (original behavior never emits a line in that case either), all jitter/
  billboard/fork computation is skipped instead of computed and discarded."
  [cam-pos start end {:keys [texture width core-width core-ratio
                              outer-color inner-color line-color
                              jitter-amount flicker-threshold
                              fork-count fork-length fork-angle fork-width-frac]}]
  (let [flicker (double (or flicker-threshold 1.0))]
    (if-not (or (>= flicker 1.0) (< (rand) flicker))
      []
      (let [texture (or texture default-beam-texture)
            width (double (or width 0.0))
            ;; Jitter: randomize endpoint to simulate arc wiggle
            jitter-amount (double (or jitter-amount 0.0))
            start' (if (pos? jitter-amount)
                     (vec3/v+ start (rand-offset (* jitter-amount 0.3))) ;; less jitter at origin
                     start)
            end' (if (pos? jitter-amount)
                   (vec3/v+ end (rand-offset jitter-amount))
                   end)
            core-width (double (or core-width (* width (double (or core-ratio 0.45)))))
            right (beam-right-axis start' end' cam-pos)
            outer-offset (vec3/v* right width)
            core-offset (vec3/v* right core-width)
            p0 (vec3/v+ start' outer-offset)
            p1 (vec3/v- start' outer-offset)
            p2 (vec3/v- end' outer-offset)
            p3 (vec3/v+ end' outer-offset)
            c0 (vec3/v+ start' core-offset)
            c1 (vec3/v- start' core-offset)
            c2 (vec3/v- end' core-offset)
            c3 (vec3/v+ end' core-offset)
            base-quads [(quad-op texture p0 p1 p2 p3 outer-color)
                        (quad-op texture c0 c1 c2 c3 inner-color)]
            ;; Forked side branches (matching original branched arc patterns)
            fork-count (int (or fork-count 0))
            fork-length (double (or fork-length 0.5))
            fork-angle (double (or fork-angle 0.5))
            fork-width-frac (double (or fork-width-frac 0.5))
            side-quads (when (pos? fork-count)
                         (let [beam-vec (vec3/v- end' start')
                               beam-len (vec3/vlen beam-vec)
                               dir (vec3/vnorm beam-vec)
                               perp1 (beam-right-axis start' end' cam-pos)
                               perp2 (if (> (vec3/vlen perp1) 0.01)
                                       (vec3/vnorm (vec3/vcross dir perp1))
                                       vec3/unit-x)
                               fork-n (inc (rand-int fork-count))]
                           (mapcat (fn [_]
                                     (let [t (rand)
                                           mid-point (vec3/v+ start' (vec3/v* beam-vec t))
                                           angle (* fork-angle (- (* 2.0 (rand)) 1.0))
                                           rot-dir (vec3/v+ (vec3/v* perp1 (Math/cos angle))
                                                       (vec3/v* perp2 (Math/sin angle)))
                                           fork-end (vec3/v+ mid-point (vec3/v* rot-dir (* beam-len fork-length)))
                                           fork-w (* width fork-width-frac)
                                           fork-core (* fork-w 0.4)
                                           fr (beam-right-axis mid-point fork-end cam-pos)
                                           fo (vec3/v* fr fork-w)
                                           fc (vec3/v* fr fork-core)
                                           fork-alpha (with-alpha outer-color
                                                       (int (* (or (:a outer-color) 128) 0.6)))]
                                       [(quad-op texture
                                          (vec3/v+ mid-point fo) (vec3/v- mid-point fo)
                                          (vec3/v- fork-end fo) (vec3/v+ fork-end fo)
                                          fork-alpha)
                                        (quad-op texture
                                          (vec3/v+ mid-point fc) (vec3/v- mid-point fc)
                                          (vec3/v- fork-end fc) (vec3/v+ fork-end fc)
                                          (with-alpha inner-color
                                            (int (* (or (:a inner-color) 255) 0.4))))]))
                                   (range fork-n))))]
        (cond-> (into base-quads side-quads)
          line-color (conj (line-op start' end' line-color)))))))

;; ---------------------------------------------------------------------------
;; Camera-relative basis helpers
;; ---------------------------------------------------------------------------

(defn beam-right-axis
  "Compute the right axis for a billboard beam between `start` and `end`,
  perpendicular to both the beam direction and the camera view direction."
  [start end cam-pos]
  (let [dir (vec3/vnorm (vec3/v- end start))
        mid (vec3/v* (vec3/v+ start end) 0.5)
        to-cam (vec3/vnorm (vec3/v- cam-pos mid))
        raw (vec3/vcross dir to-cam)]
    ;; Camera at/near start → to-cam ~parallel dir → cross magnitude tiny.
    ;; A first-person self-targeted beam (e.g. current-charging: caster's own
    ;; eye to their own crosshair target) sits on-axis with the camera EVERY
    ;; frame, not just as a rare edge case — and the synced start position
    ;; lags the live camera by a frame or two, so `raw`'s magnitude is small
    ;; but noise-dominated rather than exactly zero. The old 1.0e-5 threshold
    ;; only matched near-exact parallelism, letting that noise pick an
    ;; effectively random (often edge-on/invisible) right axis instead of
    ;; ever reaching the stable world-up fallback below. 0.05 (~3 degrees)
    ;; catches the whole near-degenerate band, where the fallback is not
    ;; just a rescue but the more numerically stable choice anyway.
    (if (> (vec3/vlen raw) 0.05)
      (vec3/vnorm raw)
      (let [perp (vec3/vcross vec3/unit-y dir)]
        (if (> (vec3/vlen perp) 1.0e-5)
          (vec3/vnorm perp)
          vec3/unit-x)))))

(defn camera-facing-right-axis
  "Right axis for a camera-facing billboard at `center`."
  [center cam-pos]
  (let [to-cam (vec3/vnorm (vec3/v- cam-pos center))
        raw (vec3/vcross vec3/unit-y to-cam)]
    (if (> (vec3/vlen raw) 1.0e-5)
      (vec3/vnorm raw)
      vec3/unit-x)))

(defn billboard-up-axis
  "Up axis for a billboard given `center`, `cam-pos`, and already-computed `right`."
  [center cam-pos right]
  (let [to-cam (vec3/vnorm (vec3/v- cam-pos center))
        raw (vec3/vcross to-cam right)]
    (if (> (vec3/vlen raw) 1.0e-5)
      (vec3/vnorm raw)
      vec3/unit-y)))
