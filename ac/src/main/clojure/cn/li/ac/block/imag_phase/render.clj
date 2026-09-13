(ns cn.li.ac.block.imag-phase.render
  "CLIENT-ONLY: Imag Phase liquid overlay TESR.

  Renders 3 scrolling overlay quad layers around the fluid surface,
  mirroring the original AcademyCraft RenderImagPhaseLiquid behavior.

  Uses the project's modern render pipeline: PoseStack + VertexConsumer
  instead of legacy fixed-function GL. Follows the cat_engine/render.clj
  pattern for quad submission and lazy resource loading.

  Minecraft-specific queries (fluid height) go through platform-be bridge.

  Render mode is compile-time selectable via `imag-phase-render-mode`
  (:surface-flash default — 方案四: sheets flush with each cell's local
  fluid surface, additive-blended so the flash reads over the opaque black
  surface; :upstream — upstream's exact depth-off rendering; :depth-anchored
  — depth-tested sheets anchored above the fluid surface; :visibility-raycast
  — upstream sheets plus a per-cell camera raycast that skips terrain-occluded
  cells). See the switch definition below.

  The sheets are NOT drawn in the block-entity pass: 1.20.1 renders block
  entities before the translucent terrain layer (verified in LevelRenderer
  bytecode), and the pool's opaque black surface (black.png, alpha 255 —
  identical to upstream's) would cover them. The BE-pass renderer only queues
  the pool cells; the loader's post-translucent stage hook (RenderLevelStageEvent
  AFTER_TRANSLUCENT_BLOCKS / WorldRenderEvents AFTER_TRANSLUCENT) calls
  `draw-pending!` to composite the flash over the liquid surface — the
  position upstream's pass-1 TESRs occupied."
  (:require [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.client.resources :as res]
            [cn.li.mcmod.client.render.tesr-api :as tesr-api]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.util.render :as render]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]))

;; ---------------------------------------------------------------------------
;; Lazy resource loading (same pattern as cat-engine/render.clj)
;; ---------------------------------------------------------------------------

(def ^:private imag-phase-resources-holder nil)
(def ^:private imag-phase-resources
  (machine-render-runtime/lazy-resources #'imag-phase-resources-holder
    {:layer-textures #(vec [(res/texture-location "effects/imag_proj_liquid/0")
                             (res/texture-location "effects/imag_proj_liquid/1")
                             (res/texture-location "effects/imag_proj_liquid/2")])}))

;; ---------------------------------------------------------------------------
;; Compile-time render mode
;; ---------------------------------------------------------------------------

