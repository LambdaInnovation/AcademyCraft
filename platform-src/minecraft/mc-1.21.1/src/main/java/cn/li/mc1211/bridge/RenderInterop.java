package cn.li.mc1211.bridge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * @deprecated Use {@link cn.li.mcver.RenderInterop}.
 */
@Deprecated
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
        cn.li.mcver.RenderInterop.submitVertex(vc, poseStack, x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
    }

    public static void addColoredVertex(VertexConsumer vc, float x, float y, float z, float r, float g, float b, float a) {
        cn.li.mcver.RenderInterop.addColoredVertex(vc, x, y, z, r, g, b, a);
    }

    public static void addVertex(VertexConsumer vc, float x, float y, float z) {
        cn.li.mcver.RenderInterop.addVertex(vc, x, y, z);
    }
}
