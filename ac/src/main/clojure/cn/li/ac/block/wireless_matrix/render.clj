(ns cn.li.ac.block.wireless-matrix.render
  "CLIENT-ONLY: Wireless matrix block entity renderer."
  (:require [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.ac.block.wireless-matrix.logic :as matrix-logic]
            [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.multiblock-helper :as mb-helper]
            [cn.li.mcmod.client.render.obj-tesr-common :as obj-tesr]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.client.render.tesr-api :as tesr-api]
            [cn.li.mcmod.client.resources :as res]
            [cn.li.mcmod.util.render :as render]))

(def ^:private matrix-resources-holder nil)
(def ^:private matrix-resources
  (machine-render-runtime/lazy-resources #'matrix-resources-holder
    {:model #(obj/bake-obj-model (res/load-obj-model "matrix")
                                 {:skip-flat-bottom-plane? true
                                  :bottom-plane-epsilon 0.0008})
     :texture #(res/texture-location "models/matrix")}))

(def ^:private shield-cache-key :last-shield-hw-state)

(defn last-shield-hw-state-snapshot []
  (machine-render-runtime/render-cache-snapshot shield-cache-key nil))

(defn clear-last-shield-hw-state! []
  (machine-render-runtime/clear-render-cache! shield-cache-key nil))

(defn reset-last-shield-hw-state-for-test!
  ([] (clear-last-shield-hw-state!))
  ([state] (machine-render-runtime/reset-render-cache-for-test! shield-cache-key nil state)))

(defn render-base
  [_tile pose-stack vertex-consumer packed-light packed-overlay]
  (obj-tesr/render-obj-parts! (:model (matrix-resources)) ["Main" "Core"] pose-stack vertex-consumer packed-light packed-overlay))

(defn render-shields
  [tile _partial-ticks pose-stack vertex-consumer packed-light packed-overlay]
  (let [plate-count (int (matrix-logic/get-plate-count tile))
        core-level (matrix-logic/get-core-level tile)
        active-plates (if (and (= plate-count 3) (> core-level 0)) 3 0)
        time (render/get-render-time)
        dtheta (/ 360.0 (max active-plates 1))
        phase (mod (* time 50.0) 360.0)
        ht-phase-offset 40.0
        hw-state {:plate-count plate-count :core-level core-level :active-plates active-plates}]
    (when (not= hw-state (last-shield-hw-state-snapshot))
      (machine-render-runtime/put-render-cache! shield-cache-key hw-state))
    (dotimes [i active-plates]
      (pose/push-pose pose-stack)
      (try
        (let [float-height 0.1
              y-offset (* float-height (Math/sin (+ (* time 1.111) (* ht-phase-offset i))))]
          (pose/translate pose-stack (double 0.0) (double y-offset) (double 0.0))
          (pose/apply-y-rotation pose-stack (+ phase (* dtheta i)))
          (obj/render-baked-part! (:model (matrix-resources)) "Shield" pose-stack vertex-consumer packed-light packed-overlay))
        (finally
          (pose/pop-pose pose-stack))))))

(defn render-at-origin
  [tile partial-ticks pose-stack buffer-source packed-light packed-overlay]
  (obj-tesr/translate-obj-y-lift! pose-stack)
  (let [vc (obj-tesr/get-solid-vc buffer-source (:texture (matrix-resources)))]
    (render-base tile pose-stack vc packed-light packed-overlay)
    (render-shields tile partial-ticks pose-stack vc packed-light packed-overlay)))

(defn register!
  []
  (let [renderer
        {:render-tile (fn [tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay]
                      (mb-helper/render-multiblock-tesr
                       tile-entity render-at-origin partial-ticks pose-stack buffer-source packed-light packed-overlay))}]
    (tesr-api/register-scripted-tile-renderer! "wireless-matrix" renderer)
    (tesr-api/register-scripted-tile-renderer! "wireless-matrix-part" renderer)))

(defn init!
  []
  (machine-render-runtime/register-client-renderer-init! 'cn.li.ac.block.wireless-matrix.render/register!))
