(ns cn.li.mc1201.client.effects.level-renderer
  "Shared client level-effect rendering core (Minecraft 1.20.1)."
  (:require [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcbase.runtime.raycast-normalize :as rn])
  (:import [com.mojang.blaze3d.vertex PoseStack VertexConsumer]
           [cn.li.mc1201.client.render ModRenderTypes]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]
           [cn.li.mcver McClientAccess]
           [cn.li.mc1201.runtime Raycast]
           [net.minecraft.core BlockPos]
           [net.minecraft.core.registries BuiltInRegistries Registries]
           [net.minecraft.client.renderer MultiBufferSource$BufferSource RenderType]
           [net.minecraft.client.renderer.texture OverlayTexture]
           [net.minecraft.resources ResourceLocation]
           [net.minecraft.tags BlockTags TagKey]
           [net.minecraft.world.entity.player Abilities]
           [net.minecraft.world.level.block Block]
           [net.minecraft.world.level.block.state BlockState]
           [net.minecraft.world.phys Vec3]
           [org.joml Matrix4f Vector3f]
           [cn.li.mcmod.math V3]))

(def ^:private full-bright-uv2 15728880)
(def ^:private default-walk-speed 0.1)
(def ^:private conventional-ore-tags*
  ;; Forge 1.20.1 and Fabric's common convention use different root ore tags.
  ;; Checking both recreates the original OreDictionary "ore" lookup without
  ;; importing either loader API into the shared Minecraft layer. Delay their
  ;; construction because touching Registries/BLOCK during AOT compilation
  ;; triggers Minecraft's bootstrap guard.
  (delay
    [(TagKey/create Registries/BLOCK (ResourceLocation. "forge:ores"))
     (TagKey/create Registries/BLOCK (ResourceLocation. "c:ores"))]))

(defn create-level-renderer-runtime
  []
  {::runtime ::level-renderer-runtime
   :last-applied-walk-speed* (atom {})})

(def ^:private level-renderer-runtime-atom (atom (create-level-renderer-runtime)))

(defn- level-renderer-runtime?
  [runtime]
  (and (map? runtime)
       (= ::level-renderer-runtime (::runtime runtime))
       (some? (:last-applied-walk-speed* runtime))))

(defn call-with-level-renderer-runtime
  "Set the level renderer runtime for the current context (primarily for testing)."
  [runtime f]
  (when-not (level-renderer-runtime? runtime)
    (throw (ex-info "Expected level renderer runtime"
                    {:runtime runtime})))
  (let [saved @level-renderer-runtime-atom]
    (try
      (reset! level-renderer-runtime-atom runtime)
      (f)
      (finally
        (reset! level-renderer-runtime-atom saved)))))

