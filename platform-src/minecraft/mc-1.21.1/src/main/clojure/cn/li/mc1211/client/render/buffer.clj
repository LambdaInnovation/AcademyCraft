(ns cn.li.mc1211.client.render.buffer
  "CLIENT-ONLY shared render-buffer helpers for Minecraft 1.20.1."
  (:import [cn.li.mc1211.client.render ModRenderTypes]
           [net.minecraft.client.renderer MultiBufferSource RenderType]
           [net.minecraft.resources ResourceLocation]))

(defn get-solid-buffer
  [^MultiBufferSource buffer-source ^ResourceLocation texture]
  (.getBuffer buffer-source (RenderType/entityCutoutNoCull texture)))

(defn get-translucent-buffer
  [^MultiBufferSource buffer-source ^ResourceLocation texture]
  (.getBuffer buffer-source (RenderType/entityTranslucent texture)))

(defn get-cutout-no-cull-buffer
  [^MultiBufferSource buffer-source ^ResourceLocation texture]
  (.getBuffer buffer-source (RenderType/entityCutoutNoCull texture)))

(defn get-translucent-see-through-buffer
  "Translucent QUADS with no depth test, no depth write and no cull — the state
  legacy TESRs set by hand. POSITION_COLOR_TEX_LIGHTMAP format, so vertices go
  through `pose/submit-vertex-no-overlay`."
  [^MultiBufferSource buffer-source ^ResourceLocation texture]
  (.getBuffer buffer-source (ModRenderTypes/academyQuadsTranslucent texture)))

(defn get-entity-buffer
  [buffer-source render-mode texture]
  (case render-mode
    :solid (get-solid-buffer buffer-source texture)
    :translucent (get-translucent-buffer buffer-source texture)
    :cutout-no-cull (get-cutout-no-cull-buffer buffer-source texture)
    (get-solid-buffer buffer-source texture)))
