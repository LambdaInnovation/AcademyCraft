(ns cn.li.mcmod.client.render.obj-tesr-common
  "Shared OBJ + solid-buffer conventions for multiblock TESR (e.g. matrix, developer).

  Bottom-plane culling / normal-mode / etc. used to be render-time `binding`
  rebinds around every draw call (every frame, per BE); they are now baked
  once into the model at load time via `obj/bake-obj-model` — see
  `default-bottom-plane-epsilon` for the shared flat-bottom epsilon most
  multiblock TESRs bake with."
  (:require [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.client.render.pose :as pose]))

(def default-obj-y-lift
  "Lift in block space; matches matrix TESR (reduces z-fight with support cells)."
  0.02)

(def default-bottom-plane-epsilon
  "OBJ bottom-plane culling tolerance; matches matrix / solar OBJ renderers.
  Pass as `:bottom-plane-epsilon` alongside `:skip-flat-bottom-plane? true`
  to `obj/bake-obj-model` when loading the model (not at render time)."
  0.0008)

(defn translate-obj-y-lift!
  [pose-stack]
  (pose/translate pose-stack (double 0.0) (double default-obj-y-lift) (double 0.0)))

(defn render-obj-parts!
  "Draw named baked-OBJ groups in order. Caller must already apply desired pose."
  [baked part-names pose-stack vertex-consumer packed-light packed-overlay]
  (doseq [part part-names]
    (when (obj/baked-has-part? baked part)
      (obj/render-baked-part! baked part pose-stack vertex-consumer packed-light packed-overlay))))

(defn get-solid-vc
  "Solid `RenderType` buffer for a texture."
  [buffer-source texture-loc]
  (rb/get-solid-buffer buffer-source texture-loc))
