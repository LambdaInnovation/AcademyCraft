(ns cn.li.ac.ability.client.effects.arc-fx-lsystem-test
  "The L-system port of upstream ArcFactory.

  Upstream keeps listAll and bufferAll as parallel per-arc ping-pong buffers:
  index j of each is one arc's read/write pair, every arc is refined on every
  pass, and a fork appends a new arc to both. The port had collapsed them into
  two flat lists and, on the passes that refined the branch list, handed the
  previous unrefined branch list back as the trunk."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]))

(def ^:private gen #'arc-fx/generate-arc-segments)
(def ^:private random-rotate #'arc-fx/random-rotate)

(defn- trunk [arcs] (first arcs))

(deftest trunk-survives-every-pass-test
  ;; The regression: with nothing forking, the old implementation replaced the
  ;; trunk with the (empty) branch list and returned no segments at all.
  (let [arcs (gen 3.0 0.3 0.8 3 0.0 0.9)]
    (is (= 1 (count arcs)) "no forks means exactly one arc")
    (is (= 8 (count (trunk arcs))) "2^3 joints after three passes")))

(deftest trunk-survives-alongside-forks-test
  (let [arcs (gen 3.0 0.3 0.8 3 0.9 0.9)]
    (is (> (count arcs) 1) "forks become their own arcs")
    (is (= 8 (count (trunk arcs)))
        "and the trunk is still refined to 2^3, not replaced by a fork")))

(deftest trunk-keeps-its-endpoints-test
  ;; Only interior joints are displaced, so the arc still spans 0 -> length
  ;; along local +X and the railgun placement transform stays valid.
  (let [arcs (gen 3.0 0.3 0.8 3 0.5 0.9)
        segs (trunk arcs)
        start (:pos (:start (first segs)))
        end (:pos (:end (last segs)))]
    (is (< (rv3/vlen (rv3/v- start (rv3/v3 0.0 0.0 0.0))) 1.0e-9))
    (is (< (rv3/vlen (rv3/v- end (rv3/v3 3.0 0.0 0.0))) 1.0e-9))))

(defn- angle-deg [a b]
  (let [dot (+ (* (.-x a) (.-x b)) (* (.-y a) (.-y b)) (* (.-z a) (.-z b)))
        mag (* (rv3/vlen a) (rv3/vlen b))]
    (if (zero? mag)
      0.0
      (Math/toDegrees (Math/acos (max -1.0 (min 1.0 (/ dot mag))))))))

(deftest random-rotate-converts-degrees-to-radians-test
  ;; Upstream: a = rangef(-range, range) / 180 * PI. The port divided by pi
  ;; where the conversion multiplies, making every rotation pi^2 (~9.9x) too
  ;; small — a 180-degree budget could not deviate more than ~26 degrees.
  (let [dir (rv3/v3 1.0 0.0 0.0)
        deviations (repeatedly 200 #(angle-deg dir (random-rotate 180.0 dir)))]
    (is (> (apply max deviations) 60.0)
        (str "a 180-degree budget must reach well past the ~26 degrees the "
             "off-by-pi-squared version topped out at; max was "
             (apply max deviations)))))

(deftest random-rotate-respects-a-zero-budget-test
  (let [dir (rv3/v3 1.0 0.0 0.0)]
    (is (< (angle-deg dir (random-rotate 0.0 dir)) 1.0e-9))))

(defn- segs-cross?
  "Proper segment-intersection test in the plane with normal n: AB and CD
  cross iff their endpoints straddle each other's lines."
  [n a b c d]
  (let [orient (fn [p q r] (rv3/vdot (rv3/vcross (rv3/v- q p) (rv3/v- r p)) n))]
    (and (neg? (* (orient a b c) (orient a b d)))
         (neg? (* (orient c d a) (orient c d b))))))

(defn- quad-simple?
  "True when the quad's two opposite edge pairs do not cross. A bowtie — the
  width axes of adjacent quads anti-parallel, so the quad folds over itself —
  fails; its GL fill covers the wrong half, tearing a visible gap."
  [{:keys [p0 p1 p2 p3]}]
  (let [n (rv3/vnorm (rv3/vcross (rv3/v- p2 p0) (rv3/v- p3 p1)))]
    (not (or (segs-cross? n p0 p1 p2 p3)
             (segs-cross? n p1 p2 p3 p0)))))

(deftest segment-quads-stay-watertight-through-kinks-test
  ;; Upstream handleSegment: width axis = cross(segDir, normal) against the
  ;; template's FIXED normal (local +Z, the ArcFactory constant), carried
  ;; across segments as lastDir — adjacent quads share one axis at the
  ;; junction AND no quad's width edges can twist anti-parallel into a
  ;; self-intersecting bowtie (camera-facing per-segment axes could, tearing
  ;; a visible gap — the arc read as breaking into separate segments).
  (let [kink (rv3/v3 1.0 0.5 0.0)
        s1 {:start {:pos (rv3/v3 0.0 0.0 0.0) :width 0.2}
            :end {:pos kink :width 0.2}
            :alpha 1.0}
        s2 {:start {:pos kink :width 0.2}
            :end {:pos (rv3/v3 2.0 0.0 0.0) :width 0.2}
            :alpha 1.0}
        [q0 q1] (#'arc-fx/segment->quads [s1 s2])]
    (is (= (:p2 q0) (:p1 q1)) "adjacent quads share the junction edge")
    (is (= (:p3 q0) (:p0 q1)))
    (is (every? quad-simple? [q0 q1]) "no quad may self-intersect (bowtie)")))
