(ns cn.li.mc262.client.effects.level-renderer
  "Shared client level-effect core for Minecraft 26.2.

   Effect plans are extracted before level submission and emitted through
   SubmitNodeCollector, with all GPU state owned by RenderType."
  (:require [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcbase.runtime.raycast-normalize :as rn]
            [cn.li.mc262.runtime.registry :as registry])
  (:import [com.mojang.blaze3d.vertex PoseStack PoseStack$Pose VertexConsumer]
           [cn.li.mc262.client.effects LevelEffectGeometry]
           [cn.li.mc262.client.render PlasmaRenderTypes]
           [cn.li.mcmod.math V3]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]
           [cn.li.mcver McClientAccess]
           [cn.li.mc262.runtime Raycast]
           [net.minecraft.client.renderer SubmitNodeCollector SubmitNodeCollector$CustomGeometryRenderer]
           [net.minecraft.client.renderer.rendertype RenderType RenderTypes]
           [net.minecraft.core BlockPos]
           [net.minecraft.core.registries Registries]
           [net.minecraft.resources Identifier]
           [net.minecraft.tags BlockTags TagKey]
           [net.minecraft.world.entity.player Abilities]
           [net.minecraft.world.level.block Block]
           [net.minecraft.world.level.block.state BlockState]
           [net.minecraft.world.phys Vec3]
           [cn.li.mcver ResourceLocations]))

(def ^:private default-walk-speed 0.1)
(def ^:private conventional-ore-tags*
  (delay
    [(TagKey/create Registries/BLOCK (ResourceLocations/parse "c:ores"))
     (TagKey/create Registries/BLOCK (ResourceLocations/parse "forge:ores"))
     (TagKey/create Registries/BLOCK (ResourceLocations/parse "neoforge:ores"))]))

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
     :player-body-yaw-rad (Math/toRadians (double body-yaw-deg))
     :player-pitch-rad (Math/toRadians (double pitch-deg))
     :x (+ (double base-x) (* (.-x look) 0.35) (* right-x 0.22))
     :y (+ (double eye-y) -0.22 (* (.-y look) 0.06))
     :z (+ (double base-z) (* (.-z look) 0.35) (* right-z 0.22))}))

(defn- block-id-for-state
  [^BlockState block-state]
  (let [^Block block (.getBlock block-state)
        key (.getKey (registry/builtin "BLOCK") block)]
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
      (boolean (block-predicate block-id)))))

(defn make-nearby-block-query-fn
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
  [v]
  (let [r (nth v 0 255)
        g (nth v 1 255)
        b (nth v 2 255)
        a (nth v 3 255)]
    [(color-channel-255 a 255)
     (color-channel-255 r 255)
     (color-channel-255 g 255)
     (color-channel-255 b 255)]))

(defn- color-int→channels
  [color]
  (let [c (int color)]
    [(bit-and (unsigned-bit-shift-right c 24) 0xff)
     (bit-and (unsigned-bit-shift-right c 16) 0xff)
     (bit-and (unsigned-bit-shift-right c 8) 0xff)
     (bit-and c 0xff)]))

(defn- emit-line-vertex!
  [^VertexConsumer consumer ^PoseStack$Pose pose x y z r g b a]
  (LevelEffectGeometry/lineVertex
    consumer pose (float x) (float y) (float z)
    (int r) (int g) (int b) (int a)))

(defn- emit-line!
  [^VertexConsumer consumer ^PoseStack$Pose pose {:keys [^V3 p1 ^V3 p2 color]}]
  (let [[a r g b] (color-int→channels color)]
    (emit-line-vertex! consumer pose (.-x p1) (.-y p1) (.-z p1) r g b a)
    (emit-line-vertex! consumer pose (.-x p2) (.-y p2) (.-z p2) r g b a)))

(defn- emit-quad-vertex!
  [^VertexConsumer consumer ^PoseStack$Pose pose ^V3 p u v color]
  (let [[a r g b] (color-int→channels color)]
    (LevelEffectGeometry/texturedVertex
      consumer pose
      (float (.-x p)) (float (.-y p)) (float (.-z p))
      (float u) (float v)
      (int r) (int g) (int b) (int a))))

(defn- emit-quad!
  [^VertexConsumer consumer ^PoseStack$Pose pose
   {:keys [p0 p1 p2 p3 u0 u1 v0 v1 color]}]
  (emit-quad-vertex! consumer pose p0 u0 v0 color)
  (emit-quad-vertex! consumer pose p1 u0 v1 color)
  (emit-quad-vertex! consumer pose p2 u1 v1 color)
  (emit-quad-vertex! consumer pose p3 u1 v0 color))

