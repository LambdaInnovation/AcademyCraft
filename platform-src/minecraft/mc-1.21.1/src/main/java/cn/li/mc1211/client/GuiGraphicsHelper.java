package cn.li.mc1211.client;

import cn.li.mcver.render.ImmediateDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class GuiGraphicsHelper {
    private GuiGraphicsHelper() {
    }

    /**
     * Wrapper for GuiGraphics.blit() 9-parameter overload.
     */
    public static void blit9(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x, int y,
            int u, int v,
            int width, int height,
            int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    /**
     * Render a textured quad with custom normalized UV coordinates.
     * Mesh upload goes through {@link ImmediateDraw} (version seam).
     *
     * <p>The vertices are transformed by the GuiGraphics PoseStack matrix, so the
     * quad honors the current translate/scale (e.g. a container screen's
     * leftPos/topPos offset).</p>
     *
     * <p>{@code ImmediateDraw.draw} uses whatever shader
     * {@code RenderSystem.setShader} last selected, and MC's RenderType shards
     * leave their own shader bound after a text/fill batch flushes. Bind
     * position_tex explicitly (exactly as vanilla {@code GuiGraphics.innerBlit}
     * does) — without it a POSITION_TEX quad is fed to e.g. position_color,
     * which reads the UV floats as the colour attribute and renders a flat,
     * untextured fill instead of the texture.</p>
     */
    public static void blitTexturedQuad(
            GuiGraphics graphics,
            ResourceLocation texture,
            float x1, float y1,
            float x2, float y2,
            float z,
            float u0, float u1,
            float v0, float v1) {
        Matrix4f pose = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Note: vanilla blit uses y1/y2 order (top/bottom) differently than UV origin;
        // preserve the historical corner mapping from the previous BufferBuilder path.
        ImmediateDraw.begin(ImmediateDraw.Mode.QUADS, ImmediateDraw.Format.POSITION_TEX);
        ImmediateDraw.vertex(pose, x1, y2, z).uv(u0, v1).endVertex();
        ImmediateDraw.vertex(pose, x2, y2, z).uv(u1, v1).endVertex();
        ImmediateDraw.vertex(pose, x2, y1, z).uv(u1, v0).endVertex();
        ImmediateDraw.vertex(pose, x1, y1, z).uv(u0, v0).endVertex();
        ImmediateDraw.draw();
    }
}
