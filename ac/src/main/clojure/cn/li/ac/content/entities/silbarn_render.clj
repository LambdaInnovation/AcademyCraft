(ns cn.li.ac.content.entities.silbarn-render
  "CLIENT-ONLY: entity_silbarn renderer.

  Ported from AcademyCraft's EntitySilbarn$RenderSibarn:
  - silbarn.obj scaled 0.05, texture models/silbarn
  - continuous spin around a per-entity persistent random axis, plus the
    entity's yaw and a fixed 90 degree tilt
  - hidden entirely once the entity has registered a hit (server-synced flag)"
  (:require [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.mcmod.client.resources :as res]
            [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.util.log :as log]))

(def ^:private silbarn-resources-holder nil)
(def ^:private silbarn-resources
  (machine-render-runtime/lazy-resources #'silbarn-resources-holder
    {:model #(obj/bake-obj-model (res/load-obj-model "silbarn"))
     :texture #(res/texture-location "models/silbarn")}))

(def ^:private spin-degrees-per-tick
  "Matches original: 0.03 deg/ms render-loop spin at 20 ticks/sec (50 ms/tick)."
  1.5)

(defn- spin-axis
  "Deterministic per-entity persistent rotation axis - stable across frames
  without needing extra synced entity state (matches the original's
  random-but-fixed-per-instance Vec3d axis)."
  [entity-id]
  (let [rng (java.util.Random. (long entity-id))]
    [(.nextInt rng) (.nextInt rng) (.nextInt rng)]))

(defn render!
  ;; `pitch` is passed by BehaviorObjRenderer for entities that orient along
  ;; their flight direction (the mag hook); the silbarn spins on its own axis.
  [entity-id hit? age-ticks yaw _pitch partial-tick pose-stack buffer-source packed-light packed-overlay]
  (when-not hit?
    (try
      (let [{:keys [model texture]} (silbarn-resources)
            vc (rb/get-solid-buffer buffer-source texture)
            [ax ay az] (spin-axis entity-id)
            spin-angle (* spin-degrees-per-tick (+ (double age-ticks) (double partial-tick)))]
        (pose/push-pose pose-stack)
        (try
          (pose/scale pose-stack 0.05 0.05 0.05)
          (pose/apply-axis-rotation pose-stack spin-angle ax ay az)
          (pose/apply-y-rotation pose-stack (- (double yaw)))
          (pose/apply-x-rotation pose-stack 90.0)
          (obj/render-baked-all! model pose-stack vc packed-light packed-overlay)
          (finally
            (pose/pop-pose pose-stack))))
      (catch Exception e
        (log/debug "Error in silbarn renderer:" (ex-message e))))))
