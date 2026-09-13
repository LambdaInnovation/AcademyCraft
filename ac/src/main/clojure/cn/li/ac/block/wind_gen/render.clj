(ns cn.li.ac.block.wind-gen.render
  "CLIENT-ONLY: Wind Generator block entity renderers."
  (:require [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.mcmod.client.resources :as res]
            [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.tesr-api :as tesr-api]
            [cn.li.mcmod.client.render.multiblock-helper :as mb-helper]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.util.render :as render]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.be :as platform-be])
  (:import [java.util HashMap]))

(def ^:private obj-bake-opts
  {:skip-flat-bottom-plane? true :bottom-plane-epsilon 0.0008})

(def ^:private wind-resources-holder nil)
(def ^:private wind-resources
  (machine-render-runtime/lazy-resources #'wind-resources-holder
    {:base-model #(obj/bake-obj-model (res/load-obj-model "windgen_base") obj-bake-opts)
     :main-model #(obj/bake-obj-model (res/load-obj-model "windgen_main") obj-bake-opts)
     :fan-model #(obj/bake-obj-model (res/load-obj-model "windgen_fan") obj-bake-opts)
     :pillar-model #(obj/bake-obj-model (res/load-obj-model "windgen_pillar") obj-bake-opts)
     :base-tex-normal #(res/texture-location "models/windgen_base")
     :base-tex-disabled #(res/texture-location "models/windgen_base_disabled")
     :main-tex #(res/texture-location "models/windgen_main")
     :fan-tex #(res/texture-location "models/windgen_fan")
     :pillar-tex #(res/texture-location "models/windgen_pillar")}))

(def ^:private fan-rot-cache-key :fan-rot-cache)

(defn- fan-rot-cache ^HashMap []
  (machine-render-runtime/render-cache fan-rot-cache-key (HashMap.)))

(defn fan-rot-cache-snapshot
  []
  (into {} (fan-rot-cache)))

(defn clear-fan-rot-cache!
  []
  (machine-render-runtime/clear-render-cache! fan-rot-cache-key (HashMap.)))

(defn reset-fan-rot-cache-for-test!
  ([]
   (clear-fan-rot-cache!))
  ([cache]
   (machine-render-runtime/reset-render-cache-for-test! fan-rot-cache-key (HashMap.) (HashMap. ^java.util.Map cache))))

(defn- tile-key [tile]
  (let [p (pos/block-pos tile)]
    [(pos/pos-x p) (pos/pos-y p) (pos/pos-z p)]))

(defn- update-fan-rotation!
  [tile speed-deg-per-sec]
  (let [k (tile-key tile)
        now (render/get-render-time)
        ^HashMap cache (fan-rot-cache)
        ^doubles prev (or (.get cache k) (double-array [now 0.0]))
        dt (max 0.0 (- now (aget prev 0)))
        next-rot (+ (aget prev 1) (* speed-deg-per-sec dt))]
    (aset-double prev 0 now)
    (aset-double prev 1 next-rot)
    (.put cache k prev)
    next-rot))

(defn- render-base-at-origin
  [tile _partial-ticks pose-stack buffer-source packed-light packed-overlay]
  (let [state (or (platform-be/get-custom-state tile) {})
        complete? (= "COMPLETE" (str (get state :status "BASE_ONLY")))
        {:keys [base-model base-tex-normal base-tex-disabled]} (wind-resources)
        tex (if complete? base-tex-normal base-tex-disabled)
        vc (rb/get-solid-buffer buffer-source tex)]
    (obj/render-baked-all! base-model pose-stack vc packed-light packed-overlay)))

(defn- render-main-at-origin
  [tile _partial-ticks pose-stack buffer-source packed-light packed-overlay]
  (let [state (or (platform-be/get-custom-state tile) {})
        fan-installed? (boolean (get state :fan-installed false))
        no-obstacle? (boolean (get state :no-obstacle false))
        complete? (boolean (get state :complete false))
        {:keys [main-model main-tex fan-model fan-tex]} (wind-resources)]
    (let [vc-main (rb/get-solid-buffer buffer-source main-tex)]
      (obj/render-baked-all! main-model pose-stack vc-main packed-light packed-overlay))

    (when (and fan-installed? no-obstacle?)
      (pose/push-pose pose-stack)
      (try
        (pose/translate pose-stack 0.0 0.5 0.82)
        (pose/apply-z-rotation pose-stack (- (double (update-fan-rotation! tile (if complete? 60.0 0.0)))))
        (let [vc-fan (rb/get-solid-buffer buffer-source fan-tex)]
          (obj/render-baked-all! fan-model pose-stack vc-fan packed-light packed-overlay))
        (finally
          (pose/pop-pose pose-stack))))))

(defn- render-pillar-at-origin
  [_tile _partial-ticks pose-stack buffer-source packed-light packed-overlay]
  (let [{:keys [pillar-model pillar-tex]} (wind-resources)
        vc (rb/get-solid-buffer buffer-source pillar-tex)]
    (pose/push-pose pose-stack)
    (try
      ;; Single-block TESR origin is block corner; move OBJ to block center.
      (pose/translate pose-stack 0.5 0.0 0.5)
      (obj/render-baked-all! pillar-model pose-stack vc packed-light packed-overlay)
      (finally
        (pose/pop-pose pose-stack)))))

(defn register!
  []
  (let [base-renderer
        {:render-tile (fn [tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay]
                        (mb-helper/render-multiblock-tesr
                          tile-entity
                          render-base-at-origin
                          partial-ticks pose-stack buffer-source packed-light packed-overlay))}

        main-renderer
        {:render-tile (fn [tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay]
                        (mb-helper/render-multiblock-tesr
                          tile-entity
                          render-main-at-origin
                          partial-ticks pose-stack buffer-source packed-light packed-overlay))}

        pillar-renderer
        {:render-tile (fn [tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay]
                        (render-pillar-at-origin
                          tile-entity
                          partial-ticks pose-stack buffer-source packed-light packed-overlay))}]

    (tesr-api/register-scripted-tile-renderer! "wind-gen-base" base-renderer)
    (tesr-api/register-scripted-tile-renderer! "wind-gen-base-part" base-renderer)
    (tesr-api/register-scripted-tile-renderer! "wind-gen-main" main-renderer)
    (tesr-api/register-scripted-tile-renderer! "wind-gen-main-part" main-renderer)
    (tesr-api/register-scripted-tile-renderer! "wind-gen-pillar" pillar-renderer)))

(defn init!
  []
  (machine-render-runtime/register-client-renderer-init! 'cn.li.ac.block.wind-gen.render/register!))
