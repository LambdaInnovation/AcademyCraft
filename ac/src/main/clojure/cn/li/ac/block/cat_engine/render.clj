(ns cn.li.ac.block.cat-engine.render
  "CLIENT-ONLY: Cat Engine scripted renderer.

  Renders a floating rotating quad using the cat_engine block texture.
  Behavior mirrors legacy AcademyCraft TESR animation speed driven by :this-tick-gen."
  (:require [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.client.resources :as res]
            [cn.li.mcmod.client.render.tesr-api :as tesr-api]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.util.render :as render]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos])
  (:import [java.util HashMap]))

(def ^:private cat-engine-resources-holder nil)
(def ^:private cat-engine-resources
  (machine-render-runtime/lazy-resources #'cat-engine-resources-holder
    {:texture #(res/texture-location "block/cat_engine")}))

(def ^:private rotor-cache-key :rotor-cache)

(defn- rotor-cache ^HashMap []
  (machine-render-runtime/render-cache rotor-cache-key (HashMap.)))

(defn rotor-cache-snapshot
  []
  (into {} (rotor-cache)))

(defn clear-rotor-cache!
  []
  (machine-render-runtime/clear-render-cache! rotor-cache-key (HashMap.)))

(defn reset-rotor-cache-for-test!
  ([]
   (clear-rotor-cache!))
  ([cache]
   (machine-render-runtime/reset-render-cache-for-test! rotor-cache-key (HashMap.) (HashMap. ^java.util.Map cache))))

(defn- tile-key [tile]
  (let [p (pos/block-pos tile)]
    [(pos/pos-x p) (pos/pos-y p) (pos/pos-z p)]))

(defn- next-rotation!
  [tile tick-gen]
  (let [k (tile-key tile)
        now-ms (long (* 1000.0 (render/get-render-time)))
        ^HashMap cache (rotor-cache)
        ^doubles prev (or (.get cache k) (double-array [(double now-ms) 0.0]))
        dt-ms (max 0 (- now-ms (long (aget prev 0))))
        rot (mod (+ (aget prev 1)
                    (* (double dt-ms) (double tick-gen) 1.0e-2))
                 360.0)]
    (aset-double prev 0 (double now-ms))
    (aset-double prev 1 rot)
    (.put cache k prev)
    rot))

(def ^:private quad-vertices
  [[0.0 0.0 0.0 0.0 0.0]
   [1.0 0.0 0.0 1.0 0.0]
   [1.0 1.0 0.0 1.0 1.0]
   [0.0 1.0 0.0 0.0 1.0]])

;; The cutout RenderType draws in QUADS mode: exactly 4 vertices per quad, in
;; ring order. Emitting 6 (two triangles) makes the buffer index only the first
;; 4 and stitch the leftovers onto the next block entity's vertices.
(def ^:private quad-vertex-order [0 1 2 3])

(defn- submit-quad!
  [vc pose-stack packed-light packed-overlay]
  (doseq [idx quad-vertex-order
          :let [[x y z u v] (nth quad-vertices idx)]]
    (rb/submit-vertex vc pose-stack x y z
                      1.0 1.0 1.0 1.0
                      u v
                      packed-overlay packed-light
                      0.0 0.0 1.0)))

;; Upstream's TESR received block-minus-camera offsets as its x/y/z arguments
;; and derived the yaw from them, so the quad's normal ends up pointing at the
;; camera: the rotor is a horizontal billboard that turns to keep facing the
;; player, and its spin axis is never seen end-on. Reconstructing that needs the
;; camera in world space — the pose stack cannot supply it, because on 1.20.1
;; and 1.21.1 the camera rotation is already baked into it (26.2 moved that into
;; the projection, hence a fresh identity pose there).
(def ^:private camera-pos-refresh-ms 16)
(def ^:private camera-pos-cache-key :cat-engine-camera-pos)
(def ^:private camera-pos-initial {:at-ms 0 :pos nil})

(defn clear-camera-pos-cache!
  "Drop the cached camera position; the next render re-reads it."
  []
  (machine-render-runtime/clear-render-cache! camera-pos-cache-key camera-pos-initial))

(defn- camera-pos
  "Camera position, re-read at most once per frame-length window and shared by
  every cat engine in view — the TESR runs per block per frame and
  `call-adapter` costs a Framework deref plus a map lookup."
  []
  (let [now (System/currentTimeMillis)
        {:keys [at-ms pos]} (machine-render-runtime/render-cache
                              camera-pos-cache-key camera-pos-initial)]
    (if (< (- now at-ms) camera-pos-refresh-ms)
      pos
      (let [fresh (bridge/call-adapter :camera-position)]
        (machine-render-runtime/put-render-cache!
          camera-pos-cache-key {:at-ms now :pos fresh})
        fresh))))

(defn- billboard-yaw
  "Upstream: `atan2(x, z) * 180/PI + 180`, where x and z are the block centre
  relative to the camera. Falls back to 0 when no camera is available (outside
  a level), which just leaves the quad axis-aligned for that frame."
  [tile]
  (if-let [cam (camera-pos)]
    (let [p (pos/block-pos tile)
          dx (- (+ 0.5 (double (pos/pos-x p))) (double (:x cam)))
          dz (- (+ 0.5 (double (pos/pos-z p))) (double (:z cam)))]
      (+ 180.0 (Math/toDegrees (Math/atan2 dx dz))))
    0.0))

(defn render-at-origin
  [tile pose-stack buffer-source packed-light packed-overlay]
  (let [state (or (platform-be/get-custom-state tile) {})
        tick-gen (double (get state :this-tick-gen 0.0))
        rot (next-rotation! tile tick-gen)
        t (render/get-render-time)
        bob (* 0.03 (Math/sin (* t 0.006)))
        yaw-deg (billboard-yaw tile)
        vc (rb/get-cutout-no-cull-buffer buffer-source (:texture (cat-engine-resources)))]
    (pose/push-pose pose-stack)
    (try
      (pose/translate pose-stack 0.5 (+ 0.03 bob) 0.5)
      ;; Faces the quad at the viewer, as upstream RenderCatEngine did.
      (pose/apply-y-rotation pose-stack yaw-deg)
      (pose/translate pose-stack 0.0 0.5 0.0)
      (pose/apply-x-rotation pose-stack rot)
      (pose/translate pose-stack -0.5 -0.5 0.0)
      (submit-quad! vc pose-stack packed-light packed-overlay)
      (finally
        (pose/pop-pose pose-stack)))))

(defn register!
  []
  (tesr-api/register-scripted-tile-renderer!
    "cat-engine"
    {:render-tile (fn [tile-entity _partial-ticks pose-stack buffer-source packed-light packed-overlay]
                     (try
                       (render-at-origin tile-entity pose-stack buffer-source packed-light packed-overlay)
                       (catch Exception e
                         (log/debug "Error in cat-engine renderer:" (ex-message e)))))}))

(defn init!
  []
  (machine-render-runtime/register-client-renderer-init! 'cn.li.ac.block.cat-engine.render/register!))
