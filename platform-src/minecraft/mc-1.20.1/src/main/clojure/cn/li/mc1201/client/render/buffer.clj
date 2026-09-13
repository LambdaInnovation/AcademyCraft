(ns cn.li.mc1201.client.render.buffer
  "CLIENT-ONLY shared render-buffer helpers for Minecraft 1.20.1."
  (:import [cn.li.mc1201.client.render ModRenderTypes]
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

(defn get-translucent-see-through-target-buffer
  "Same as `get-translucent-see-through-buffer` but rendered into the
  translucent render target, so the geometry composites WITH the translucent
  terrain (fluid surfaces) instead of being covered by the target's final blit
  into the main framebuffer."
  [^MultiBufferSource buffer-source ^ResourceLocation texture]
  (.getBuffer buffer-source (ModRenderTypes/academyQuadsTranslucentTarget texture)))

(defn get-additive-buffer
  "Additive translucent QUADS (SRC_ALPHA/ONE — light adds to whatever is
  behind), depth-tested (LEQUAL) but never written, no cull. The imag-phase
  :surface-flash mode uses it so the flash reads over the opaque black pool
  surface while the pool's own surface and terrain still occlude from the
  side."
  [^MultiBufferSource buffer-source ^ResourceLocation texture]
  (.getBuffer buffer-source (ModRenderTypes/academyQuadsAdditive texture)))

(defn get-entity-buffer
  [buffer-source render-mode texture]
  (case render-mode
    :solid (get-solid-buffer buffer-source texture)
    :translucent (get-translucent-buffer buffer-source texture)
    :cutout-no-cull (get-cutout-no-cull-buffer buffer-source texture)
    (get-solid-buffer buffer-source texture)))