(defmacro with-level-renderer-runtime
  [runtime & body]
  `(call-with-level-renderer-runtime ~runtime (fn [] ~@body)))

(defn- current-level-renderer-runtime
  []
  @level-renderer-runtime-atom)

(defn- last-applied-walk-speed-atom
  []
  (:last-applied-walk-speed* (current-level-renderer-runtime)))

(defn- walk-speed-owner-key
  [owner]
  (client-session/owner-key owner))

(defn walk-speed-snapshot
  []
  @(last-applied-walk-speed-atom))

(defn reset-walk-speed-for-test!
  ([]
   (reset-walk-speed-for-test! {}))
  ([snapshot]
  (reset! (last-applied-walk-speed-atom) (or snapshot {}))
   nil))

(defn tick-level-effects!
  []
  (power-runtime/client-tick-level-effects!))

(defn current-fov-offset
  "Per-frame camera FOV offset (degrees) contributed by the local player's
  active level effects (meltdowner charge zoom). Read by the loader's
  ComputeFov handler."
  [player-uuid]
  (power-runtime/client-level-effect-fov-offset player-uuid))

(defn set-local-walk-speed!
  [^LocalPlayer player speed]
  (try
    (let [^Abilities abilities (.getAbilities player)]
      (.setWalkingSpeed abilities (float speed))
      (.onUpdateAbilities player))
    (catch Exception _
      nil)))

(defn clear-owner-walk-speed!
  ([owner]
   (clear-owner-walk-speed! owner nil))
  ([owner ^LocalPlayer player]
   (let [owner-key (walk-speed-owner-key owner)]
     (when (contains? @(last-applied-walk-speed-atom) owner-key)
       (when player
         (set-local-walk-speed! player default-walk-speed))
       (swap! (last-applied-walk-speed-atom) dissoc owner-key)))
   nil))

(defn apply-local-walk-speed-from-plan!
  ([^LocalPlayer player plan]
   (when-let [owner (client-session/current-local-player-owner)]
     (apply-local-walk-speed-from-plan! owner player plan)))
  ([owner ^LocalPlayer player plan]
   (let [owner-key (walk-speed-owner-key owner)
         target-speed (:local-walk-speed plan)]
     (if (number? target-speed)
       (let [spd (double target-speed)]
         (when (not= (get @(last-applied-walk-speed-atom) owner-key) spd)
           (set-local-walk-speed! player spd)
           (swap! (last-applied-walk-speed-atom) assoc owner-key spd)))
       (clear-owner-walk-speed! owner player)))))

(defn- local-camera-first-person?
  "True while the local camera is in first person (F5 not toggled out).

  This is the port of the original's ViewOptimize.isFirstPerson gameSettings
  half; effect code pairs it with an is-this-my-effect check on the uuid.
  Both loaders' level-effect renderers call render-level-plan! below, so
  reading it here from Minecraft's own options is all the wiring it needs."
  []
  (if-let [^Minecraft mc (Minecraft/getInstance)]
    (.isFirstPerson (.getCameraType (.-options mc)))
    true))

(defn- client-world-fns
  "The client's own view of the world, for effects whose ORIGINAL computes
  itself client-side each tick (MarkTeleport's aim marker: MTContextC.l_update
  calls getDest against the client world and CPData). Raycast's helpers take a
  Level, so the client level answers them with the same code the server uses --
  nothing here reimplements a trace."
  [^LocalPlayer player]
  {:raycast-from-view
   (fn [max-distance living-only?]
     (try
       (rn/normalize-bridge-map
         (Raycast/raycastCombinedFromPlayer player (double max-distance) (boolean living-only?)))
       (catch Exception _ nil)))
   :raycast-combined-excluding-from
   (fn [sx sy sz dx dy dz max-distance]
     (try
       (rn/normalize-bridge-map
         (Raycast/raycastCombinedExcluding
           (.level player)
           (double sx) (double sy) (double sz)
           (double dx) (double dy) (double dz)
           (double max-distance)
           (str (.getUUID player))))
       (catch Exception _ nil)))
   :block-solid-at?
   (fn [x y z]
     (try
       (let [level (.level player)
             state (.getBlockState level (BlockPos. (int x) (int y) (int z)))]
         (not (.isAir state)))
       (catch Exception _ false)))
   ;; PenetrateTeleport's hasPlace asks canCollideCheck, not isAir -- grass and
   ;; torches are not air but you can stand in them. Mirrors the server-side
   ;; block-collidable?.
   :block-collidable-at?
   (fn [x y z]
     (try
       (let [level (.level player)
             pos (BlockPos. (int x) (int y) (int z))
             state (.getBlockState level pos)]
         (not (.isEmpty (.getCollisionShape state level pos))))
       (catch Exception _ false)))})

(defn hand-center-pos
  "Local player's hand position, plus the view context effect code needs to
  recognise its own player's effects (`:player-uuid`) and which of the
  original's two ViewOptimize offsets applies (`:first-person?`).

  `:player-*` is the player's PARTIAL-TICK interpolated pose — the position
  the character model is drawn at this frame. Anchoring body-level effects
  (storm-wing tornadoes etc.) to the raw tick position makes them step ahead
  of the rendered character while moving: the upstream StormWingEffect is a
  world entity whose renderer interpolates between the same two player tick
  positions, so it always tracks the model."
  [^LocalPlayer player]
  (let [^Vec3 look (.getLookAngle player)
        yaw-rad (Math/toRadians (double (.getYRot player)))
        right-x (Math/cos yaw-rad)
        right-z (Math/sin yaw-rad)
        [base-x base-y base-z eye-y yaw-deg pitch-deg body-yaw-deg]
        (McClientAccess/playerRenderPosition
         player (McClientAccess/clientPartialTick (Minecraft/getInstance)))]
    {:player-uuid (str (.getUUID player))
     :first-person? (local-camera-first-person?)
     :player-x (double base-x)
     :player-y (double base-y)
     :player-z (double base-z)
     :player-eye-y (double eye-y)
     :player-width (.getBbWidth player)
     :player-height (.getBbHeight player)
     :player-yaw-rad (Math/toRadians (double yaw-deg))
     ;; Body yaw (the original's renderYawOffset): effects anchored to the
     ;; torso must not swing when only the head turns.
     :player-body-yaw-rad (Math/toRadians (double body-yaw-deg))
     :player-pitch-rad (Math/toRadians (double pitch-deg))
     :x (+ (double base-x) (* (.-x look) 0.35) (* right-x 0.22))
     :y (+ (double eye-y) -0.22 (* (.-y look) 0.06))
     :z (+ (double base-z) (* (.-z look) 0.35) (* right-z 0.22))}))

(defn- block-id-for-state
  [^BlockState block-state]
  (let [^Block block (.getBlock block-state)
        key (.getKey BuiltInRegistries/BLOCK block)]
    (when key (str key))))

(defn- harvest-level-for-state
  [^BlockState block-state]
  (cond
    (.is block-state BlockTags/NEEDS_DIAMOND_TOOL) 3
    (.is block-state BlockTags/NEEDS_IRON_TOOL) 2
    (.is block-state BlockTags/NEEDS_STONE_TOOL) 1
    :else 0))

(defn- conventionally-tagged-ore?
  [^BlockState block-state]
  (boolean
    (some (fn [^TagKey tag-key]
            (.is block-state tag-key))
          @conventional-ore-tags*)))

(defn- block-predicate-matches?
  [block-predicate block-id metadata]
  (try
    (boolean (block-predicate block-id metadata))
    (catch clojure.lang.ArityException _
      ;; Preserve the original one-argument nearby-block query contract for
      ;; effects that do not consume platform metadata.
      (boolean (block-predicate block-id)))))

(defn- make-nearby-block-query-fn
  [^LocalPlayer player]
  (fn [x y z radius block-predicate]
    (try
      (let [level (.level player)
            x* (double x)
            y* (double y)
            z* (double z)
            radius* (double radius)
            radius-sq (* radius* radius*)
            min-x (int (Math/floor (- x* radius*)))
            min-y (int (Math/floor (- y* radius*)))
            min-z (int (Math/floor (- z* radius*)))
            max-x (int (Math/ceil (+ x* radius*)))
            max-y (int (Math/ceil (+ y* radius*)))
            max-z (int (Math/ceil (+ z* radius*)))]
        (if (or (nil? level) (not (pos? radius*)))
          []
          (loop [bx min-x
                 acc []]
            (if (> bx max-x)
              acc
              (recur (inc bx)
                     (loop [by min-y
                            acc2 acc]
                       (if (> by max-y)
                         acc2
                         (recur (inc by)
                                (loop [bz min-z
                                       acc3 acc2]
                                  (if (> bz max-z)
                                    acc3
                                    (let [dx (- (double bx) x*)
                                          dy (- (double by) y*)
                                          dz (- (double bz) z*)
                                          dist-sq (+ (* dx dx) (* dy dy) (* dz dz))]
                                      (if (> dist-sq radius-sq)
                                        (recur (inc bz) acc3)
                                        (let [pos (BlockPos. bx by bz)
                                              ^BlockState block-state (.getBlockState level pos)
                                              block-id (block-id-for-state block-state)
                                              metadata {:ore-tagged? (conventionally-tagged-ore? block-state)}]
                                          (if (and block-id
                                                   (block-predicate-matches? block-predicate block-id metadata))
                                            (recur (inc bz)
                                                   (conj acc3 {:x bx
                                                               :y by
                                                               :z bz
                                                               :block-id block-id
                                                               :harvest-level (harvest-level-for-state block-state)}))
                                            (recur (inc bz) acc3)))))))))))))))
      (catch Exception _
        []))))

(defn- color-channel-255
  "Normalize one color channel to an int in [0,255].

  Accepts either byte-style values (0..255) or unit values (0.0..1.0)."
  [v default-v]
  (let [raw (if (number? v) (double v) (double default-v))
        scaled (if (<= 0.0 raw 1.0)
                 (* raw 255.0)
                 raw)
        clamped (max 0.0 (min 255.0 scaled))]
    (int (Math/round clamped))))

(defn- map-color->channels
  [m]
  (let [r (or (:r m) (get m "r"))
        g (or (:g m) (get m "g"))
        b (or (:b m) (get m "b"))
        a (or (:a m) (get m "a") 255)]
    [(color-channel-255 a 255)
     (color-channel-255 r 255)
     (color-channel-255 g 255)
     (color-channel-255 b 255)]))

(defn- vec-color->channels
  [color-vec]
  (let [[r g b a] (concat (take 4 color-vec) [nil nil nil 255])]
    [(color-channel-255 a 255)
     (color-channel-255 r 255)
     (color-channel-255 g 255)
     (color-channel-255 b 255)]))

(defn- color-int→channels
  "Render ops may carry color as packed ARGB int or legacy RGBA map/vector.
   Minecraft's VertexConsumer expects separate RGBA int channels."
  [color]
  (cond
    (number? color)
    (let [c (long color)]
      [(unchecked-int (bit-and (bit-shift-right c 24) 0xFF))      ;; a
       (unchecked-int (bit-and (bit-shift-right c 16) 0xFF))      ;; r
       (unchecked-int (bit-and (bit-shift-right c 8)  0xFF))      ;; g
       (unchecked-int (bit-and c 0xFF))])                         ;; b

    (map? color)
    (map-color->channels color)

    (sequential? color)
    (vec-color->channels color)

    :else
    [255 255 255 255]))

(defn- emit-line-vertex!
  [^VertexConsumer vc mat x y z r g b a]
  (-> vc
      (.vertex mat (float x) (float y) (float z))
      (.color (int r) (int g) (int b) (int a))
      (.normal 0.0 1.0 0.0)
      (.endVertex)))

(defn- emit-line!
  [^VertexConsumer vc mat {:keys [^V3 p1 ^V3 p2 color]}]
  (let [[a r g b] (color-int→channels color)]
    (emit-line-vertex! vc mat (.-x p1) (.-y p1) (.-z p1) r g b a)
    (emit-line-vertex! vc mat (.-x p2) (.-y p2) (.-z p2) r g b a)))

(defn- emit-quad-vertex!
  [^VertexConsumer vc mat ^V3 p u v color]
  (let [[a r g b] (color-int→channels color)]
    (-> vc
        (.vertex mat (float (.-x p)) (float (.-y p)) (float (.-z p)))
        (.color (int r) (int g) (int b) (int a))
        (.uv (float u) (float v))
        (.overlayCoords (int OverlayTexture/NO_OVERLAY))
        (.uv2 (int full-bright-uv2))
        (.normal 0.0 1.0 0.0)
        (.endVertex))))

(defn- emit-quad!
  "Emit ONE quad as exactly 4 vertices.

  The buffer comes from RenderType/entityTranslucent, whose vertex mode is
  QUADS: the builder slices the stream into primitives every 4 vertices,
  ignoring where each op started. Emitting the 6-vertex two-triangle form
  here pushed every following op out of phase with that grouping, so quads
  got assembled from a mix of one op's trailing corners and the next op's
  leading ones — the arc rendered as scattered garbage polygons instead of
  ribbons.

  p0/p1 are the two corners at the segment's start and p2/p3 at its end (see
  render-util's beam quads), so `u` runs along the beam and `v` across its
  width."
  [^VertexConsumer vc mat {:keys [p0 p1 p2 p3 u0 u1 v0 v1 color]}]
  (emit-quad-vertex! vc mat p0 u0 v0 color)
  (emit-quad-vertex! vc mat p1 u0 v1 color)
  (emit-quad-vertex! vc mat p2 u1 v1 color)
  (emit-quad-vertex! vc mat p3 u1 v0 color))

(defn- sort-ops
  "Single pass over `ops`, bucketing into {:lines [...] :translucent-lines [...]
  :quads {texture [...]} :plasma [...]} with transients / LinkedHashMap —
  avoids per-op persistent conj/update.

  The map has to keep INSERTION order. Quads are batched per texture, and
  translucent geometry writes depth, so whichever texture is drawn first wins
  the depth test where they overlap. A plain HashMap made that order arbitrary:
  a ray's glow boards and its cylinders are different textures, and when the
  boards happened to go last the wide flat glow sat on top of the round tube
  and the whole beam read as a sheet. Insertion order is the order the effect
  emitted its ops, which is the order upstream draws them in."
  [ops]
  (let [lines (transient [])
        tlines (transient [])
        plasma (transient [])
        ^java.util.LinkedHashMap quads-t (java.util.LinkedHashMap.)]
    (doseq [op ops]
      (case (:kind op)
        :line (if (:translucent? op)
                (conj! tlines op)
                (conj! lines op))
        :quad (let [tex (:texture op)
                    bucket (or (.get quads-t tex)
                               (let [b (transient [])]
                                 (.put quads-t tex b)
                                 b))]
                (conj! bucket op))
        :plasma-body (conj! plasma op)
        nil))
    {:lines (persistent! lines)
     :translucent-lines (persistent! tlines)
     ;; array-map preserves insertion order for the small number of textures a
     ;; frame's effects use; into {} would rehash and lose it again.
     :quads (reduce (fn [m ^java.util.Map$Entry e]
                      (assoc m (.getKey e) (persistent! (.getValue e))))
                    (array-map)
                    (.entrySet quads-t))
     :plasma (persistent! plasma)}))

;; ---------------------------------------------------------------------------
;; Plasma-body ray-march shader (vanilla Minecraft API only)
;; ---------------------------------------------------------------------------

(defn- map->v3
  "Convert a {:x :y :z} map (crossing from the shared map-based level-effect
  plan context) into a V3 for zero-allocation local math."
  ^V3 [{:keys [x y z]}]
  (V3. (double (or x 0.0)) (double (or y 0.0)) (double (or z 0.0))))

(def ^:private ball-matrix-uniform-names
  "Precomputed \"balls0\".. \"balls3\" uniform names — avoids 4 string
  concatenations per frame in `set-plasma-uniforms!`. 16 balls are packed as
  4 mat4 uniforms (one vec4 ball per column): the vanilla 1.20.1
  ShaderInstance JSON loader only recognizes int/float/matrix uniform types."
  (mapv (fn [i] (str "balls" i)) (range 4)))

(defn- set-plasma-uniforms!
  "Set the plasma ray-march uniforms. Ball positions are WORLD coordinates
  transformed into camera space by the ModelView matrix — matching upstream
  PlasmaBodyEffect's Matrix4f.transform(pos) + negated z, so the density
  field lives in the same space as the fragment's `camspace`."
  [^Matrix4f mat {:keys [alpha balls]}]
  (when-let [shader (ModRenderTypes/getPlasmaBodyShader)]
    ;; `balls` may be a lazy seq upstream — vec once so the 16x `nth` below is O(1)
    ;; each, not O(n) per call against a non-indexed seq.
    (let [balls-vec (vec (take 16 (or balls [])))
          ball-count (count balls-vec)]
      (when-let [uniform (.getUniform shader "ballCount")]
        (.set uniform (int ball-count)))
      (when-let [uniform (.getUniform shader "alpha")]
        (.set uniform (float (double (or alpha 0.0)))))
      (doseq [mat-idx (range 4)]
        (when-let [uniform (.getUniform shader (nth ball-matrix-uniform-names mat-idx))]
          (let [row (float-array 16)]
            (doseq [col (range 4)]
              (let [ball-idx (+ (* mat-idx 4) col)
                    {:keys [x y z size]} (or (nth balls-vec ball-idx nil) {})
                    base (* col 4)
                    cam (doto (Vector3f. (float (double (or x 0.0)))
                                         (float (double (or y 0.0)))
                                         (float (double (or z 0.0))))
                          (.mulPosition mat))]
                (aset row base (.-x cam))
                (aset row (inc base) (.-y cam))
                (aset row (+ base 2) (float (- (.-z cam))))
                (aset row (+ base 3) (float (double (or size 0.0))))))
            ;; setMat4x4 uploads a SINGLE matrix (glUniformMatrix4fv count 1);
            ;; set(float[]) would upload `count` matrices and corrupt the
            ;; uniform value.
            (.setMat4x4 uniform
                       (aget row 0) (aget row 1) (aget row 2) (aget row 3)
                       (aget row 4) (aget row 5) (aget row 6) (aget row 7)
                       (aget row 8) (aget row 9) (aget row 10) (aget row 11)
                       (aget row 12) (aget row 13) (aget row 14) (aget row 15))))))))

(defn- emit-plasma-vertex! [^VertexConsumer vc mat ^V3 p]
  (-> vc
      (.vertex mat (float (.-x p)) (float (.-y p)) (float (.-z p)))
      (.endVertex)))

(def ^:private world-up (V3. 0.0 1.0 0.0))
(def ^:private axis-x (V3. 1.0 0.0 0.0))

(defn- emit-plasma-quad!
  [^VertexConsumer vc mat cam-pos {:keys [center]}]
  (let [^V3 cam (map->v3 cam-pos)
        ^V3 center (if center (map->v3 center) cam)
        to-cam (V3/normalize (V3/sub cam center))
        right-raw (V3/cross world-up to-cam)
        right (if (< (+ (Math/abs (.-x right-raw))
                        (Math/abs (.-y right-raw))
                        (Math/abs (.-z right-raw)))
                     1.0e-6)
                axis-x
                (V3/normalize right-raw))
        up (V3/normalize (V3/cross to-cam right))
        ;; The quad is the ray-march SAMPLING WINDOW, not the ball itself —
        ;; the ball renders inside it as a 3D density field, so a ball-sized
        ;; quad reads as a flat square instead of a floating orb. Upstream
        ;; draws a unit billboard (createBillboard(-.5,-.5,.5,.5)) scaled by
        ;; size=22, i.e. 22 blocks across: an 11-block half-size, not 20.
        half-size 11.0
        side (V3/scale right half-size)
        lift (V3/scale up half-size)
        p0 (V3/add (V3/sub center side) lift)
        p1 (V3/add (V3/add center side) lift)
        p2 (V3/sub (V3/add center side) lift)
        p3 (V3/sub (V3/sub center side) lift)]
    ;; ModRenderTypes/plasmaBody is a QUADS render type — 4 vertices per
    ;; primitive, not a triangle pair.
    (emit-plasma-vertex! vc mat p0)
    (emit-plasma-vertex! vc mat p1)
    (emit-plasma-vertex! vc mat p2)
    (emit-plasma-vertex! vc mat p3)))

(defn- render-plasma-op!
  [{:keys [^MultiBufferSource$BufferSource buffer-source mat camera-pos op]}]
  (set-plasma-uniforms! mat op)
  (let [rtype (ModRenderTypes/plasmaBody)
        ^VertexConsumer plasma-vc (.getBuffer buffer-source rtype)]
    (emit-plasma-quad! plasma-vc mat camera-pos op)
    (.endBatch buffer-source rtype)))

(defn render-level-plan!
  [{:keys [^LocalPlayer player
           ^PoseStack pose-stack
           ^MultiBufferSource$BufferSource buffer-source
           camera-pos
           tick]}]
  (let [owner (client-session/current-local-player-owner)
        ;; Skip hand-center-pos/query-fn allocation and the plan build itself
        ;; when no level effect is active (idle skill) — checked first so the
        ;; common (idle) frame does none of the below.
        plan (when (power-runtime/client-level-effects-active?)
               (power-runtime/client-build-level-effect-plan
                 camera-pos (merge (hand-center-pos player) (client-world-fns player)) tick (make-nearby-block-query-fn player)))]
    (when owner
      (apply-local-walk-speed-from-plan! owner player plan))
    (when (seq (:ops plan))
      (let [{:keys [lines translucent-lines quads plasma]} (sort-ops (:ops plan))]
        (.pushPose pose-stack)
        (.translate pose-stack
                    (double (- (:x camera-pos)))
                    (double (- (:y camera-pos)))
                    (double (- (:z camera-pos))))
        (let [mat (.pose (.last pose-stack))]
          (when (seq lines)
            (let [^VertexConsumer line-vc (.getBuffer buffer-source (RenderType/lines))]
              (doseq [op lines]
                (emit-line! line-vc mat op))))
          (when (seq translucent-lines)
            ;; The render type owns the line-width GL state via its
            ;; LineStateShard — no raw GL calls here.
            (let [^VertexConsumer tline-vc (.getBuffer buffer-source (ModRenderTypes/academyLinesTranslucent))]
              (doseq [op translucent-lines]
                (emit-line! tline-vc mat op))))
          (doseq [[texture texture-ops] quads]
            (when-let [loc (ResourceLocation/tryParse texture)]
              (let [no-fog-ops (filter :no-fog? texture-ops)
                    depth-ops (remove #(or (:no-depth-test? %) (:no-depth-write? %) (:no-fog? %)) texture-ops)
                    ;; Upstream MarkRender disables depth test + cull for the
                    ;; tp_mark humanoid so it stays visible through walls.
                    no-depth-ops (filter #(and (:no-depth-test? %)
                                               (not (:no-fog? %)))
                                         texture-ops)
                    ;; Depth-tested but not depth-writing: upstream's
                    ;; SubArcHandler.drawAll batch (glDepthMask(false)).
                    read-only-ops (filter #(and (:no-depth-write? %)
                                                (not (:additive? %))
                                                (not (:no-depth-test? %))
                                                (not (:no-fog? %)))
                                          texture-ops)
                    ;; The ray glow boards: also depth-tested and
                    ;; non-writing, but ADDITIVE — a halo is light, not smoke.
                    additive-ops (filter :additive? texture-ops)]
                (when (seq depth-ops)
                  (let [^VertexConsumer quad-vc (.getBuffer buffer-source (RenderType/entityTranslucent loc))]
                    (doseq [op depth-ops]
                      (emit-quad! quad-vc mat op))))
                ;; Depth test on, depth WRITE off — vanilla entityNoOutline is
                ;; entityTranslucent with COLOR_WRITE, i.e. exactly what
                ;; upstream's SubArcHandler.drawAll gets from glDepthMask(false).
                (when (seq read-only-ops)
                  (let [^VertexConsumer ro-vc (.getBuffer buffer-source (RenderType/entityNoOutline loc))]
                    (doseq [op read-only-ops]
                      (emit-quad! ro-vc mat op))))
                (when (seq additive-ops)
                  (let [^VertexConsumer ad-vc (.getBuffer buffer-source (ModRenderTypes/academyQuadsAdditive loc))]
                    (doseq [op additive-ops]
                      (emit-quad! ad-vc mat op))))
                (when (seq no-depth-ops)
                  (let [^VertexConsumer nd-vc (.getBuffer buffer-source (ModRenderTypes/academyQuadsTranslucent loc))]
                    (doseq [op no-depth-ops]
                      (emit-quad! nd-vc mat op))))
                ;; Fog-free quads (MineDetect ore highlights): upstream
                ;; HandlerRender disables GL_FOG for the mineview pass so the
                ;; boxes stay visible through the skill's own blindness fog.
                (when (seq no-fog-ops)
                  (let [^VertexConsumer nf-vc (.getBuffer buffer-source (ModRenderTypes/academyQuadsNoFog loc))]
                    (doseq [op no-fog-ops]
                      (emit-quad! nf-vc mat op)))))))
          (when (seq plasma)
            (doseq [op plasma]
              (render-plasma-op! {:buffer-source buffer-source
                                  :mat mat
                                  :camera-pos camera-pos
                                  :op op})))
          (.popPose pose-stack)
          (.endBatch buffer-source))))
    plan))
