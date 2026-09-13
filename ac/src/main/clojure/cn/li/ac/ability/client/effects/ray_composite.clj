(ns cn.li.ac.ability.client.effects.ray-composite
  "Port of RendererRayComposite — the shared shape every AC ray entity is drawn
  with (railgun, mdray_small, meltdowner's rays...).

  Upstream stacks three things along the ray:

    RendererRayGlow      three boards: blend_in over the first `width` units,
                         tile over the middle, blend_out over the last `width`,
                         each `width` across (drawBoard halves it), with the
                         ray extended by startFix / endFix. These are the only
                         TEXTURED part -- they draw through ShaderSimple.
    RendererRayCylinder  an outer tube of radius `width`, and
    RendererRayCylinder  an inner tube, both DIV=12 surfaces of revolution with
                         a y = sqrt(x) paraboloid head at each end (headFix
                         0.98 on the inner one). Both draw through ShaderNotex
                         -- notex.frag, pure vertex colour, NO texture.

  Callers supply the radii, colours and textures; the geometry is identical
  across skills, which is exactly why upstream has one class for it.

  ORDER MATTERS, in the order listed above. RendererRayComposite appends glow,
  then cylinderIn, then cylinderOut, and RendererList draws in append order.
  The tubes are translucent AND write depth, so whichever tube lands first
  wins the depth test wherever they overlap:

    glow before the tubes  -- the glow is a halo: it tests depth but never
                              writes it (see glow-ops), so wherever a tube sits
                              in front of it the glow's fragments drop out and
                              the ray reads as a round beam, not a sheet.
    inner before outer     -- the outer shell is only alpha 50; if it draws
                              first it stamps depth over the alpha-230 core
                              nested inside it, and that core is what makes a
                              ray look solid. Getting this backwards left the
                              beam patchily solid and hollow as the angle
                              changed, since the two only overlap where the
                              shell's near wall is between you and the core."
  (:require [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid])
  (:import [cn.li.mcmod.math V3]))

;; RendererRayCylinder: DIV, and the head mesh's D subdivision.
(def ^:private tube-segments 12)
(def ^:private head-segments 4)

;; RendererRayComposite sets cylinderIn.headFix = 0.98; the outer keeps 1.0.
(def inner-head-fix 0.98)
(def outer-head-fix 1.0)

;; RendererRayGlow's startFix / endFix as the composite configures them.
(def default-start-fix -0.3)
(def default-end-fix 0.3)

;; EntityRayBase.glowWiggleRadius = 0.1: getGlowAlpha() multiplies getAlpha()
;; by a factor in [1 - radius, 1] as glowWiggle random-walks upstream.
(def ^:private glow-wiggle-radius 0.1)

(defn glow-wiggle-factor
  "EntityRayBase.getGlowAlpha's (1 - glowWiggleRadius + glowWiggle): upstream
  glowWiggle random-walks in [0, glowWiggleRadius] at maxGlowWiggleSpeed
  0.4/s — a slow drift. The port models random walks as a deterministic sine
  of a per-ray seed (see ray-width-factor in the skills), reading the same
  range.

  The sine travels HALF a cycle over the ray's life, not more: an earlier
  version ran 15 rad per life (~2.4 full pulses) and the glow visibly
  flickered — with the steep squared fade-out, a pulse peak landing just
  before the beam died read as the glow flashing at the moment the ray
  ended."
  ^double [^double seed ^double life]
  (+ (- 1.0 glow-wiggle-radius)
     (* glow-wiggle-radius
        (+ 0.5 (* 0.5 (Math/sin (+ seed (* Math/PI life))))))))

(defn glow-alpha
  "RendererRayGlow.doRender: alpha = preA * getAlpha() * getGlowAlpha(), and
  EntityRayBase.getGlowAlpha() = glow-wiggle-factor * getAlpha() — so the glow
  multiplies the base alpha by the ray's getAlpha() TWICE, while the
  cylinders multiply once. The port squared this off and every glow stayed
  too bright through the fades."
  ^double [^double pre-a ^double get-alpha ^double seed ^double life]
  (* pre-a get-alpha get-alpha (glow-wiggle-factor seed life)))

(defn glow-textures
  "Resources.getRayTextures(name): effects/<name>/{blend_in,tile,blend_out}."
  [name]
  {:blend-in (modid/asset-path "textures" (str "effects/" name "/blend_in.png"))
   :tile (modid/asset-path "textures" (str "effects/" name "/tile.png"))
   :blend-out (modid/asset-path "textures" (str "effects/" name "/blend_out.png"))})

(defn ray-profile
  "Radius samples [distance-along-ray radius] for a cylinder of `radius` over a
  ray of `length`: a paraboloid nose over the first `radius * head-fix` units,
  the straight body, and a mirrored nose past the end."
  [^double length ^double radius ^double head-fix]
  (let [nose (* radius head-fix)
        nose-pts (for [i (range (inc head-segments))
                       :let [u (/ (double i) head-segments)]]
                   [(* nose u) (* radius (Math/sqrt u))])
        tail-pts (for [i (range 1 (inc head-segments))
                       :let [u (/ (double i) head-segments)]]
                   [(+ length (* nose u)) (* radius (Math/sqrt (- 1.0 u)))])]
    (if (<= length nose)
      ;; Too short for a body — nose straight into tail.
      (concat nose-pts tail-pts)
      (concat nose-pts [[length radius]] tail-pts))))

(def solid-texture
  "The cylinders have NO texture upstream (ShaderNotex = notex.frag, pure
  vertex colour). This plan renderer samples a texture per quad, so they get a
  4x4 fully opaque white one, which multiplies to the same thing.

  Handing them a real sprite is not a cosmetic choice: effects/arc.png is 79%
  fully transparent and averages alpha 13/255, so texturing the tubes with it
  scaled them down to a few percent opacity and left only the three flat glow
  boards visible -- every ray in the game read as a flat sheet."
  (modid/asset-path "textures" "effects/solid.png"))

