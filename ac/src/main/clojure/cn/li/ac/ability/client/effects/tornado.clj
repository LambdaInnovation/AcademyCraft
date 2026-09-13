(ns cn.li.ac.ability.client.effects.tornado
  "Port of the original TornadoEffect + TornadoRenderer (vecmanip client
  effects), shared by every skill that spawns a tornado column.

  The ring stack is randomized once when the effect starts (TornadoEffect's
  constructor); per tick the noise-driven ring displacement / radius / texture
  scroll are sampled (TornadoRenderer's calcdx/r/rot at GameTimer*4); per frame
  the sampled rings are turned into world-space textured quads.

  Note the original's `rot` is a texture-U scroll (`u0 = uStep*idx - rot`),
  NOT a geometric spin: the ring's vertex ring is fixed and the texture slides
  around it. Each quad covers exactly 1/div of the texture, so the 20 segments
  wrap tornado_ring.png once around the column.

  Callers differ only in the affine placement of the column: Storm Wing pins
  four small columns to the player's back (a rotation matrix), Plasma Cannon
  drops one huge column on the ground unrotated. Both feed `ring-quad-ops` an
  origin plus a linear map, so this namespace never has to know which."
  (:require [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]))

;; ---------------------------------------------------------------------------
;; Constants (original TornadoEffect_/TornadoRenderer)
;; ---------------------------------------------------------------------------

(def divide 40) ;; original TornadoEffect_.divide
(def ring-segments 20) ;; original TornadoRenderer.div
(def ^:private u-step (/ 1.0 (double ring-segments)))

(def ring-texture
  (modid/asset-path "textures" "effects/tornado_ring.png"))

;; Original TornadoRenderer.circleData: (sin, cos) per segment, indexed mod div
;; so the last quad closes the ring back onto segment 0.
(def ^:private ring-circle
  (mapv (fn [i]
          (let [rad (* 2.0 Math/PI (/ (double i) (double ring-segments)))]
            [(Math/sin rad) (Math/cos rad)]))
        (range ring-segments)))

;; ---------------------------------------------------------------------------
;; ImprovedNoise (Ken Perlin 2002) - deterministic, no deps
;; ---------------------------------------------------------------------------

(def ^:private ^"[I" perm-padded
  (let [^"[I" a (int-array 512)
        perm [151 160 137 91 90 15 131 13 201 95 96 53 194 233 7 225
              140 36 103 30 69 142 8 99 37 240 21 10 23 190 6 148 247
              120 234 75 0 26 197 62 94 252 219 203 117 35 11 32 57 177
              33 88 237 149 56 87 174 20 125 136 171 168 68 175 74 165
              71 134 139 48 27 166 77 146 158 231 83 111 229 122 60 211
              133 230 220 105 92 41 55 46 245 40 244 102 143 54 65 25 63
              161 1 216 80 73 209 76 132 187 208 89 18 169 200 196 135
              130 116 188 159 86 164 100 109 198 173 186 3 64 52 217 226
              250 124 123 5 202 38 147 118 126 255 82 85 212 207 206 59
              227 47 16 58 17 182 189 28 42 223 183 170 213 119 248 152 2
              44 154 163 70 221 153 101 155 167 43 172 9 129 22 39 253 19
              98 108 110 79 113 224 232 178 185 112 104 218 246 97 228
              251 34 242 193 238 210 144 12 191 179 162 241 81 51 145 235
              249 14 239 107 49 192 214 31 181 199 106 157 184 84 204 176
              115 121 50 45 127 4 150 254 138 236 205 93 222 114 67 29 24
              72 243 141 128 195 78 66 215 61 156 180]]
    (dotimes [i 512]
      (aset a i (int (nth perm (bit-and i 255)))))
    a))

(defn fade
  ^double [^double t]
  ;; 6t^5 - 15t^4 + 10t^3 == t*t*t*(t*(t*6 - 15) + 10). The +10 is an ADDEND,
  ;; not a factor: folding it into the product made fade(1) = -90 instead of 1,
  ;; so every lerp weight extrapolated wildly and the "noise" came out in the
  ;; hundreds — which is what turned the tornadoes into flailing giant rings.
  (* t t t (+ (* t (- (* t 6.0) 15.0)) 10.0)))

(defn- noise-grad
  ^double [^long hash ^double x ^double y ^double z]
  (let [h (bit-and hash 15)
        u (if (< h 8) x y)
        v (cond (< h 4) y
                (or (= h 12) (= h 14)) x
                :else z)]
    (+ (if (zero? (bit-and h 1)) u (- u))
       (if (zero? (bit-and h 2)) v (- v)))))

