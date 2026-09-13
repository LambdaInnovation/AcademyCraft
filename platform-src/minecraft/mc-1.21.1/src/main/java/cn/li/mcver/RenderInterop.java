package cn.li.mcver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Cross-version VertexConsumer helpers.
 * 1.21.1: addVertex / set* API (endVertex removed).
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
        PoseStack.Pose pose = poseStack.last();
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF)
                .setNormal(pose, nx, ny, nz);
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
        vc.addVertex(poseStack.last(), x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF);
    }

    public static void addColoredVertex(VertexConsumer vc, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(x, y, z)
                .setColor(r, g, b, a)
                // RenderType.lines() is POSITION_COLOR_NORMAL; 1.21.1's
                // BufferBuilder.endLastVertex() throws "Missing elements in
                // vertex: Normal" when the normal was never written (1.20.1
                // silently zero-filled it). Writing it is a no-op for formats
                // without a normal element.
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    public static void addColoredVertex(Object vc, double x, double y, double z, int r, int g, int b, int a) {
        if (vc instanceof VertexConsumer consumer) {
            addColoredVertex(consumer, (float) x, (float) y, (float) z,
                    r / 255f, g / 255f, b / 255f, a / 255f);
        }
    }

    public static void addVertex(VertexConsumer vc, float x, float y, float z) {
        vc.addVertex(x, y, z);
    }

    public static void addVertex(Object vc, double x, double y, double z) {
        if (vc instanceof VertexConsumer consumer) {
            addVertex(consumer, (float) x, (float) y, (float) z);
        }
    }
}