;; Render mode switch (compile-time only: flip and recompile).
;;
;;   :surface-flash (default) — 方案四: sheets flush with each cell's LOCAL
;;     fluid surface (+5mm to clear the surface's own depth), additive-blended
;;     (SRC_ALPHA/ONE) with the vertex alpha boosted ~5× so the sparse
;;     low-alpha texture (99% of texels ≤ 0.25 alpha — invisible over the
;;     opaque black surface with upstream's blend) reads as a blue flash over
;;     the whole pool. Depth-tested: the pool's own surface and the terrain
;;     occlude from the side — nothing hovers above concave cells (the
;;     :depth-anchored rejection), nothing shows through dirt, and flat/full
;;     cells (h=1) show the flash like every other cell.
;;   :upstream — 方案三: upstream-faithful — sheets at the original heights
;;     (-0.3/0.35/0.7)·ht with depth test off, exactly like upstream's
;;     glDisable(GL_DEPTH_TEST). Faithful, but the flash only ever reads where
;;     a sheet pokes above the local surface (concave cells) — flat cells are
;;     black, because the texture's alpha is too low to show over the opaque
;;     black surface.
;;   :depth-anchored — 方案一: sheets anchored just above the per-cell fluid
;;     surface, depth-tested against solid terrain (entityTranslucent). Terrain
;;     occludes the flash, but it floats above the pool's concave spots — the
;;     pool's own surface writes depth before the block-entity pass, so no
;;     depth-tested sheet below it can show.
;;   :visibility-raycast — 方案二: upstream heights + depth off, but each cell
;;     is skipped when a camera→cell raycast (fluids pass through) finds solid
;;     terrain in the way. Needs the :camera-raycast-visible? bridge op
;;     (installed on all six loaders); degrades to :upstream behaviour on any
;;     loader without it.
(def ^:const imag-phase-render-mode :surface-flash)

;; The flash texture's texels are ~99% alpha ≤ 0.25 (59% fully transparent,
;; most of the rest 1-63) — with upstream's SRC_ALPHA/ONE_MINUS_SRC_ALPHA
;; blend the flash is imperceptible over the opaque black surface. The
;; additive mode multiplies the vertex alpha by this boost so the sparse
;; bright texels read as a visible blue flash. Tunable.
(def ^:private surface-flash-alpha-boost 5.0)

(defn- depth-anchored?     [] (= imag-phase-render-mode :depth-anchored))
(defn- visibility-raycast? [] (= imag-phase-render-mode :visibility-raycast))
(defn- surface-flash?      [] (= imag-phase-render-mode :surface-flash))

;; ---------------------------------------------------------------------------
;; Layer definitions
;; ---------------------------------------------------------------------------

(def ^:private layer-defs
  (cond
    (depth-anchored?)
    ;; Anchored just above the fluid surface — every layer clears the
    ;; surface's depth and stays visible across the pool.
    [{:idx 0 :height-fn (fn [ht surface] (+ surface (* 0.02 ht))) :vx 0.3 :vz 0.2 :density 0.7}
     {:idx 1 :height-fn (fn [ht surface] (+ surface (* 0.15 ht))) :vx 0.3 :vz 0.05 :density 0.7}
     {:idx 2 :height-fn (fn [ht surface] (+ surface (* 0.35 ht))) :vx 0.1 :vz 0.25 :density 0.7}]

    (surface-flash?)
    ;; Flush with each cell's local surface (+5mm so the sheet clears the
    ;; surface's own depth from above). All three layers share the height —
    ;; the scrolling UVs merge into one animated flash pattern over the pool.
    [{:idx 0 :height-fn (fn [_ht surface] (+ surface 0.005)) :vx 0.3 :vz 0.2 :density 0.7}
     {:idx 1 :height-fn (fn [_ht surface] (+ surface 0.005)) :vx 0.3 :vz 0.05 :density 0.7}
     {:idx 2 :height-fn (fn [_ht surface] (+ surface 0.005)) :vx 0.1 :vz 0.25 :density 0.7}]

    :else
    ;; :upstream / :visibility-raycast — upstream heights, cell-relative: the
    ;; original 3-layer configuration.
    [{:idx 0 :height-fn (fn [ht _surface] (* -0.3 ht)) :vx 0.3 :vz 0.2 :density 0.7}
     {:idx 1 :height-fn (fn [ht _surface] (* 0.35 ht)) :vx 0.3 :vz 0.05 :density 0.7}
     {:idx 2 :height-fn (fn [ht _surface] (* 0.7 ht))  :vx 0.1 :vz 0.25 :density 0.7
      :condition (fn [ht] (> ht 0.5))}]))

;; Fullbright packed-light constant: LightTexture/pack(15, 15) = 15728880
(def ^:private fullbright-packed-light 15728880)

;; ---------------------------------------------------------------------------
;; Fluid height — uses platform bridge (no reflection, no Minecraft class deps)
;; ---------------------------------------------------------------------------

;; ---------------------------------------------------------------------------
;; Quad geometry — horizontal quad on XZ plane
;; ---------------------------------------------------------------------------

;; [x y z u v] — y=0 (translated to computed height via pose/translate)
(def ^:private quad-vertices
  [[0.0 0.0 0.0 0.0 0.0]   ;; (x=0, y=0, z=0)
   [1.0 0.0 0.0 1.0 0.0]   ;; (x=1, y=0, z=0)
   [1.0 0.0 1.0 1.0 1.0]   ;; (x=1, y=0, z=1)
   [0.0 0.0 1.0 0.0 1.0]]) ;; (x=0, y=0, z=1)

;; The translucent RenderType draws in QUADS mode: exactly 4 vertices per quad,
;; in ring order. Emitting 6 (two triangles) makes the buffer index only the
;; first 4 and stitch the leftovers onto the next block entity's vertices.
(def ^:private quad-vertex-order [0 1 2 3])

;; ---------------------------------------------------------------------------
;; Vertex submission
;; ---------------------------------------------------------------------------

(defn- submit-scrolling-quad!
  "Submit a single scrolling quad layer.

  The vertex format depends on the chosen buffer: :full goes to the entity
  translucent buffer (POSITION_COLOR_TEX_LIGHTMAP_NORMAL — wants the packed
  overlay and an upward normal); :no-overlay goes to the see-through /
  additive types (POSITION_COLOR_TEX_LIGHTMAP with neither)."
  [vc pose-stack du dv density packed-light packed-overlay alpha submit-style]
  (doseq [idx quad-vertex-order
          :let [[x y z u v] (nth quad-vertices idx)
                tex-u (+ (double du) (* (double u) (double density)))
                tex-v (+ (double dv) (* (double v) (double density)))]]
    (if (= submit-style :full)
      (rb/submit-vertex vc pose-stack
                        (double x) (double y) (double z)
                        1.0 1.0 1.0 (float alpha)
                        tex-u tex-v
                        (int packed-overlay)
                        (int packed-light)
                        0.0 1.0 0.0)
      (rb/submit-vertex-no-overlay vc pose-stack
                                   (double x) (double y) (double z)
                                   1.0 1.0 1.0 (float alpha)
                                   tex-u tex-v
                                   (int packed-light)))))

;; ---------------------------------------------------------------------------
;; Distance fade — upstream alpha = 1/(1 + 0.2*dist), block skipped below 0.1
;; ---------------------------------------------------------------------------

(def ^:private min-visible-alpha 0.1)

;; The TESR runs once per pool block per frame and `call-adapter` costs a
;; Framework deref plus a map lookup, so the viewer position is read at most
;; once per frame-length window and shared by every block in the pool. Held in
;; the shared render cache (same governance as cat-engine's rotor cache) rather
;; than a namespace-local atom.
(def ^:private viewer-pos-refresh-ms 16)
(def ^:private viewer-pos-cache-key :imag-phase-viewer-pos)
(def ^:private viewer-pos-initial {:at-ms 0 :pos nil})

(defn clear-viewer-pos-cache!
  "Drop the cached viewer position; the next render re-reads it."
  []
  (machine-render-runtime/clear-render-cache! viewer-pos-cache-key viewer-pos-initial))

(defn- viewer-pos []
  (let [now (System/currentTimeMillis)
        {:keys [at-ms pos]} (machine-render-runtime/render-cache
                              viewer-pos-cache-key viewer-pos-initial)]
    (if (< (- now at-ms) viewer-pos-refresh-ms)
      pos
      (let [fresh (bridge/call-adapter :local-player-pos)]
        (machine-render-runtime/put-render-cache!
          viewer-pos-cache-key {:at-ms now :pos fresh})
        fresh))))

(defn- distance-alpha
  "Upstream RenderImagPhaseLiquid fades the overlay out with viewer distance:
  alpha = 1/(1 + 0.2*dist) from the block centre, and skips the block entirely
  below 0.1 (~45 blocks). Returns 1.0 when the loader installs no
  :local-player-pos op (fabric), i.e. the un-faded look stays the fallback."
  [tile]
  (if-let [viewer (viewer-pos)]
    (let [p (pos/block-pos tile)
          dx (- (+ 0.5 (double (pos/pos-x p))) (double (:x viewer)))
          dy (- (+ 0.5 (double (pos/pos-y p))) (double (:y viewer)))
          dz (- (+ 0.5 (double (pos/pos-z p))) (double (:z viewer)))
          dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))]
      (/ 1.0 (+ 1.0 (* 0.2 dist))))
    1.0))