(defn perlin-noise
  "Classic Perlin improved noise; z fixed to an integer lattice plane (0/1)."
  ^double [^double x ^double y ^double z]
  (let [X (bit-and (long (Math/floor x)) 255)
        Y (bit-and (long (Math/floor y)) 255)
        Z (bit-and (long (Math/floor z)) 255)
        xf (- x (Math/floor x))
        yf (- y (Math/floor y))
        zf (- z (Math/floor z))
        u (fade xf) v (fade yf) w (fade zf)
        ^"[I" p perm-padded
        A (+ (aget p X) Y)
        AA (+ (aget p A) Z) AB (+ (aget p (inc A)) Z)
        B (+ (aget p (inc X)) Y)
        BA (+ (aget p B) Z) BB (+ (aget p (inc B)) Z)
        lerp (fn [t a b] (+ a (* t (- b a))))]
    (lerp w
          (lerp v
                (lerp u (noise-grad (aget p AA) xf yf zf)
                      (noise-grad (aget p BA) (- xf 1.0) yf zf))
                (lerp u (noise-grad (aget p AB) xf (- yf 1.0) zf)
                      (noise-grad (aget p BB) (- xf 1.0) (- yf 1.0) zf)))
          (lerp v
                (lerp u (noise-grad (aget p (inc AA)) xf yf (- zf 1.0))
                      (noise-grad (aget p (inc BA)) (- xf 1.0) yf (- zf 1.0)))
                (lerp u (noise-grad (aget p (inc AB)) xf (- yf 1.0) (- zf 1.0))
                      (noise-grad (aget p (inc BB)) (- xf 1.0) (- yf 1.0) (- zf 1.0)))))))

(defn- noise2 ^double [x y] (perlin-noise x y 0.0))
(defn- noise2-alt ^double [x y] (perlin-noise x y 1.0))
(defn- noise1 ^double [x] (perlin-noise x 0.0 0.0))

