package cn.li.mcver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Cross-version VertexConsumer helpers.
 * 1.20.1: classic vertex/color/uv/.../endVertex chain.
 */
public final class RenderInterop {
    private RenderInterop() {
    }

    public static void submitVertex(
            VertexConsumer vc,
            PoseStack poseStack,
            float x, float y, float z,
            float r, float g, float b, float a,
            float u, float v,
            int overlay, int light,
            float nx, float ny, float nz) {
        PoseStack.Pose entry = poseStack.last();
        vc.vertex(entry.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(entry.normal(), nx, ny, nz)
                .endVertex();
    }

    /**
     * Vertex for POSITION_COLOR_TEX_LIGHTMAP render types, which carry no
     * overlay and no normal element — writing those would misalign the buffer.
     * Used by the see-through translucent types that mirror upstream's
     * glDisable(GL_DEPTH_TEST) + glDepthMask(false) overlays.
     */
    public static void submitVertexNoOverlay(
            VertexConsumer vc,
            PoseStack poseStack,
            float x, float y, float z,
            float r, float g, float b, float a,
            float u, float v,
            int light) {
        vc.vertex(poseStack.last().pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .uv2(light)
                .endVertex();
    }

    public static void addColoredVertex(VertexConsumer vc, float x, float y, float z, float r, float g, float b, float a) {
        vc.vertex(x, y, z).color(r, g, b, a).endVertex();
    }

    public static void addColoredVertex(Object vc, double x, double y, double z, int r, int g, int b, int a) {
        if (vc instanceof VertexConsumer consumer) {
            addColoredVertex(consumer, (float) x, (float) y, (float) z,
                    r / 255f, g / 255f, b / 255f, a / 255f);
        }
    }

    public static void addVertex(VertexConsumer vc, float x, float y, float z) {
        vc.vertex(x, y, z).endVertex();
    }

    public static void addVertex(Object vc, double x, double y, double z) {
        if (vc instanceof VertexConsumer consumer) {
            addVertex(consumer, (float) x, (float) y, (float) z);
        }
    }
}
