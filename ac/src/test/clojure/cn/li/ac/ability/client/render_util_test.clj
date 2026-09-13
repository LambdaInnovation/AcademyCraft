(ns cn.li.ac.ability.client.render-util-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.effects.rv3 :as v3]))

(deftest billboard-beam-ops-builds-standard-primitive
  (testing "outer quad, inner quad, and center line use the shared beam geometry"
    (let [start (v3/v3 0.0 0.0 0.0)
          end (v3/v3 0.0 0.0 1.0)
          cam-pos (v3/v3 1.0 0.0 0.0)
          outer {:r 1 :g 2 :b 3 :a 4}
          inner {:r 5 :g 6 :b 7 :a 8}
          line {:r 9 :g 10 :b 11 :a 12}
          ops (ru/billboard-beam-ops cam-pos start end
                {:width 0.2
                 :core-ratio 0.5
                 :outer-color outer
                 :inner-color inner
                 :line-color line})]
      (is (= 3 (count ops)))
      (is (= {:kind :quad
              :texture "academy:textures/effects/arc.png"
              :p0 (v3/v3 0.0 0.2 0.0)
              :p1 (v3/v3 0.0 -0.2 0.0)
              :p2 (v3/v3 0.0 -0.2 1.0)
              :p3 (v3/v3 0.0 0.2 1.0)
              :u0 0.0 :u1 1.0 :v0 0.0 :v1 1.0
              :color outer}
             (first ops)))
      (is (= {:kind :quad
              :texture "academy:textures/effects/arc.png"
              :p0 (v3/v3 0.0 0.1 0.0)
              :p1 (v3/v3 0.0 -0.1 0.0)
              :p2 (v3/v3 0.0 -0.1 1.0)
              :p3 (v3/v3 0.0 0.1 1.0)
              :u0 0.0 :u1 1.0 :v0 0.0 :v1 1.0
              :color inner}
             (second ops)))
      (is (= {:kind :line :p1 start :p2 end :color line}
             (nth ops 2))))))

(defn- segs-cross?
  "Proper segment-intersection test in the plane with normal n: AB and CD
  cross iff their endpoints straddle each other's lines."
  [n a b c d]
  (let [orient (fn [p q r] (v3/vdot (v3/vcross (v3/v- q p) (v3/v- r p)) n))]
    (and (neg? (* (orient a b c) (orient a b d)))
         (neg? (* (orient c d a) (orient c d b))))))

(defn- quad-simple?
  "True when the quad's two opposite edge pairs do not cross. A bowtie — the
  width axes of adjacent quads anti-parallel, so the quad folds over itself —
  fails; its GL fill covers the wrong half, tearing a visible gap. (Concave-
  but-simple quads pass and render fine — GL splits on the p0-p2 diagonal.)"
  [{:keys [p0 p1 p2 p3]}]
  (let [n (v3/vnorm (v3/vcross (v3/v- p2 p0) (v3/v- p3 p1)))]
    (not (or (segs-cross? n p0 p1 p2 p3)
             (segs-cross? n p1 p2 p3 p0)))))

(deftest zigzag-arc-junctions-are-watertight-test
  ;; Upstream ArcFactory.handleSegment: width axis = cross(segDir, normal)
  ;; against the arc's FIXED normal (template local +Z), carried across
  ;; segments as lastDir. Adjacent quads share one axis at the junction, so
  ;; the strip stays continuous through every kink — and the fixed normal
  ;; keeps every width axis in one plane, so a quad can never twist past 90°
  ;; into a self-intersecting bowtie. Camera-facing per-segment axes could:
  ;; segments straddling the camera get anti-parallel axes, and the torn GL
  ;; fill read as the arc breaking into separate segments (ThunderBolt's
  ;; strongArc, worst on the wide light-blue outer layer).
  (let [vertices [{:pos (v3/v3 0.0 0.0 0.0) :u 0.0 :v 0.0}
                  {:pos (v3/v3 0.0 1.0 0.0) :u 0.5 :v 0.0}
                  {:pos (v3/v3 1.0 1.0 0.0) :u 1.0 :v 0.0}]
        pattern {:width 0.3 :core-ratio 0.4 :fork-count 0
                 :color-outer {:r 130 :g 210 :b 255}
                 :color-inner {:r 230 :g 245 :b 255}
                 :color-line {:r 200 :g 240 :b 255}}
        ops (ru/zigzag-arc-ops (v3/v3 0.5 0.5 1.0) vertices pattern
                               {:life-ratio 0.5})
        [o0 o1] ops
        ;; The fixed normal of this arc (start (0,0,0) -> end (1,1,0)) is
        ;; cross(forward, up) = (0,0,1); the two segments' in-plane laterals
        ;; genuinely differ.
        lateral-0 (v3/vcross (v3/v- (v3/v3 0.0 1.0 0.0) (v3/v3 0.0 0.0 0.0))
                             (v3/v3 0.0 0.0 1.0))
        lateral-1 (v3/vcross (v3/v- (v3/v3 1.0 1.0 0.0) (v3/v3 0.0 1.0 0.0))
                             (v3/v3 0.0 0.0 1.0))]
    (is (not= lateral-0 lateral-1) "the two segments genuinely fan apart")
    (is (= (:p2 o0) (:p1 o1)) "adjacent quads share the junction edge")
    (is (= (:p3 o0) (:p0 o1)))
    (is (every? quad-simple? ops) "no quad may self-intersect (bowtie)")))