(defn- sort-ops
  [ops]
  (let [lines (transient [])
        plasma (transient [])
        ^java.util.LinkedHashMap quads-t (java.util.LinkedHashMap.)]
    (doseq [op ops]
      (case (:kind op)
        :line (conj! lines op)
        :quad (let [texture (:texture op)
                    bucket (or (.get quads-t texture)
                               (let [created (transient [])]
                                 (.put quads-t texture created)
                                 created))]
                (conj! bucket op))
        :plasma-body (conj! plasma op)
        nil))
    {:lines (persistent! lines)

     ;; array-map preserves insertion order for the small number of textures a
     ;; frame's effects use; into {} would rehash and lose it again.
     :quads (reduce (fn [result ^java.util.Map$Entry entry]
                      (assoc result (.getKey entry)
                             (persistent! (.getValue entry))))
                    (array-map)
                    (.entrySet quads-t))
     :plasma (persistent! plasma)}))

(defn- map->v3
  ^V3 [{:keys [x y z]}]
  (V3. (double (or x 0.0))
       (double (or y 0.0))
       (double (or z 0.0))))

(def ^:private world-up (V3. 0.0 1.0 0.0))
(def ^:private axis-x (V3. 1.0 0.0 0.0))

(defn- billboard-basis
  [camera-pos center]
  (let [^V3 camera (map->v3 camera-pos)
        ^V3 center-v3 (map->v3 center)
        to-camera (V3/normalize (V3/sub camera center-v3))
        right-raw (V3/cross world-up to-camera)
        right (if (< (+ (Math/abs (.-x right-raw))
                        (Math/abs (.-y right-raw))
                        (Math/abs (.-z right-raw)))
                     1.0e-6)
                axis-x
                (V3/normalize right-raw))
        up (V3/normalize (V3/cross to-camera right))]
    [center-v3 right up]))

(def ^:private plasma-slices
  "Six back-to-front field samples. UV.x's integer band carries the sample
   index, leaving its fractional position available as the billboard coordinate.

   This is intentionally still cheaper than 1.21.1's 20-step ray march. Unlike
   the old 26.2 approximation, however, every sample evaluates a primary ball
   plus its two nearest neighbours, so small clusters share a metaball field
   instead of merely stacking unrelated noisy billboards."
  [{:index 0 :depth -1.50}
   {:index 1 :depth -0.90}
   {:index 2 :depth -0.30}
   {:index 3 :depth 0.30}
   {:index 4 :depth 0.90}
   {:index 5 :depth 1.50}])

(def ^:private plasma-field-half-size 1.8)
(def ^:private encoded-neighbor-range 4.0)

(defn- clamp-double
  ^double [value min-value max-value]
  (max (double min-value) (min (double max-value) (double value))))

(defn- encode-neighbor-axis
  "Encode an offset measured in primary-ball radii into one UNORM8 colour
   channel. The shader reverses this mapping to [-4, 4]."
  [value]
  (int
    (Math/round
      (* 255.0
         (/ (+ (clamp-double value
                             (- encoded-neighbor-range)
                             encoded-neighbor-range)
               encoded-neighbor-range)
            (* 2.0 encoded-neighbor-range))))))

(defn- ball-radius
  ^double [ball]
  (max 0.05 (double (or (:size ball) 0.5))))

