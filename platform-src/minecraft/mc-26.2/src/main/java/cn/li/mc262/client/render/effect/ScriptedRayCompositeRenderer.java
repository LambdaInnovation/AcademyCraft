package cn.li.mc262.client.render.effect;

import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class ScriptedRayCompositeRenderer<T extends Entity>
        extends AbstractScriptedGeometryRenderer<T> {
    public ScriptedRayCompositeRenderer(EntityRendererProvider.Context context) {
        this(context, "ray-composite");
    }

    public ScriptedRayCompositeRenderer(EntityRendererProvider.Context context, String rendererId) {
        super(context, rendererId);
    }

    @Override
    public void extractRenderState(T entity, ScriptedEntityRenderState<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ScriptedRaySpec spec = ScriptedRenderAccess.getRaySpec(entity);
        state.lifeTicks = spec == null ? 15 : spec.getLifeTicks();
    }

    @Override
    public void submit(ScriptedEntityRenderState<T> state, PoseStack stack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, stack, collector, camera);
        ScriptedRaySpec spec = ScriptedRenderAccess.getRaySpec(state.entity);
        if (spec == null) return;
        float age = state.ageTicks + state.partialTick;
        float fadeIn = Math.max(1.0F, (float) spec.getBlendInMs() / 50.0F);
        float fadeOut = Math.max(1.0F, (float) spec.getBlendOutMs() / 50.0F);
        float alpha = Math.min(Mth.clamp(age / fadeIn, 0, 1),
                Mth.clamp((state.lifeTicks - age) / fadeOut, 0, 1));
        int a = (int) (220.0F * alpha);
        float length = Math.max(0.1F, (float) spec.getLength());
        float inner = (float) spec.getInnerWidth();
        float outer = (float) spec.getOuterWidth();
        float glow = (float) spec.getGlowWidth();
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        stack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(stack, RenderTypes.LINES_TRANSLUCENT, (pose, vc) -> {
            Matrix4f m = pose.pose();
            emitBeam(vc, m, 0, 0, length, spec.getStartColor(), spec.getEndColor(), a);
            emitBeam(vc, m, inner, 0, length, spec.getStartColor(), spec.getEndColor(), a);
            emitBeam(vc, m, -inner, 0, length, spec.getStartColor(), spec.getEndColor(), a);
            emitBeam(vc, m, 0, inner, length, spec.getStartColor(), spec.getEndColor(), a);
            emitBeam(vc, m, 0, -inner, length, spec.getStartColor(), spec.getEndColor(), a);
            emitBeam(vc, m, outer, outer, length, brighten(spec.getStartColor()),
                    brighten(spec.getEndColor()), (int) (a * 0.8F));
            emitBeam(vc, m, -outer, -outer, length, brighten(spec.getStartColor()),
                    brighten(spec.getEndColor()), (int) (a * 0.8F));
            emitBeam(vc, m, glow, 0, length, brighten(spec.getStartColor()),
                    brighten(spec.getEndColor()), (int) (a * 0.35F));
            emitBeam(vc, m, -glow, 0, length, brighten(spec.getStartColor()),
                    brighten(spec.getEndColor()), (int) (a * 0.35F));
        });
        stack.popPose();
    }

    private static void emitBeam(com.mojang.blaze3d.vertex.VertexConsumer vc, Matrix4f m,
                                 float x, float y, float length,
                                 int start, int end, int alpha) {
        vc.addVertex(m, x, y, 0).setColor((start >> 16) & 255, (start >> 8) & 255,
                start & 255, alpha).setNormal(0, 1, 0);
        vc.addVertex(m, x, y, -length).setColor((end >> 16) & 255, (end >> 8) & 255,
                end & 255, alpha).setNormal(0, 1, 0);
    }

    private static int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 255) + 30);
        int g = Math.min(255, ((color >> 8) & 255) + 30);
        int b = Math.min(255, (color & 255) + 30);
        return (r << 16) | (g << 8) | b;
    }
}