(defn tube-ops
  "One RendererRayCylinder: a surface of revolution following `ray-profile`,
  drawn untextured in `color`.

  A flat strip has the same silhouette only from the one angle it is turned
  to; from anywhere else it thins out, which is why rays drawn that way read
  as flat slivers."
  [^V3 start ^V3 end radius head-fix color]
  (let [delta (vec3/v- end start)
        length (vec3/vlen delta)
        radius (double radius)]
    (when (and (> length 1.0e-5) (> radius 1.0e-5))
      (let [dir (vec3/vnorm delta)
            candidate (if (< (Math/abs (.-y ^V3 dir)) 0.9) vec3/unit-y vec3/unit-x)
            right (vec3/vnorm (vec3/vcross candidate dir))
            up (vec3/vnorm (vec3/vcross dir right))
            dtheta (/ (* 2.0 Math/PI) (double tube-segments))
            ring (vec (for [i (range (inc tube-segments))
                            :let [a (* (double i) dtheta)]]
                        (vec3/v+ (vec3/v* right (Math/cos a))
                                 (vec3/v* up (Math/sin a)))))
            at (fn [d w u]
                 (vec3/v+ (vec3/v+ start (vec3/v* dir (double d)))
                          (vec3/v* u (double w))))
            pts (vec (ray-profile length radius (double head-fix)))]
        (vec
          (for [[[d0 w0] [d1 w1]] (partition 2 1 pts)
                i (range tube-segments)
                :let [ua (nth ring i)
                      ub (nth ring (inc i))]]
            (ru/quad-op solid-texture
                        (at d0 w0 ua) (at d0 w0 ub)
                        (at d1 w1 ub) (at d1 w1 ua)
                        color)))))))

