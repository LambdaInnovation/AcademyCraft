package cn.li.mcver;

import cn.li.mc262.client.render.SubmitNodeRenderBufferAdapter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Cross-version VertexConsumer helpers.
 * 26.2: addVertex / set* plus deferred SubmitNode collector path.
 */
public final class RenderInterop {
    /** OverlayTexture.NO_OVERLAY — pack(u=0, v=WHITE_OVERLAY_V=10). */
    private static final int NO_OVERLAY = 10 << 16;

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
        if (vc instanceof SubmitNodeRenderBufferAdapter.DeferredVertexConsumer deferred) {
            deferred.submitVertex(
                    poseStack,
                    x, y, z,
                    r, g, b, a,
                    u, v,
                    overlay, light,
                    nx, ny, nz);
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF)
                .setNormal(pose, nx, ny, nz);
    }

    /**
     * Vertex for this version's see-through translucent render type. The vertex
     * format differs per version and this method owns the difference: 1.20.1
     * and 1.21.1 use POSITION_COLOR_TEX_LIGHTMAP (no overlay, no normal), while
     * 26.2's {@code ModRenderTypes.academyQuadsTranslucent} is built on
     * DefaultVertexFormat.ENTITY and therefore needs both — supplied here as
     * NO_OVERLAY and an upward normal, matching the flat quads the callers draw.
     */
    public static void submitVertexNoOverlay(
            VertexConsumer vc,
            PoseStack poseStack,
            float x, float y, float z,
            float r, float g, float b, float a,
            float u, float v,
            int light) {
        submitVertex(vc, poseStack, x, y, z, r, g, b, a, u, v,
                NO_OVERLAY, light, 0.0F, 1.0F, 0.0F);
    }

    public static void addColoredVertex(VertexConsumer vc, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(x, y, z)
                .setColor(r, g, b, a)
                // RenderType.lines() is POSITION_COLOR_NORMAL; writing the
                // normal is required by endLastVertex() and a no-op for
                // formats without a normal element.
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