(defn- distance-squared
  ^double [^V3 a ^V3 b]
  (let [dx (- (.-x a) (.-x b))
        dy (- (.-y a) (.-y b))
        dz (- (.-z a) (.-z b))]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn- nearest-neighbors
  [balls ball-index ^V3 center]
  (->> balls
       (keep-indexed
         (fn [candidate-index candidate]
           (when (not= ball-index candidate-index)
             (let [candidate-center (map->v3 candidate)]
               {:ball candidate
                :center candidate-center
                :distance (distance-squared center candidate-center)}))))
       (sort-by :distance)
       (take 2)
       vec))

(defn- radius-ratio-code
  "Quantize neighbour/primary radius to four UV.y bands. Zero means no
   neighbour; the shader maps 1..4 to 0.5, 1.0, 1.5, and 2.0."
  [ratio]
  (cond
    (< ratio 0.75) 1
    (< ratio 1.25) 2
    (< ratio 1.75) 3
    :else 4))

(defn- encoded-neighbor
  [neighbor-entry ^V3 center ^V3 right ^V3 up ^V3 to-camera radius]
  (if-let [{neighbor :ball
            ^V3 neighbor-center :center} neighbor-entry]
    (let [delta (V3/sub neighbor-center center)
          inverse-radius (/ 1.0 radius)]
      {:r (encode-neighbor-axis (* (V3/dot delta right) inverse-radius))
       :g (encode-neighbor-axis (* (V3/dot delta up) inverse-radius))
       :b (encode-neighbor-axis (* (V3/dot delta to-camera) inverse-radius))
       :ratio-code (radius-ratio-code (/ (ball-radius neighbor) radius))})
    {:r 128 :g 128 :b 128 :ratio-code 0}))

(defn- normal-payload-axis
  "Map an encoded UNORM8 neighbour axis to the signed-normal range consumed by
   the ENTITY vertex format. The shader scales it back to [-4, 4]."
  [encoded-axis]
  (- (* 2.0 (/ (double encoded-axis) 255.0)) 1.0))

(defn- neighbor-payload
  [balls ball-index ^V3 center ^V3 right ^V3 up ^V3 to-camera radius]
  (let [[first-neighbor second-neighbor]
        (nearest-neighbors balls ball-index center)
        first-payload
        (encoded-neighbor first-neighbor center right up to-camera radius)
        second-payload
        (encoded-neighbor second-neighbor center right up to-camera radius)]
    {:r (:r first-payload)
     :g (:g first-payload)
     :b (:b first-payload)
     :ratio-code (:ratio-code first-payload)
     :nx (normal-payload-axis (:r second-payload))
     :ny (normal-payload-axis (:g second-payload))
     :nz (normal-payload-axis (:b second-payload))
     :second-ratio-code (:ratio-code second-payload)}))

(defn- emit-plasma-vertex!
  [^VertexConsumer consumer ^PoseStack$Pose pose ^V3 p u v r g b a nx ny nz]
  (LevelEffectGeometry/plasmaVertex
    consumer pose
    (float (.-x p)) (float (.-y p)) (float (.-z p))
    (float u) (float v)
    (int r) (int g) (int b) (int a)
    (float nx) (float ny) (float nz)))

(defn- emit-plasma-slice!
  [^VertexConsumer consumer ^PoseStack$Pose pose
   ^V3 center ^V3 right ^V3 up ^V3 to-camera radius alpha
   {:keys [r g b ratio-code nx ny nz second-ratio-code]}
   {:keys [index depth]}]
  (let [uv-u-base (* 2.0 (double index))
        radius-pair-code (+ (int ratio-code) (* 5 (int second-ratio-code)))
        uv-v-base (* 2.0 (double radius-pair-code))
        slice-center (V3/add center (V3/scale to-camera (* radius (double depth))))
        side (V3/scale right (* radius plasma-field-half-size))
        lift (V3/scale up (* radius plasma-field-half-size))
        p0 (V3/add (V3/sub slice-center side) lift)
        p1 (V3/add (V3/add slice-center side) lift)
        p2 (V3/sub (V3/add slice-center side) lift)
        p3 (V3/sub (V3/sub slice-center side) lift)]
    (emit-plasma-vertex! consumer pose p0 uv-u-base uv-v-base r g b alpha nx ny nz)
    (emit-plasma-vertex! consumer pose p1 (inc uv-u-base) uv-v-base r g b alpha nx ny nz)
    (emit-plasma-vertex! consumer pose p2 (inc uv-u-base) (inc uv-v-base) r g b alpha nx ny nz)
    (emit-plasma-vertex! consumer pose p3 uv-u-base (inc uv-v-base) r g b alpha nx ny nz)))

(defn- emit-plasma-ball!
  [^VertexConsumer consumer ^PoseStack$Pose pose camera-pos
   balls ball-index ball alpha]
  (let [[^V3 center ^V3 right ^V3 up] (billboard-basis camera-pos ball)
        ^V3 camera (map->v3 camera-pos)
        to-camera (V3/normalize (V3/sub camera center))
        radius (ball-radius ball)
        payload (neighbor-payload
                  balls ball-index center right up to-camera radius)]
    (doseq [slice plasma-slices]
      (emit-plasma-slice!
        consumer pose center right up to-camera radius alpha payload slice))))

(defn- emit-plasma-op!
  [^VertexConsumer consumer ^PoseStack$Pose pose camera-pos
   {:keys [center radius alpha balls]}]
  (let [alpha-channel (color-channel-255 alpha 1.0)
        visible-balls (vec
                        (or (seq (take 16 balls))
                            [(assoc (or center {:x 0.0 :y 0.0 :z 0.0})
                                    :size (double (or radius 0.75)))]))]
    (doseq [[ball-index ball] (map-indexed vector visible-balls)]
      (emit-plasma-ball!
        consumer pose camera-pos visible-balls ball-index ball alpha-channel))))

(defn- submit-custom-geometry!
  [^SubmitNodeCollector collector ^PoseStack pose-stack
   ^RenderType render-type render-fn]
  (.submitCustomGeometry
    collector pose-stack render-type
    (reify SubmitNodeCollector$CustomGeometryRenderer
      (render [_ pose consumer]
        (render-fn pose consumer)))))

(defn extract-level-plan!
  "Build the immutable render plan during ExtractLevelRenderStateEvent.
   World queries and player-speed synchronization happen only in extraction."
  [{:keys [^LocalPlayer player camera-pos tick]}]
  (let [owner (client-session/current-local-player-owner)
        plan (when (power-runtime/client-level-effects-active?)
               (power-runtime/client-build-level-effect-plan
                 camera-pos
                 (merge (hand-center-pos player) (client-world-fns player))
                 tick
                 (make-nearby-block-query-fn player)))]
    (when owner
      (apply-local-walk-speed-from-plan! owner player plan))
    {:plan plan
     :camera-pos camera-pos}))

(defn render-level-plan!
  "Submit an extracted level-effect plan to 26.2's collector.
   RenderType owns blend/depth/texture/pipeline state for every callback."
  [{:keys [plan camera-pos
           ^PoseStack pose-stack
           ^SubmitNodeCollector submit-node-collector]}]
  (when (seq (:ops plan))
    (let [{:keys [lines quads plasma]} (sort-ops (:ops plan))]
      (.pushPose pose-stack)
      (try
        (.translate pose-stack
                    (double (- (:x camera-pos)))
                    (double (- (:y camera-pos)))
                    (double (- (:z camera-pos))))
        (when (seq lines)
          (submit-custom-geometry!
            submit-node-collector pose-stack (RenderTypes/linesTranslucent)
            (fn [pose consumer]
              (doseq [op lines]
                (emit-line! consumer pose op)))))
        (doseq [[texture texture-ops] quads]
          (when-let [^Identifier identifier
                     (try
                       (Identifier/tryParse (str texture))
                       (catch Exception _ nil))]
            ;; NOTE: ops carrying :no-depth-write? (upstream's SubArcHandler
            ;; glDepthMask(false) batches) fall in with the ordinary depth ops
            ;; here. 1.20.1/1.21.1 route them to vanilla entityNoOutline —
            ;; entityTranslucent with COLOR_WRITE — but 26.2 removed that type,
            ;; and its replacement needs a custom RenderPipeline with a texture
            ;; bind group (see PlasmaRenderTypes). Until that exists, 26.2 keeps
            ;; writing depth for those quads: overlapping arcs occlude instead
            ;; of blending, which is the pre-existing behaviour, not a new one.
            (let [depth-ops (remove #(or (:no-depth-test? %) (:no-fog? %)) texture-ops)
                  ;; Upstream MarkRender disables depth test + cull for the
                  ;; tp_mark humanoid so it stays visible through walls.
                  no-depth-ops (filter #(and (:no-depth-test? %)
                                             (not (:no-fog? %)))
                                       texture-ops)
                  ;; MineDetect ore highlights (upstream HandlerRender
                  ;; disables GL_FOG for the mineview pass so the boxes stay
                  ;; visible through the skill's own blindness fog). The
                  ;; proper 26.2 fix is a custom fog-free RenderPipeline with
                  ;; a texture bind group (mirroring PlasmaRenderTypes) —
                  ;; blocked by the missing textured-pipeline infra the
                  ;; no-depth-write NOTE above documents. Until that exists,
                  ;; these ride the vanilla see-through path as tp_mark;
                  ;; whether textSeeThrough is fog-free in 26.2 needs
                  ;; verification against the 26.2 sources when the target
                  ;; builds.
                  no-fog-ops (filter :no-fog? texture-ops)]
              (when (seq depth-ops)
                (submit-custom-geometry!
                  submit-node-collector pose-stack
                  (RenderTypes/entityTranslucent identifier)
                  (fn [pose consumer]
                    (doseq [op depth-ops]
                      (emit-quad! consumer pose op)))))
              (when (seq no-depth-ops)
                ;; Vanilla see-through text type: texture x vertex colour
                ;; (no lightmap), depth test disabled — the tp_mark humanoid
                ;; stays visible through walls (upstream MarkRender disables
                ;; GL_DEPTH_TEST). NB: keeps vanilla culling.
                (submit-custom-geometry!
                  submit-node-collector pose-stack
                  (RenderTypes/textSeeThrough identifier)
                  (fn [pose consumer]
                    (doseq [op no-depth-ops]
                      (emit-quad! consumer pose op)))))
              (when (seq no-fog-ops)
                (submit-custom-geometry!
                  submit-node-collector pose-stack
                  (RenderTypes/textSeeThrough identifier)
                  (fn [pose consumer]
                    (doseq [op no-fog-ops]
                      (emit-quad! consumer pose op))))))))
        (when (seq plasma)
          (submit-custom-geometry!
            submit-node-collector pose-stack (PlasmaRenderTypes/plasmaBody)
            (fn [pose consumer]
              (doseq [op plasma]
                (emit-plasma-op! consumer pose camera-pos op)))))
        (finally
          (.popPose pose-stack)))))
  plan)