;; ---------------------------------------------------------------------------
;; 方案二: per-cell visibility — camera→cell raycast with fluid pass-through
;; ---------------------------------------------------------------------------

;; Same refresh pattern as the viewer-pos cache above: the camera position is
;; read at most once per frame-length window and shared by every pool cell.
(def ^:private camera-pos-refresh-ms 16)
(def ^:private camera-pos-cache-key :imag-phase-camera-pos)
(def ^:private camera-pos-initial {:at-ms 0 :pos nil})

(defn clear-camera-pos-cache!
  "Drop the cached camera position; the next render re-reads it."
  []
  (machine-render-runtime/clear-render-cache! camera-pos-cache-key camera-pos-initial))

(defn- camera-pos []
  (let [now (System/currentTimeMillis)
        {:keys [at-ms pos]} (machine-render-runtime/render-cache
                              camera-pos-cache-key camera-pos-initial)]
    (if (< (- now at-ms) camera-pos-refresh-ms)
      pos
      (let [fresh (bridge/call-adapter :camera-position)]
        (machine-render-runtime/put-render-cache!
          camera-pos-cache-key {:at-ms now :pos fresh})
        fresh))))

(defn- cell-visible?
  "方案二: is the cell reachable from the camera without passing through
  solid terrain? The raycast ignores fluids (ClipContext$Fluid/NONE), so the
  pool's own cells and black surface never self-occlude. Degrades to true
  (draw everything, i.e. :upstream behaviour) when the loader installs no
  :camera-position or :camera-raycast-visible? op."
  [tile]
  (if-let [camera (camera-pos)]
    (let [p (pos/block-pos tile)
          cx (+ 0.5 (double (pos/pos-x p)))
          cy (+ 0.5 (double (pos/pos-y p)))
          cz (+ 0.5 (double (pos/pos-z p)))
          visible (bridge/call-adapter :camera-raycast-visible?
                                       (:x camera) (:y camera) (:z camera)
                                       cx cy cz)]
      (if (nil? visible) true visible))
    true))

