package cn.li.mc1211.client.render.effect;

import cn.li.mc1211.entity.ScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renderer for the tiered-zigzag arc effect kind.
 *
 * Called directly from {@link ScriptedEffectBillboardRenderer} for the
 * "tiered-zigzag" renderer key. This class lives in mc-1.20.1 as an
 * internal rendering implementation; content registers the kind→key mapping
 * through the mcmod script-abi layer.
 */
public final class TieredZigzagArcRenderer {

    private TieredZigzagArcRenderer() {}

    public static <T extends Entity> void render(
            T entity, ScriptedEffectSpec spec, String rendererId,
            float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!(entity instanceof ScriptedEffectEntity effectEntity)) {
            return;
        }
        List<ScriptedEffectEntity.ArcData> arcs = effectEntity.getActiveArcs();
        if (arcs.isEmpty()) {
            return;
        }

        float life = Math.max(1.0F, spec == null ? 15.0F : spec.getLifeTicks());
        float age = ScriptedRenderAccess.getAgeTicks(entity) + partialTick;
        float globalAlpha = Math.max(0.0F, 1.0F - (age / life));
        if (globalAlpha <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());

        for (ScriptedEffectEntity.ArcData arc : arcs) {
            float arcLifeRatio = arc.lifeTicks <= 0 ? 0.0F
                : Mth.clamp((float) arc.lifeTicks / 3.0F, 0.0F, 1.0F);
            float alpha = globalAlpha * arcLifeRatio;
            if (alpha <= 0.01F) {
                continue;
            }
            int a = (int) (255.0F * alpha);

            // Render main strand as zigzag lines
            float[][] main = arc.strands[0];
            for (int i = 1; i < main.length; i++) {
                float[] p0 = main[i - 1];
                float[] p1 = main[i];
                // Gradient: brighter at start, dimmer toward end
                float gradient = 1.0F - ((float) i / (float) main.length);
                int r = (int) (110 + 90 * gradient);
                int g = (int) (190 + 50 * gradient);
                int b = 255;
                vc.addVertex(mat, p0[0], p0[1], p0[2]).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
                vc.addVertex(mat, p1[0], p1[1], p1[2]).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
            }

            // Render branch strands as dimmer lines
            for (int bIdx = 1; bIdx < arc.strands.length; bIdx++) {
                float[][] branch = arc.strands[bIdx];
                float branchAlpha = alpha * 0.6F;
                int ba = (int) (255.0F * branchAlpha);
                if (ba <= 0 || branch.length < 2) {
                    continue;
                }
                for (int i = 1; i < branch.length; i++) {
                    float[] p0 = branch[i - 1];
                    float[] p1 = branch[i];
                    float gradient = 1.0F - ((float) i / (float) branch.length);
                    int r = (int) (80 + 50 * gradient);
                    int g = (int) (140 + 40 * gradient);
                    int b2 = (int) (220 + 35 * gradient);
                    vc.addVertex(mat, p0[0], p0[1], p0[2]).setColor(r, g, b2, ba).setNormal(0.0F, 1.0F, 0.0F);
                    vc.addVertex(mat, p1[0], p1[1], p1[2]).setColor(r, g, b2, ba).setNormal(0.0F, 1.0F, 0.0F);
                }
            }
        }

        poseStack.popPose();
    }
}