(defn glow-ops
  "RendererRayGlow.draw: extend the ray by start-fix / end-fix, then lay the
  three boards along it. `width` is the full board width — drawBoard halves it
  — and doubles as the length of each cap."
  [^V3 cam-pos ^V3 start ^V3 end
   {:keys [textures width color start-fix end-fix]
    :or {start-fix default-start-fix end-fix default-end-fix}}]
  (let [delta (vec3/v- end start)]
    (when (> (vec3/vlen delta) 1.0e-5)
      (let [dir (vec3/vnorm delta)
            width (double width)
            right (vec3/v* (ru/beam-right-axis start end cam-pos) (* 0.5 width))
            gs (vec3/v+ start (vec3/v* dir (double start-fix)))
            ge (vec3/v+ end (vec3/v* dir (double end-fix)))
            ;; A ray shorter than the two caps has no body left; clamp so the
            ;; caps meet in the middle instead of crossing over each other.
            span (vec3/vlen (vec3/v- ge gs))
            cap (min width (* 0.5 span))
            mid1 (vec3/v+ gs (vec3/v* dir cap))
            mid2 (vec3/v- ge (vec3/v* dir cap))
            ;; The corner order encodes the UV orientation: emit-quad!
            ;; assigns p0=(u0,v0), p1=(u0,v1), p2=(u1,v1), p3=(u1,v0).
            ;; Passing [a-right, a+right, b+right, b-right] makes u run
            ;; ALONG the beam and v ACROSS the board's width — exactly
            ;; upstream's drawBoard (v1..v4). The tile texture tapers
            ;; across its own V axis, so v-across-the-width is what
            ;; softens the board's long edges. With the axes the other
            ;; way round the taper ran along the beam and the sides were
            ;; the texture's hard edges: every glow read as a hard-edged
            ;; rectangle with the cap textures' lens shapes smeared
            ;; sideways.
            board (fn [texture ^V3 a ^V3 b]
                    (assoc (ru/quad-op texture
                                       (vec3/v- a right) (vec3/v+ a right)
                                       (vec3/v+ b right) (vec3/v- b right)
                                       color)
                           ;; The glow is a flat plane THROUGH the axis, and
                           ;; the cylinder's near wall sits only `radius` in
                           ;; front of it -- 0.045 for the small rays. Depth
                           ;; precision falls off with distance, so with the
                           ;; glow writing depth the near stretch of a ray won
                           ;; the fight and the far stretch lost to the glow
                           ;; sheet: solid close up, hollow further out.
                           ;;
                           ;; A halo has no business occluding anything, so it
                           ;; tests depth but does not write it, and the tubes
                           ;; drawn after it always land on top.
                           :no-depth-write? true
                           ;; And a halo is light, not smoke: it ADDS to what
                           ;; is behind it (blend ONE/ONE, source-weighted)
                           ;; instead of alpha-blending over it. The level
                           ;; renderer routes this to the additive render type.
                           :additive? true))]
        [(board (:blend-in textures) gs mid1)
         (board (:tile textures) mid1 mid2)
         (board (:blend-out textures) mid2 ge)]))))

(defn composite-ops
  "The WHOLE of RendererRayComposite for one ray: glow boards, cylinderIn,
  cylinderOut, in that order.

  This is the only thing skills should call. The layering is a property of the
  composite, not of any one skill, and every time it lived at the call sites
  instead some of the seven drifted out of order and rays across the whole mod
  rendered wrong.

    {:glow  {:textures ... :width ... :color ... :start-fix ... :end-fix ...}
     :inner {:radius ... :color ...}
     :outer {:radius ... :color ...}}

  Any of the three may be omitted. `head-fix` is not a caller's choice -- the
  inner cylinder gets 0.98 and the outer 1.0, as the composite sets them."
  [^V3 cam-pos ^V3 start ^V3 end {:keys [glow inner outer]}]
  (concat
    (when glow (glow-ops cam-pos start end glow))
    (when inner (tube-ops start end (:radius inner) inner-head-fix (:color inner)))
    (when outer (tube-ops start end (:radius outer) outer-head-fix (:color outer)))))