;; ---------------------------------------------------------------------------
;; Main render
;; ---------------------------------------------------------------------------

(defn- pick-buffer-and-submit
  "Choose the vertex consumer and the matching submit style for the current
  render mode.

  - :depth-anchored / :surface-flash — depth-tested: the pool's own surface
    and the terrain occlude from the side (no flash through dirt, nothing
    hovering). :surface-flash prefers the additive type (SRC_ALPHA/ONE +
    LEQUAL, so the flash reads over the opaque black surface) and falls back
    to the entity translucent buffer.
  - :upstream / :visibility-raycast — see-through (no depth test), rendered
    INTO the translucent target where available: the fluid surface lives in
    that target and is blitted over the main buffer at the end of the level
    pass, which would cover a main-target draw. Degrade to the plain
    see-through or the depth-tested buffer if a loader lacks the target
    variant."
  [buffer-source tex]
  (cond
    (depth-anchored?)
    {:vc (rb/get-translucent-buffer buffer-source tex) :submit-style :full}

    (surface-flash?)
    (if (rb/additive-buffer-available?)
      {:vc (rb/get-additive-buffer buffer-source tex) :submit-style :no-overlay}
      {:vc (rb/get-translucent-buffer buffer-source tex) :submit-style :full})

    :else
    (cond
      (rb/translucent-see-through-target-available?)
      {:vc (rb/get-translucent-see-through-target-buffer buffer-source tex)
       :submit-style :no-overlay}
      (rb/translucent-see-through-available?)
      {:vc (rb/get-translucent-see-through-buffer buffer-source tex)
       :submit-style :no-overlay}
      :else
      {:vc (rb/get-translucent-buffer buffer-source tex) :submit-style :full})))

(defn- render-layer!
  "Render one scrolling overlay layer.

  Height fns take (ht, surface): :depth-anchored and :surface-flash anchor
  the sheets to the per-cell fluid surface; the other modes ignore the
  surface and use the upstream cell-relative heights."
  [pose-stack buffer-source packed-light packed-overlay alpha
   {:keys [idx height-fn vx vz density]} ht surface time]
  (let [height (height-fn ht surface)
        du (mod (* time vx) 1.0)
        dv (mod (* time vz) 1.0)
        textures (:layer-textures (imag-phase-resources))
        tex (nth textures idx)
        {:keys [vc submit-style]} (pick-buffer-and-submit buffer-source tex)]
    (pose/push-pose pose-stack)
    (try
      (pose/translate pose-stack 0.0 height 0.0)
      (submit-scrolling-quad! vc pose-stack du dv density
                              packed-light packed-overlay alpha submit-style)
      (finally
        (pose/pop-pose pose-stack)))))

;; ---------------------------------------------------------------------------
;; Post-translucent stage drawing
;; ---------------------------------------------------------------------------