;; ---------------------------------------------------------------------------
;; Ring stacks (randomized once per start, like TornadoEffect's ctor)
;; ---------------------------------------------------------------------------

(defn- ranged-random [lower upper]
  (+ (double lower) (* (rand) (- (double upper) (double lower)))))

(defn- rand-gaussian
  ^double []
  (* (Math/sqrt (* -2.0 (Math/log (max 1.0e-9 (rand)))))
     (Math/cos (* 2.0 Math/PI (rand)))))

(defn build-ring-stack
  "Port of TornadoEffect's constructor: accumulate ring heights along a column
  of height `ht` with jittered spacing, ~35% doubled rings. The original's
  `density` gate is not modelled — every caller (Storm Wing, Plasma Cannon)
  passes 1.0, so every step emits."
  [ht]
  (let [ht (double ht)
        stdstep (/ ht (double divide))
        rings (transient [])]
    (loop [accum 0.0]
      (if (< accum ht)
        ;; Original emits the ring unconditionally after stepping, so the
        ;; topmost ring legitimately sits slightly above ht (ny > 1).
        (let [accum' (+ accum (* stdstep (+ 1.0 (* (rand-gaussian) 0.2))))]
          (conj! rings {:y accum'
                        :w (* stdstep (ranged-random 1.8 2.2))
                        :phase (* (rand) 360.0)
                        :scale (ranged-random 0.9 1.2)})
          (when (< (rand) 0.35)
            (conj! rings {:y accum'
                          :w (* stdstep (ranged-random 1.8 2.2))
                          :phase (* (rand) 360.0)
                          :scale (ranged-random 1.2 1.7)}))
          (recur accum'))
        (persistent! rings)))))

(defn new-column
  "One tornado column's fixed randomness: the ring stack plus TornadoEffect's
  per-instance `timeOffest`. `extra` is merged in for the caller's own
  placement data (Storm Wing's per-column offsets/angles)."
  ([params] (new-column params nil))
  ([{:keys [ht]} extra]
   (merge extra
          {:t-offset (* (rand) 20.0)
           :rings (build-ring-stack ht)})))

;; ---------------------------------------------------------------------------
;; Per-tick animation sampling (TornadoRenderer.calcdx / r / rot)
;; ---------------------------------------------------------------------------

(defn effect-time
  "TornadoEffect.time(): GameTimer.getTime * 4. One client tick is 0.05s."
  ^double [ticks]
  (* 0.2 (double ticks)))

(defn ring-states
  "Sample every ring of one tornado column at `time` (GameTimer*4 equivalent).
  Returns [{:y :w :r :dx :dz :u-scroll} ...] in tornado-local space.

  `params` is {:ht :sz :dscale} — the original TornadoEffect constructor args."
  [time {:keys [ht sz dscale]} {:keys [rings t-offset]}]
  (let [time (- (double time) (double (or t-offset 0.0)))
        ht (double ht)
        sz (double sz)
        dscale (double (or dscale 1.0))]
    (mapv (fn [{:keys [y w phase scale]}]
            (let [ny (/ (double y) ht)
                  t0 (* 0.1 time)
                  ;; Original calcdx: displacement scaled by sz*dscale, ring
                  ;; RADIUS (r * sz * sizeScale) by sz only.
                  sway (* (+ 0.3 (Math/pow (Math/abs (* 2.0 ny)) 1.4))
                          sz dscale)
                  rr (* (+ (+ 0.5 (* 0.3 (noise2 ny (* 0.2 time))))
                           (* 0.5 (Math/pow (* 1.5 ny) 2.0))
                           (noise1 ny))
                        sz (double scale))
                  ;; drawRing's `rot` argument: rot(ny,t) + ring.phase, both in
                  ;; texture-U units. Wrapped into [0,1) here — the texture
                  ;; repeats, so this is visually identical to the original's
                  ;; unbounded value but keeps float UV precision intact over a
                  ;; long session.
                  scroll (+ (* 0.1 (+ 1.0 (* 0.5 ny)) time) (double phase))]
              {:y y
               :w w
               :r rr
               :dx (* (noise2 ny t0) sway)
               :dz (* (noise2-alt ny t0) sway)
               :u-scroll (- scroll (Math/floor scroll))}))
          rings)))

;; ---------------------------------------------------------------------------
;; Render ops (TornadoRenderer.drawRing)
;; ---------------------------------------------------------------------------

(defn ring-quad-ops
  "Turn one column's sampled rings into world-space quad ops.

  `origin` is the column base in world space, `linear` a linear map from
  tornado-local coordinates to world offsets (`(fn [x y z] [x' y' z'])`) — the
  caller's rotation, or `identity-linear` for an upright column. A ring corner
  is `origin + linear(x, y, z)`; the ring band's half-width runs along the
  column axis `linear(0,1,0)`, which a tilted caller makes very much not
  world-up.

  `alpha` is TornadoEffect.alpha; the renderer's `glColor4d(1,1,1,alpha*0.7)`
  is applied here."
  [origin-x origin-y origin-z linear alpha rings]
  (let [[ux uy uz] (linear 0.0 1.0 0.0)
        color {:r 255 :g 255 :b 255
               :a (max 0 (min 255 (int (* 255.0 (double alpha) 0.7))))}]
    (mapcat
      (fn [{:keys [y w r dx dz u-scroll]}]
        (let [hw (* 0.5 (double w))
              scroll (double u-scroll)
              ;; One point per segment boundary, shared by the two quads that
              ;; meet there.
              edges (mapv (fn [[^double s ^double c]]
                            (linear (+ (double dx) (* s (double r)))
                                    (double y)
                                    (+ (double dz) (* c (double r)))))
                          ring-circle)]
          (map (fn [i]
                 (let [[e0x e0y e0z] (nth edges i)
                       [e1x e1y e1z] (nth edges (rem (inc (long i)) ring-segments))
                       t0x (+ origin-x e0x) t0y (+ origin-y e0y) t0z (+ origin-z e0z)
                       t1x (+ origin-x e1x) t1y (+ origin-y e1y) t1z (+ origin-z e1z)
                       u0 (- (* u-step (double i)) scroll)]
                   ;; p0/p3 top (v=0), p1/p2 bottom (v=1) — matches the
                   ;; original's glTexCoord/glVertex emission order.
                   (ru/quad-op ring-texture
                     (vec3/v3 (+ t0x (* ux hw)) (+ t0y (* uy hw)) (+ t0z (* uz hw)))
                     (vec3/v3 (- t0x (* ux hw)) (- t0y (* uy hw)) (- t0z (* uz hw)))
                     (vec3/v3 (- t1x (* ux hw)) (- t1y (* uy hw)) (- t1z (* uz hw)))
                     (vec3/v3 (+ t1x (* ux hw)) (+ t1y (* uy hw)) (+ t1z (* uz hw)))
                     u0 (+ u0 u-step) 0.0 1.0 color)))
               (range ring-segments))))
      rings)))

(defn identity-linear
  "The unrotated column: TornadoEntityRenderer only translates to the entity
  position before calling TornadoRenderer.doRender."
  [x y z]
  [x y z])
