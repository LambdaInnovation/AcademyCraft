(ns cn.li.ac.entity.mag-hook-render
  "CLIENT-ONLY: mag hook entity renderer.

  Port of RendererMagHook: maghook.obj while the hook is in flight, and
  maghook_open.obj once it bites into a block, at the original's orientation
  and 0.0054 scale. The port drew the hook with the generic thrown-item
  renderer instead — a flat item sprite that never changed on anchoring — so
  the hook never appeared to open.

  Reached through the shared BehaviorObjRenderer bridge: the platform renderer
  resolves this namespace from cn.li.mcmod.spi.entity-render-registry by the
  entity's registry name and calls `render!` with plain values, which is what
  keeps this namespace free of Minecraft types."
  (:require [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.client.resources :as res]
            [cn.li.mcmod.util.log :as log]))

;; Upstream RendererMagHook draws both models at scale 0.0054.
(def ^:private model-scale 0.0054)

(def ^:private mag-hook-resources-holder nil)
(def ^:private mag-hook-resources
  (machine-render-runtime/lazy-resources #'mag-hook-resources-holder
    {:closed #(obj/bake-obj-model (res/load-obj-model "maghook") {})
     :open #(obj/bake-obj-model (res/load-obj-model "maghook_open") {})
     :texture #(res/texture-location "models/maghook")}))

(defn render!
  "`hit?` is the entity's anchored state (upstream's EntityMagHook.isHit); the
  pose stack arrives translated to the entity, matching upstream's
  glTranslate(x, y, z)."
  [_entity-id hit? _age-ticks yaw pitch _partial-tick pose-stack buffer-source
   packed-light packed-overlay]
  (try
    (let [{:keys [closed open texture]} (mag-hook-resources)]
      (pose/push-pose pose-stack)
      (try
        ;; Original: glRotated(-yaw + 90, 0,1,0); glRotated(pitch - 90, 0,0,1).
        (pose/apply-y-rotation pose-stack (+ (- (double (or yaw 0.0))) 90.0))
        (pose/apply-z-rotation pose-stack (- (double (or pitch 0.0)) 90.0))
        (pose/scale pose-stack (float model-scale) (float model-scale) (float model-scale))
        (let [vc (rb/get-solid-buffer buffer-source texture)]
          (obj/render-baked-all! (if hit? open closed)
                                 pose-stack vc packed-light packed-overlay))
        (finally
          (pose/pop-pose pose-stack))))
    (catch Exception e
      (log/debug "Error in mag hook renderer:" (ex-message e)))))