;; The BE pass runs before the opaque fluid surface, so the flash must be
;; drawn after the translucent terrain instead. The BE-pass renderer queues
;; the pool cells here; the loader's post-translucent stage hook drains the
;; queue via `draw-pending!` once per frame. Both run on the render thread,
;; so no synchronization is needed.
(def ^:private pending-tiles (atom #{}))

;; OverlayTexture.NO_OVERLAY = pack(0, 10) = 10 << 16. The stage hook has no
;; packed-overlay parameter (the BE pass did), and the sheets must not land in
;; the red "hurt" rows.
(def ^:private no-overlay 655360)

(defn- draw-cell!
  "Draw the three sheets for one pool cell at `tile`'s position.

  The stage pose-stack is camera-relative, so each cell is translated by
  tile-pos − camera-pos before drawing (the BE pass delivered a pre-translated
  stack; the stage does not).

  Mode semantics, all evaluated here:
  - :surface-flash (default) — sheets flush with each cell's local fluid
    surface, additive-blended with the boosted vertex alpha (visible over the
    opaque black surface), depth-tested (the pool's own surface and terrain
    occlude from the side — no hovering, no flash through dirt).
  - :upstream — upstream's exact state: depth test off
    (glDisable(GL_DEPTH_TEST)), sheets at the original heights; drawn after
    the translucent terrain, the flash composites over the black surface (the
    in-liquid look) and over terrain alike, exactly like upstream's pass-1
    TESRs.
  - :visibility-raycast — the same sheets, but `cell-visible?` skips cells
    whose camera→centre ray hits solid terrain, so the flash no longer shines
    through dirt and stone.
  - :depth-anchored — sheets anchor at the per-cell fluid surface (ownHeight)
    plus small above-surface offsets: the surface wrote depth, so the offsets
    keep every layer clear of it while solid terrain in front still occludes.
    Depth is never written (COLOR_WRITE), so the sheets composite with each
    other."
  [tile pose-stack buffer-source camera-pos]
  (let [alpha (distance-alpha tile)]
    (when (>= alpha min-visible-alpha)
      (when (or (not (visibility-raycast?))
                (cell-visible? tile))
        (let [p (pos/block-pos tile)
              dx (- (double (pos/pos-x p)) (double (:x camera-pos)))
              dy (- (double (pos/pos-y p)) (double (:y camera-pos)))
              dz (- (double (pos/pos-z p)) (double (:z camera-pos)))
              fluid-height (platform-be/get-fluid-height tile)
              ht (* 1.2 (Math/sqrt (max 0.0 fluid-height)))
              ;; :surface-flash boosts the vertex alpha so the low-alpha
              ;; texture reads over the opaque black surface (additive blend
              ;; multiplies the contribution by the alpha).
              alpha (if (surface-flash?)
                      (* alpha surface-flash-alpha-boost)
                      alpha)
              time (render/get-render-time)]
          (pose/push-pose pose-stack)
          (try
            (pose/translate pose-stack dx dy dz)
            (doseq [layer layer-defs]
              (when (or (nil? (:condition layer))
                        ((:condition layer) ht))
                (render-layer! pose-stack buffer-source
                               fullbright-packed-light no-overlay alpha
                               layer ht fluid-height time)))
            (finally
              (pose/pop-pose pose-stack))))))))

(defn draw-pending!
  "Draw the pool sheets queued by the block-entity pass, after the translucent
  terrain has rendered. Called per frame by the loader's post-translucent
  stage hook (forge/neoforge: RenderLevelStageEvent AFTER_TRANSLUCENT_BLOCKS /
  AFTER_PARTICLES; fabric: WorldRenderEvents AFTER_TRANSLUCENT) with the
  stage's pose-stack, buffer source and camera position. The stage fires
  several times per frame (the level renders the translucent layer in two
  passes), but the queue drains on the first call, so the later fires draw
  nothing.

  Returns the number of queued cells (0 when the queue was empty) so the
  caller can flush the buffer source only when geometry was actually
  submitted."
  [{:keys [pose-stack buffer-source camera-pos]}]
  (when (and pose-stack buffer-source camera-pos)
    (let [tiles (let [t @pending-tiles] (reset! pending-tiles #{}) t)]
      (doseq [tile tiles]
        (draw-cell! tile pose-stack buffer-source camera-pos))
      (count tiles))))

;; ---------------------------------------------------------------------------
;; Registration (cat_engine/render.clj pattern)
;; ---------------------------------------------------------------------------

(defn register!
  "Register the imag-phase TESR via the scripted tile renderer registry.

  The render callback does not draw: it queues the cell for `draw-pending!`,
  which runs in the post-translucent stage (the opaque fluid surface would
  cover BE-pass geometry)."
  []
  (tesr-api/register-scripted-tile-renderer!
    "imag-phase"
    {:render-tile (fn [tile-entity _partial-ticks _pose-stack _buffer-source
                       _packed-light _packed-overlay]
                    (swap! pending-tiles conj tile-entity))}))

(defn init!
  "Client-side renderer init hook. Called by the AC hook registry during
  client setup."
  []
  (machine-render-runtime/register-client-renderer-init!
    'cn.li.ac.block.imag-phase.render/register!))
