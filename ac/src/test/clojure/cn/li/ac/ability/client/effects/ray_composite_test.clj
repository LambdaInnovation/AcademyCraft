(ns cn.li.ac.ability.client.effects.ray-composite-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cn.li.ac.ability.client.effects.ray-composite :as rc]
            [cn.li.ac.ability.client.effects.rv3 :as v])
  (:import [cn.li.mcmod.math V3]
           [java.io File]))

(defn- perpendicular-extents
  "Largest offset of any emitted vertex from the beam axis, along the two
  perpendiculars. A tube reaches the radius on BOTH; a flat strip on one."
  [ops ^V3 start ^V3 end]
  (let [dir (v/vnorm (v/v- end start))
        cand (if (< (Math/abs (.-y dir)) 0.9) v/unit-y v/unit-x)
        r (v/vnorm (v/vcross cand dir))
        u (v/vnorm (v/vcross dir r))]
    (reduce (fn [[mr mu] p]
              (let [d (v/v- p start)]
                [(max mr (Math/abs (v/vdot d r)))
                 (max mu (Math/abs (v/vdot d u)))]))
            [0.0 0.0]
            (mapcat (fn [op] (map #(get op %) [:p0 :p1 :p2 :p3])) ops))))

(deftest cylinders-are-untextured-test
  ;; RendererRayCylinder draws through ShaderNotex — notex.frag, pure vertex
  ;; colour, no texture at all. Only RendererRayGlow's three boards are
  ;; textured (ShaderSimple).
  ;;
  ;; This is not a detail. effects/arc.png, the shared beam sprite, is 79%
  ;; fully transparent and averages alpha 13/255; texturing the tubes with it
  ;; multiplied them down to a few percent opacity, so every ray in the game
  ;; lost its tubes and read as nothing but the flat glow boards.
  (let [start (v/v3 0.0 64.0 0.0)
        end (v/v3 0.0 64.0 15.0)
        ops (rc/tube-ops start end 0.22 rc/outer-head-fix
                         {:r 106 :g 242 :b 106 :a 50})]
    (is (seq ops))
    (is (every? #(= rc/solid-texture (:texture %)) ops)
        "the cylinders must not sample a sprite")
    (is (not (re-find #"arc\.png" (str rc/solid-texture))))))

(deftest cylinders-are-round-not-flat-test
  (let [start (v/v3 0.0 64.0 0.0)
        end (v/v3 0.0 64.0 15.0)
        radius 0.22
        ops (rc/tube-ops start end radius rc/outer-head-fix
                         {:r 255 :g 255 :b 255 :a 255})
        [er eu] (perpendicular-extents ops start end)]
    ;; DIV = 12 around, 4-segment nose at each end plus the body
    (is (= 108 (count ops)))
    (is (< (Math/abs (- er radius)) 1.0e-6))
    (is (< (Math/abs (- eu radius)) 1.0e-6))))

(defn- source-root
  "The directory the ac Clojure sources sit under on the classpath."
  ^File []
  (let [here (io/file (.toURI (io/resource "cn/li/ac/ability/client/effects/ray_composite.clj")))]
    ;; .../<root>/cn/li/ac/ability/client/effects/ray_composite.clj
    (nth (iterate #(.getParentFile ^File %) here) 8)))

(defn- ray-composite-call-sites
  []
  (->> (file-seq (source-root))
       (filter #(str/ends-with? (.getName ^File %) ".clj"))
       (keep (fn [^File f]
               (let [s (slurp f)]
                 (when (re-find #"ray-composite/composite-ops" s)
                   [(.getName f) s]))))))

(defn- direct-layer-call-sites
  "Files calling glow-ops / tube-ops directly instead of through
  composite-ops."
  []
  (->> (file-seq (source-root))
       (filter #(str/ends-with? (.getName ^File %) ".clj"))
       (filter (fn [^File f]
                 (re-find #"ray-composite/(glow|tube)-ops" (slurp f))))
       (map #(.getName ^File %))))

(deftest composite-emits-glow-then-inner-then-outer-test
  ;; RendererRayComposite appends glow, cylinderIn, cylinderOut, and
  ;; RendererList draws in append order. Every layer is translucent, so
  ;; emitting them out of order silently deletes whichever layer loses the
  ;; depth test:
  ;;
  ;;   tubes before glow  -> the 1.5-wide flat board covers the 0.44 tube and
  ;;                         the ray reads as a sheet from every angle.
  ;;   outer before inner -> the alpha-50 shell stamps depth over the alpha-230
  ;;                         core nested inside it, and the beam goes patchily
  ;;                         solid/hollow as the viewing angle changes.
  ;;
  ;; Both have shipped as bugs when the order lived at seven call sites. It
  ;; now lives in ONE function, so the test sits next to the implementation.
  (let [tex (rc/glow-textures "mdray")
        start (v/v3 0.0 64.0 0.0)
        end (v/v3 0.0 64.0 15.0)
        ops (rc/composite-ops (v/v3 0.0 70.0 -8.0) start end
              {:glow {:textures tex :width 1.5 :color {:r 255 :g 255 :b 255 :a 204}}
               :inner {:radius 0.1 :color {:r 216 :g 248 :b 216 :a 230}}
               :outer {:radius 0.2 :color {:r 106 :g 242 :b 106 :a 50}}})]
    ;; three glow boards, then 108 inner + 108 outer cylinder quads
    (is (= 219 (count ops)))
    (is (= [(:blend-in tex) (:tile tex) (:blend-out tex)]
           (mapv :texture (take 3 ops))))
    (is (every? #(= rc/solid-texture (:texture %)) (drop 3 ops)))
    ;; the bright core lands before the faint shell
    (is (= 230 (:a (:color (first (drop 3 ops))))))
    (is (= 50 (:a (:color (first (drop 111 ops))))))))

(deftest glow-never-writes-depth-test
  ;; The glow is a flat plane THROUGH the axis and the cylinder's near wall
  ;; sits only `radius` (0.045 for the small rays) in front of it. Depth
  ;; precision falls off with distance, so a depth-writing glow won the depth
  ;; fight far out and rays went hollow away from the camera. A halo has no
  ;; business occluding anything: it tests depth, never writes it, and the
  ;; tubes drawn after it always land on top.
  (let [tex (rc/glow-textures "mdray")
        start (v/v3 0.0 64.0 0.0)
        end (v/v3 0.0 64.0 15.0)
        glow (rc/glow-ops (v/v3 0.0 70.0 -8.0) start end
              {:textures tex :width 1.5 :color {:r 255 :g 255 :b 255 :a 204}})
        tubes (rc/tube-ops start end 0.22 rc/outer-head-fix
                           {:r 106 :g 242 :b 106 :a 50})]
    (is (every? :no-depth-write? glow))
    (is (not-any? :no-depth-write? tubes))
    ;; A halo is light, not smoke: additive (ONE/ONE) blending, never the
    ;; plain translucent blend the tubes use.
    (is (every? :additive? glow))
    (is (not-any? :additive? tubes))))

(deftest skills-route-all-layers-through-composite-ops-test
  ;; composite-ops is the only entry point: skills pass style parameters and
  ;; never call glow-ops / tube-ops themselves. The layering used to live at
  ;; seven call sites, and sites drifted out of order — both wrong orders
  ;; shipped as visible bugs. A direct call to the layer fns is the exact
  ;; shape of that regression.
  (let [sites (ray-composite-call-sites)]
    (is (<= 7 (count sites)) "expected to find the ray call sites on the classpath")
    (is (empty? (direct-layer-call-sites))
        "every layer must be emitted by composite-ops, not assembled at a call site")))

(deftest glow-alpha-doubles-get-alpha-test
  ;; RendererRayGlow.doRender: alpha = preA * getAlpha() * getGlowAlpha(), and
  ;; EntityRayBase.getGlowAlpha() = (1 - glowWiggleRadius + glowWiggle) *
  ;; getAlpha() — the glow multiplies getAlpha TWICE, the cylinders once. The
  ;; port multiplied once and every glow stayed too bright through the fades.
  ;; glowWiggleRadius is 0.1, so the wiggle keeps the factor in [0.9, 1.0].
  (let [sample (fn [s l] (rc/glow-alpha 127.0 0.5 s l))]
    ;; 127 * 0.25 * [0.9, 1.0] — a single getAlpha would give up to 63.5
    (is (every? #(<= 28.575 % 31.75)
                (for [s (range 40) l (range 20)]
                  (sample (* s 0.13) (* l 0.05)))))
    (is (<= 0.9 (rc/glow-wiggle-factor 0.0 0.0) 1.0))
    (is (<= 0.9 (rc/glow-wiggle-factor 5.0 0.7) 1.0))))

(deftest glow-boards-stay-textured-test
  ;; The glow is the one textured layer, and it keeps its three sprites.
  (let [tex (rc/glow-textures "mdray")
        ops (rc/glow-ops (v/v3 0.0 70.0 -8.0) (v/v3 0.0 64.0 0.0) (v/v3 0.0 64.0 15.0)
                         {:textures tex :width 1.5 :color {:r 255 :g 255 :b 255 :a 204}})]
    (is (= 3 (count ops)))
    (is (= [(:blend-in tex) (:tile tex) (:blend-out tex)]
           (mapv :texture ops)))))
