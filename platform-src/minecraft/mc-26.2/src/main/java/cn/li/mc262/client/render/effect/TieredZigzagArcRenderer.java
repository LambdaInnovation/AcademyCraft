package cn.li.mc262.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Submit-node renderer for the short-lived strands produced by TieredArcsEffectHook. */
final class TieredZigzagArcRenderer {
    private TieredZigzagArcRenderer() {
    }

    static void submit(ScriptedEntityRenderState<?> state,
                       PoseStack poseStack,
                       SubmitNodeCollector collector) {
        if (state.activeArcs.isEmpty()) {
            return;
        }
        float globalAlpha = Mth.clamp(1.0F - (state.ageTicks + state.partialTick)
                / Math.max(1.0F, state.lifeTicks), 0.0F, 1.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.LINES_TRANSLUCENT,
                (pose, consumer) -> draw(state, pose.pose(), consumer, globalAlpha));
    }

    private static void draw(ScriptedEntityRenderState<?> state,
                             Matrix4f matrix,
                             VertexConsumer consumer,
                             float globalAlpha) {
        for (ScriptedRenderAccess.ArcDataView arc : state.activeArcs) {
            float flicker = 0.72F + 0.28F * (float) Math.sin(
                    state.ageInTicks * 2.4F + arc.phase + arc.flickerSeed);
            float lifeAlpha = Mth.clamp(arc.lifeTicks / 3.0F, 0.0F, 1.0F);
            float alpha = globalAlpha * lifeAlpha * flicker;
            for (int strandIndex = 0; strandIndex < arc.strands.length; strandIndex++) {
                float[][] strand = arc.strands[strandIndex];
                if (strand.length < 2) {
                    continue;
                }
                float strandAlpha = strandIndex == 0 ? alpha : alpha * 0.58F;
                int a = Mth.clamp((int) (255.0F * strandAlpha), 0, 255);
                for (int i = 1; i < strand.length; i++) {
                    float t = (float) i / strand.length;
                    int r = strandIndex == 0 ? (int) (205.0F - 85.0F * t) : 95;
                    int g = strandIndex == 0 ? (int) (245.0F - 45.0F * t) : 165;
                    int b = 255;
                    float[] p0 = strand[i - 1];
                    float[] p1 = strand[i];
                    AbstractScriptedGeometryRenderer.line(consumer, matrix,
                            p0[0], p0[1], p0[2], p1[0], p1[1], p1[2],
                            r, g, b, a);
                }
            }
        }
    }
}
