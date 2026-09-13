(ns cn.li.ac.ability.client.effects.beam-ops
  "Higher-level beam/ray render operation helpers for client ability FX.

  `beam-render` owns the primitive billboard implementation. This namespace owns
  the repeated ability-side concerns around style maps: RGB + alpha composition,
  fading ttl/max-ttl beams, direct beam rendering, and small glow-line helpers
  used by trajectory previews."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.client.effects.beam-render :as beam-render]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.render-util :as ru])
  (:import [cn.li.mcmod.math V3]))

(def default-glow-line-texture
  (modid/asset-path "textures" "effects/glow_line.png"))

(defn resolve-value
  "Resolve either a constant value or a `[context life]` callback."
  [value context life]
  (if (fn? value)
    (value context life)
    value))

(defn rgba
  "Compose an RGB map and alpha into a clamped RGBA map."
  [rgb alpha]
  (ru/with-alpha rgb alpha))

(defn color-fn
  "Create a beam-render callback from RGB and alpha constants/functions."
  [rgb alpha]
  (fn [context life]
    (rgba (resolve-value rgb context life)
          (resolve-value alpha context life))))

(defn- style-color
  [style color-key rgb-key alpha-key]
  (if (contains? style color-key)
    (get style color-key)
    (when (contains? style rgb-key)
      (color-fn (get style rgb-key) (get style alpha-key 255)))))

(defn render-style
  "Convert an ability FX style map into the lower-level beam-render options.

  Supports either direct `:*color` maps/callbacks or split `:*rgb` + `:*alpha`
  entries. Alpha entries may be constants or `[beam life]` callbacks for fading
  beams. Width/core entries may also be constants or callbacks."
  [style]
  {:texture (:texture style)
   :width (:width style)
   :core-width (:core-width style)
   :core-ratio (:core-ratio style)
   :outer-color (style-color style :outer-color :outer-rgb :outer-alpha)
   :inner-color (style-color style :inner-color :inner-rgb :inner-alpha)
   :line-color (style-color style :line-color :line-rgb :line-alpha)})

(defn- resolve-render-style
  [style context life]
  (->> (render-style style)
       (map (fn [[k v]] [k (resolve-value v context life)]))
       (into {})))

(defn beam-ops
  "Build a non-fading beam from explicit start/end positions and a style map."
  [cam-pos start end style]
  (beam-render/beam-ops cam-pos start end (resolve-render-style style {:start start :end end} 1.0)))

(defn fading-beam-ops
  "Build a fading beam/ray from a state map containing :start/:end/:ttl/:max-ttl."
  [cam-pos beam style]
  (beam-render/fading-beam-ops cam-pos beam (render-style style)))

(defn fading-beams-ops
  "Build fading beam/ray ops for a collection of beam state maps."
  [cam-pos beams style]
  (mapcat #(fading-beam-ops cam-pos % style) beams))

(defn fading-tube-beam-ops
  "Build fading TUBE (cylinder) beam ops for one beam state.

  A billboard quad is edge-on — zero projected area — for a camera sitting on
  the beam axis, which is exactly the caster's own first-person view of a ray
  fired from their eye. Upstream renders these rays as RendererRayCylinder
  (DIV=12) tubes for that reason; the outer tube carries the translucent
  :outer color at style :width radius, the inner tube the bright core at
  :width * :core-ratio."
  [beam style]
  (let [life (beam-render/life-ratio (:ttl beam) (:max-ttl beam))
        resolve-value (fn [value]
                        (if (fn? value) (value beam life) value))
        resolved (render-style style)
        outer-color (resolve-value (:outer-color resolved))
        inner-color (resolve-value (:inner-color resolved))
        outer-r (double (or (resolve-value (:width resolved)) 0.1))
        inner-r (* outer-r (double (or (resolve-value (:core-ratio resolved)) 0.45)))]
    (concat
     (beam-render/cylinder-beam-ops (:start beam) (:end beam)
       {:radius outer-r :color outer-color})
     (beam-render/cylinder-beam-ops (:start beam) (:end beam)
       {:radius inner-r :color inner-color}))))

(defn fading-glow-board-ops
  "Upstream RendererRayGlow board: one wide soft quad along the (already
  hand-fixed) beam, `glow-width` wide (upstream MDRay: 1.5), tinted with the
  resolved :outer color.

  The board's lateral axis is what keeps it visible from the caster's own
  first-person camera, which sits ON the beam axis: first person uses the
  FIXED up-and-back axis (0,1,-0.5) — any axis derived from the beam or view
  direction is parallel to the view ray there, so the board would be edge-on
  — and the hand-fixed start sits off that axis. Third person uses the
  view-perpendicular axis (upstream cross(perpViewDir, dir))."
  ([cam-pos beam style]
   (fading-glow-board-ops cam-pos beam style {}))
  ([cam-pos beam style {:keys [glow-width first-person?] :or {glow-width 1.5
                                                              first-person? false}}]
   (let [life (beam-render/life-ratio (:ttl beam) (:max-ttl beam))
         resolve-value (fn [value]
                         (if (fn? value) (value beam life) value))
         outer-color (resolve-value (:outer-color (render-style style)))
         start (:start beam)
         end (:end beam)
         dir (vec3/vnorm (vec3/v- end start))
         axis (if first-person?
                (vec3/vnorm (vec3/v3 0.0 1.0 -0.5))
                (let [to-beam (vec3/v- start (vec3/map->v3 cam-pos))
                      perp (vec3/vcross to-beam dir)]
                  (if (> (vec3/vlen perp) 1.0e-5)
                    (vec3/vnorm perp)
                    vec3/unit-x)))
         half (* 0.5 (double glow-width))
         p0 (vec3/v+ start (vec3/v* axis half))
         p1 (vec3/v- start (vec3/v* axis half))
         p2 (vec3/v- end (vec3/v* axis half))
         p3 (vec3/v+ end (vec3/v* axis half))]
     [(ru/quad-op default-glow-line-texture p0 p1 p2 p3 outer-color)])))

(defn fade-alpha
  "Return an indexed fade alpha compatible with trajectory/ribbon previews.

  Defaults mirror the existing VecAccel ribbon fade: raw alpha starts at 0.7,
  decreases by 0.021 per segment, and is scaled to 0..255."
  ([idx]
   (fade-alpha idx {}))
  ([idx {:keys [start step scale]}]
   (let [raw-alpha (max 0.0 (- (double (or start 0.7))
                               (* (double idx) (double (or step 0.021)))))]
     (int (* raw-alpha (double (or scale 255.0)))))))

(defn glow-line-quad-op
  "Build the standard glow-line textured quad used by trajectory previews."
  ([p0 p1 p2 p3 color]
   (glow-line-quad-op p0 p1 p2 p3 color default-glow-line-texture))
  ([p0 p1 p2 p3 color texture]
   (ru/quad-op texture p0 p1 p2 p3 color)))
