(ns cn.li.ac.ability.client.effects.rv3
  "V3-based vector math for the client ability effect render pipeline
  (arc/beam/quad/line ops) — mirrors cn.li.ac.util.math.vec3's map-based API
  by name, but returns/accepts cn.li.mcmod.math.V3 instances: no map/box
  allocation per op.

  `cn.li.ac.util.math.vec3` itself stays map-based on purpose: it's re-exported
  by `cn.li.ac.ability.effects.geom` to 28 mostly-non-render consumers, several
  of which read positions from network-synced/persisted player state (EDN
  pr-str/read-string can't serialize a Java object). This namespace is the
  render-pipeline-only counterpart, used from render-util/arc-fx/arc-patterns/
  surround-arc/vec-reflection-fx/the fx-templates impls, and consumed at the
  other end by mc1201.client.effects.level-renderer's emit-line!/emit-quad!."
  (:import [cn.li.mcmod.math V3]))

(defn v3
  "Construct a V3 from raw doubles."
  ^V3 [x y z]
  (V3. (double x) (double y) (double z)))

(defn map->v3
  "Convert a {:x :y :z ...} map (crossing from network/persisted state, or a
  mixed record like hand-center-pos/camera-pos) into a V3. Extra keys ignored."
  ^V3 [{:keys [x y z]}]
  (V3. (double (or x 0.0)) (double (or y 0.0)) (double (or z 0.0))))

(def ^V3 zero (v3 0.0 0.0 0.0))
(def ^V3 unit-x (v3 1.0 0.0 0.0))
(def ^V3 unit-y (v3 0.0 1.0 0.0))
(def ^V3 unit-z (v3 0.0 0.0 1.0))

(defn v+
  ^V3 [^V3 a ^V3 b]
  (V3/add a b))

(defn v-
  ^V3 [^V3 a ^V3 b]
  (V3/sub a b))

(defn v*
  ^V3 [^V3 a scale]
  (V3/scale a (double scale)))

(defn vcross
  ^V3 [^V3 a ^V3 b]
  (V3/cross a b))

(defn vdot
  ^double [^V3 a ^V3 b]
  (V3/dot a b))

(defn vlen
  ^double [^V3 a]
  (V3/length a))

(defn vnorm
  "Zero-length input returns (0,1,0), matching cn.li.ac.util.math.vec3/vnorm's
  epsilon-clamped-then-scale behavior (both derive from the same original
  map-based helper)."
  ^V3 [^V3 a]
  (V3/normalize a))

(defn vdist
  ^double [^V3 a ^V3 b]
  (vlen (v- a b)))

(defn vdist-sq
  ^double [^V3 a ^V3 b]
  (let [dx (- (.-x a) (.-x b))
        dy (- (.-y a) (.-y b))
        dz (- (.-z a) (.-z b))]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn orthonormal-basis
  "Return [right up] orthonormal vectors perpendicular to dir."
  [^V3 dir]
  (let [up-axis (if (> (Math/abs (.-y dir)) 0.95) unit-x unit-y)
        right (vnorm (vcross dir up-axis))
        up (vnorm (vcross right dir))]
    [right up]))

(defn rotate-around-axis
  "Rotate vec around axis by degrees."
  ^V3 [^V3 v ^V3 axis degrees]
  (let [axis-unit (vnorm axis)
        theta (Math/toRadians (double degrees))
        cos-t (Math/cos theta)
        sin-t (Math/sin theta)
        term1 (v* v cos-t)
        term2 (v* (vcross axis-unit v) sin-t)
        term3 (v* axis-unit (* (vdot axis-unit v) (- 1.0 cos-t)))]
    (vnorm (v+ (v+ term1 term2) term3))))
