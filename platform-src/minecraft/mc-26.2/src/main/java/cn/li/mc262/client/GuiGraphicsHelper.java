package cn.li.mc262.client;

import cn.li.mc262.client.render.GuiPerspectiveWarp;
import cn.li.mc262.client.render.PerspectiveQuadRenderState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Thin GuiGraphicsExtractor helpers for 26.2. */
public final class GuiGraphicsHelper {
    private GuiGraphicsHelper() {}

    /**
     * Route a textured quad through the active {@link GuiPerspectiveWarp}.
     *
     * <p>Every textured entry point below tries this first. It returns false
     * whenever no warp is installed — which is every UI except the terminal, and
     * the terminal only while its camera is up — so the ordinary affine paths are
     * reached completely unchanged.</p>
     */
    private static boolean submitWarped(GuiGraphicsExtractor gge, RenderPipeline pipeline,
                                        TextureSetup textures,
                                        float x0, float y0, float x1, float y1,
                                        float u0, float u1, float v0, float v1,
                                        int argb) {
        // 26.2 exposes GUI extraction through GuiRenderState rather than a
        // public submit method on GuiGraphicsExtractor. Keep this seam as a
        // no-op until the custom perspective state is ported to addGuiElement.
        return false;
    }

    /** Solid/gradient counterpart of {@link #submitWarped}. */
    private static boolean submitWarpedFill(GuiGraphicsExtractor gge, RenderPipeline pipeline,
                                            int x0, int y0, int x1, int y1,
                                            int argbTop, int argbBottom) {
        return false;
    }

    /** Solid rectangle, warped when a perspective camera is installed. */
    public static void fill(Object graphics, int x0, int y0, int x1, int y1, int argb) {
        fillGradient(graphics, x0, y0, x1, y1, argb, argb);
    }

