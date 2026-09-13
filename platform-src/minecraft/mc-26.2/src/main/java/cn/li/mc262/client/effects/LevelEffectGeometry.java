package cn.li.mc262.client.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Typed vertex helpers for level-effect geometry submitted through a
 * {@code SubmitNodeCollector}. Render state is deliberately owned by the
 * caller's RenderType; this class only writes the attributes required by it.
 */
public final class LevelEffectGeometry {
    private LevelEffectGeometry() {
    }

    public static void lineVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                // 26.2's lines pipelines bind POSITION_COLOR_NORMAL_LINE_WIDTH;
                // endLastVertex() throws "Missing elements in vertex" when the
                // per-vertex line width is never written. Width 1.0 matches
                // the 1.20.1/1.21.1 lines render types (no width override).
                // No-op for formats without a LineWidth element.
                .setLineWidth(1.0F);
    }

    public static void texturedVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v,
            int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static void coloredVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha);
    }

    public static void plasmaVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v,
            int red, int green, int blue, int alpha,
            float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z)
                .setUv(u, v)
                .setColor(red, green, blue, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                // Plasma uses the otherwise-unneeded normal attribute as a
                // second neighbour offset; do not transform it by the pose.
                .setNormal(normalX, normalY, normalZ);
    }
}