    /** Top-to-bottom gradient rectangle, warped when a camera is installed. */
    public static void fillGradient(Object graphics, int x0, int y0, int x1, int y1,
                                    int argbTop, int argbBottom) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return;
        }
        if (submitWarpedFill(gge, RenderPipelines.GUI, x0, y0, x1, y1, argbTop, argbBottom)) {
            return;
        }
        if (argbTop == argbBottom) {
            gge.fill(x0, y0, x1, y1, argbTop);
        } else {
            gge.fillGradient(x0, y0, x1, y1, argbTop, argbBottom);
        }
    }

    /**
     * Replace the current pose with the warp's tangent plane at local
     * {@code (x, y)}, so content vanilla will only draw through an affine pose
     * still lands on the warped surface.
     *
     * <p>Callers must have pushed the pose themselves and must pop it. Returns
     * false when no warp is installed, leaving the pose untouched.</p>
     */
    public static boolean anchorWarp(Object graphics, float x, float y) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return false;
        }
        Matrix3x2f anchored = GuiPerspectiveWarp.localAnchor(gge.pose(), x, y);
        if (anchored == null) {
            return false;
        }
        gge.pose().set(anchored);
        return true;
    }

    /** Warp helper for the common "whole texture, default sampler" case. */
    private static boolean submitWarpedTexture(GuiGraphicsExtractor gge, RenderPipeline pipeline,
                                               Identifier texture,
                                               float x0, float y0, float x1, float y1,
                                               float u0, float u1, float v0, float v1,
                                               int argb) {
        if (GuiPerspectiveWarp.active() == null || texture == null) {
            return false;
        }
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        return submitWarped(gge, pipeline,
                TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler()),
                x0, y0, x1, y1, u0, u1, v0, v1, argb);
    }

    public static void blit9(Object graphics, Identifier texture,
                             int x, int y, int w, int h,
                             int u, int v, int regionW, int regionH,
                             int texW, int texH,
                             int borderL, int borderT, int borderR, int borderB,
                             int color) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return;
        }
        // The caller's region/tex sizes were the destination size (100×177)
        // rather than the texture's (48×48), so every slice UV fraction was
        // wrong: the border slices sampled only the outer ~2px of the 48px
        // texture (the soft alpha gradient stretched blocky by NEAREST — the
        // "mosaic" panel) and the center sampled an off-center band. The
        // GpuTexture's real dimensions are authoritative for both the source
        // region and the UV divisor.
        int realTexW = texW;
        int realTexH = texH;
        AbstractTexture abstractTex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        GpuTexture gpuTex = abstractTex != null ? abstractTex.getTexture() : null;
        if (gpuTex != null && gpuTex.getWidth(0) > 0 && gpuTex.getHeight(0) > 0) {
            realTexW = gpuTex.getWidth(0);
            realTexH = gpuTex.getHeight(0);
        }
        int left = Math.min(Math.max(0, borderL), Math.min(w, realTexW));
        int top = Math.min(Math.max(0, borderT), Math.min(h, realTexH));
        int right = Math.min(Math.max(0, borderR), Math.min(w - left, realTexW - left));
        int bottom = Math.min(Math.max(0, borderB), Math.min(h - top, realTexH - top));
        int centerW = Math.max(0, w - left - right);
        int centerH = Math.max(0, h - top - bottom);
        int sourceCenterW = Math.max(0, realTexW - left - right);
        int sourceCenterH = Math.max(0, realTexH - top - bottom);

        blitSlice(gge, texture, x, y, left, top, u, v, left, top, realTexW, realTexH, color);
        blitSlice(gge, texture, x + left, y, centerW, top,
                u + left, v, sourceCenterW, top, realTexW, realTexH, color);
        blitSlice(gge, texture, x + left + centerW, y, right, top,
                u + realTexW - right, v, right, top, realTexW, realTexH, color);
        blitSlice(gge, texture, x, y + top, left, centerH,
                u, v + top, left, sourceCenterH, realTexW, realTexH, color);
        blitSlice(gge, texture, x + left, y + top, centerW, centerH,
                u + left, v + top, sourceCenterW, sourceCenterH, realTexW, realTexH, color);
        blitSlice(gge, texture, x + left + centerW, y + top, right, centerH,
                u + realTexW - right, v + top, right, sourceCenterH, realTexW, realTexH, color);
        blitSlice(gge, texture, x, y + top + centerH, left, bottom,
                u, v + realTexH - bottom, left, bottom, realTexW, realTexH, color);
        blitSlice(gge, texture, x + left, y + top + centerH, centerW, bottom,
                u + left, v + realTexH - bottom, sourceCenterW, bottom, realTexW, realTexH, color);
        blitSlice(gge, texture, x + left + centerW, y + top + centerH, right, bottom,
                u + realTexW - right, v + realTexH - bottom, right, bottom, realTexW, realTexH, color);
    }

    private static void blitSlice(GuiGraphicsExtractor graphics, Identifier texture,
                                  int x, int y, int width, int height,
                                  int u, int v, int sourceWidth, int sourceHeight,
                                  int textureWidth, int textureHeight, int color) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        if (submitWarpedTexture(graphics, RenderPipelines.GUI_TEXTURED, texture,
                x, y, x + width, y + height,
                (float) u / textureWidth, (float) (u + sourceWidth) / textureWidth,
                (float) v / textureHeight, (float) (v + sourceHeight) / textureHeight,
                color)) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y, (float) u, (float) v,
                width, height, sourceWidth, sourceHeight, textureWidth, textureHeight, color);
    }

    public static void blitTexturedQuad(Object graphics, Identifier texture,
                                        float x1, float y1, float x2, float y2, float z,
                                        float u0, float u1, float v0, float v1) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return;
        }
        int x = Math.round(Math.min(x1, x2));
        int y = Math.round(Math.min(y1, y2));
        int w = Math.max(1, Math.round(Math.abs(x2 - x1)));
        int h = Math.max(1, Math.round(Math.abs(y2 - y1)));
        if (submitWarpedTexture(gge, RenderPipelines.GUI_TEXTURED, texture,
                x, y, x + w, y + h, u0, u1, v0, v1, -1)) {
            return;
        }
        // blit's signature is (x0, y0, x1, y1, u0, u1, v0, v1) — the END
        // coordinates and the UVs in order. Passing (w, h) here put the width
        // and height in the x1/y1 slots (an inverted rect) and shuffled the UVs
        // into a degenerate span (u1 got v0), which sampled the texture's
        // bottom-left texel — transparent for line.png — so the quad was
        // discarded (the nine-slice decorative lines and any cropped sprite
        // region rendered nothing).
        gge.blit(texture, x, y, x + w, y + h, u0, u1, v0, v1);
    }

    /** Compatibility overload matching older float-free call sites. */
    public static void blitTexturedQuad(Object graphics, Identifier texture,
                                        int x0, int y0, int x1, int y1,
                                        float u0, float v0, float u1, float v1) {
        blitTexturedQuad(graphics, texture,
                (float) x0, (float) y0, (float) x1, (float) y1, 0f,
                u0, u1, v0, v1);
    }

    /** Full-texture blit at pixel size (w×h). */
    public static void blit(Object graphics, Identifier texture, int x, int y, int w, int h) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return;
        }
        if (submitWarpedTexture(gge, RenderPipelines.GUI_TEXTURED, texture,
                x, y, x + w, y + h, 0f, 1f, 0f, 1f, -1)) {
            return;
        }
        gge.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, w, h);
    }

    /**
     * Full-texture blit using vanilla's {@code SRC_ALPHA, ONE} pipeline.
     *
     * <p>This is extraction-safe: blend state belongs to the submitted render
     * state instead of being mutated globally while the GUI tape is recorded.
     */
    public static void blitAdditive(Object graphics, Identifier texture,
                                    int x, int y, int w, int h, int argb) {
        if (!(graphics instanceof GuiGraphicsExtractor gge) || texture == null || w <= 0 || h <= 0) {
            return;
        }
        if (submitWarpedTexture(gge, RenderPipelines.MOJANG_LOGO, texture,
                x, y, x + w, y + h, 0f, 1f, 0f, 1f, argb)) {
            return;
        }
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        gge.blit(tex.getTextureView(), tex.getSampler(),
                x, y, w, h, 0f, 0f, 1f, 1f);
    }

    /**
     * Textured quad with fractional UVs and a per-quad ARGB tint.
     *
     * <p>Every public {@code GuiGraphicsExtractor.blit} overload that accepts
     * fractional UVs pins the tint to -1, and the ones that accept a tint take
     * texel UVs against a texture size the caller does not know. Submitting the
     * BlitRenderState directly is the only way to get both, which the progress
     * bar needs for its multi-stop colour ramp.
     *
     * <p>The caller's pose is captured as-is, so a sheared pose yields a
     * parallelogram; the current scissor still clips it.
     */
    public static void blitTintedQuad(Object graphics, Identifier texture,
                                      int x0, int y0, int x1, int y1,
                                      float u0, float u1, float v0, float v1,
                                      int argb) {
        if (!(graphics instanceof GuiGraphicsExtractor gge) || texture == null) {
            return;
        }
        if (x0 >= x1 || y0 >= y1) {
            return;
        }
        if (submitWarpedTexture(gge, RenderPipelines.GUI_TEXTURED, texture,
                x0, y0, x1, y1, u0, u1, v0, v1, argb)) {
            return;
        }
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        gge.blit(tex.getTextureView(), tex.getSampler(),
                x0, y0, x1 - x0, y1 - y0, u0, v0, u1, v1);
    }

    @FunctionalInterface
    public interface TwoTextureBlitFunction {
        boolean submit(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup textures,
                       int x0, int y0, int x1, int y1,
                       float u0, float u1, float v0, float v1, int argb);
    }

    @FunctionalInterface
    public interface GuiElementSubmitFunction {
        boolean submit(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup textures,
                       Matrix3x2f pose, int x0, int y0, int x1, int y1,
                       float u0, float u1, float v0, float v1, int color);
    }

    private static volatile GuiElementSubmitFunction guiElementSubmitter;

    /**
     * Install the loader's submission path for custom GUI elements (NeoForge's
     * {@code GuiGraphicsExtractor.submitGuiElementRenderState}). Used by
     * {@link #blitRotated} for diagonal connection lines, which the
     * axis-aligned extractor blits cannot express. The loader builds the
     * BlitRenderState so it can attach the current scissor — a null scissor
     * would sort the element first (SCISSOR_COMPARATOR nullsFirst) and let
     * later-drawn backgrounds cover it.
     */
    public static void installGuiElementSubmitter(GuiElementSubmitFunction function) {
        guiElementSubmitter = function;
    }

    /**
     * Submit a rotated textured quad — the diagonal connection lines, matching
     * 1.20.1/1.21.1's p1→p2 axis quad with ±normal offsets sampling the
     * {@code tex-line} gradient texture (the extractor's blits are
     * axis-aligned, so a unit rectangle is transformed by a rotate/scale pose
     * instead). Returns false when the loader has no submitter, letting the
     * caller fall back to axis-aligned fills.
     */
    public static boolean blitRotated(Object graphics, Identifier texture, int argb,
                                      double x1, double y1, double x2, double y2,
                                      double thickness) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return false;
        }
        GuiElementSubmitFunction submitter = guiElementSubmitter;
        if (submitter == null) {
            return false;
        }
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) {
            return false;
        }
        double angle = Math.atan2(dy, dx);
        // Unit rectangle → [0,len]×[0,thick] anchored at p1 and rotated onto
        // p1→p2: u=0 at p1, u=1 at p2, v centered across the thickness — the
        // same mapping 1.20.1/1.21.1 use for the tex-line gradient quad. (The
        // earlier translate(-0.5F, -0.5F) centered the quad on p1, so it
        // spanned ±len/2 around the parent: half the line stuck out behind
        // the node and the child end was never reached, and with the node
        // icons drawn on top the skill-tree connections read as missing
        // entirely.) Then compose with the extractor's current pose
        // (draw-tape's left/top translate + ancestor pushes) so the quad
        // lands in screen space like every other element's abs coordinates.
        Matrix3x2f lineTransform = new Matrix3x2f()
                .translate((float) x1, (float) y1)
                .rotate((float) angle)
                .scale((float) len, (float) thickness)
                .translate(0.0F, -0.5F);
        Matrix3x2f pose = new Matrix3x2f(gge.pose()).mul(lineTransform);
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        // Dedicated line pipeline: created last, so its sort key puts the line
        // after every background/blit pipeline in the sorted GUI mesh (a line
        // through vanilla GUI_TEXTURED could sort before the background that
        // covers it and vanish).
        //
        // The unit rect [0,1]² must be UV-mapped 0..1 along both axes (u0..u1,
        // v0..v1) so the tex-line gradient spans the full quad — u=0 at p1,
        // u=1 at p2, v across the thickness. A collapsed span (u0==u1) samples
        // a single texel; line.png's bottom row is transparent, so the
        // fragment shader's alpha cutoff would discard the whole quad.
        //
        // Sample with a LINEAR clamp sampler rather than the texture's default:
        // AbstractTexture's default is NEAREST minification, and the 16×16
        // tex-line is minified along the thickness (v) axis, which renders the
        // diagonal as hard pixel steps instead of 1.20.1/1.21.1's smooth bar.
        return submitter.submit(gge, cn.li.mc262.client.render.GuiRenderPipelines.lineTextured(),
                TextureSetup.singleTexture(tex.getTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                pose, 0, 0, 1, 1,
                0.0F, 1.0F, 0.0F, 1.0F, argb);
    }

    private static volatile TwoTextureBlitFunction twoTextureBlitter;

    /**
     * Install the loader's submission path for two-sampler GUI draws.
     *
     * <p>The skill/cpbar shaders sample a mask texture from Sampler1, but the
     * vanilla extractor only blits single textures (Sampler0), so a draw with
     * those pipelines fails the "Missing sampler Sampler1" check at execute
     * time. The loader (NeoForge) patches {@code GuiGraphicsExtractor} with
     * {@code submitGuiElementRenderState} and submits a BlitRenderState that
     * carries the double TextureSetup.</p>
     */
    public static void installTwoTextureBlitter(TwoTextureBlitFunction function) {
        twoTextureBlitter = function;
    }

    /**
     * Submit a textured quad through a custom extraction-safe GUI pipeline.
     *
     * <p>The optional second texture is bound as Sampler1. The packed ARGB
     * colour is available to custom shaders as the vertex {@code Color}, which
     * is how 26.2 GUI nodes carry per-draw scalar parameters without mutable
     * shader uniforms.</p>
     */
    public static void blitPipeline(Object graphics, RenderPipeline pipeline,
                                    Identifier texture0, Identifier texture1,
                                    int x0, int y0, int x1, int y1,
                                    float u0, float u1, float v0, float v1,
                                    int argb) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)
                || pipeline == null || texture0 == null || x0 >= x1 || y0 >= y1) {
            return;
        }
        AbstractTexture first = Minecraft.getInstance().getTextureManager().getTexture(texture0);
        if (texture1 != null) {
            AbstractTexture second = Minecraft.getInstance().getTextureManager().getTexture(texture1);
            TextureSetup textures = TextureSetup.doubleTexture(
                    first.getTextureView(), first.getSampler(),
                    second.getTextureView(), second.getSampler());
            TwoTextureBlitFunction blitter = twoTextureBlitter;
            if (blitter != null) {
                blitter.submit(gge, pipeline, textures, x0, y0, x1, y1, u0, u1, v0, v1, argb);
            }
            return;
        }
        TextureSetup textures = TextureSetup.singleTexture(first.getTextureView(), first.getSampler());
        if (submitWarped(gge, pipeline, textures, x0, y0, x1, y1, u0, u1, v0, v1, argb)) {
            return;
        }
        gge.blit(pipeline, texture0, x0, y0, u0, v0,
                x1 - x0, y1 - y0,
                x1 - x0, y1 - y0);
    }

    /** Submit a solid rectangle with pipeline-owned depth/blend state. */
    public static void fillPipeline(Object graphics, RenderPipeline pipeline,
                                    int x0, int y0, int x1, int y1, int argb) {
        if (!(graphics instanceof GuiGraphicsExtractor gge) || pipeline == null) {
            return;
        }
        if (submitWarpedFill(gge, pipeline, x0, y0, x1, y1, argb, argb)) {
            return;
        }
        gge.fill(pipeline, x0, y0, x1, y1, argb);
    }

    /** Sprite-sheet region blit. */
    public static void blitRegion(Object graphics, Identifier texture,
                                  int x, int y, int w, int h,
                                  float u, float v, int regionW, int regionH,
                                  int texW, int texH) {
        if (!(graphics instanceof GuiGraphicsExtractor gge)) {
            return;
        }
        if (submitWarpedTexture(gge, RenderPipelines.GUI_TEXTURED, texture,
                x, y, x + w, y + h,
                u / texW, (u + regionW) / texW,
                v / texH, (v + regionH) / texH,
                -1)) {
            return;
        }
        gge.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y, u, v, w, h, regionW, regionH, texW, texH);
    }
}
