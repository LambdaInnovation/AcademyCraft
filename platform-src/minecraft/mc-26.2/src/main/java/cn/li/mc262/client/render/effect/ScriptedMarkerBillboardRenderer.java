package cn.li.mc262.client.render.effect;

import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class ScriptedMarkerBillboardRenderer<T extends Entity>
        extends AbstractScriptedGeometryRenderer<T> {
    public ScriptedMarkerBillboardRenderer(EntityRendererProvider.Context context) {
        this(context, "marker-billboard");
    }

    public ScriptedMarkerBillboardRenderer(EntityRendererProvider.Context context, String rendererId) {
        super(context, rendererId);
    }

    @Override
    public void extractRenderState(T entity, ScriptedEntityRenderState<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ScriptedMarkerSpec spec = ScriptedRenderAccess.getMarkerSpec(entity);
        state.lifeTicks = spec == null ? 15 : spec.getLifeTicks();
    }

    @Override
    public void submit(ScriptedEntityRenderState<T> state, PoseStack stack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, stack, collector, camera);
        ScriptedMarkerSpec spec = ScriptedRenderAccess.getMarkerSpec(state.entity);
        if (spec == null) return;
        float alpha = Math.max(0.0F, 1.0F - (state.ageTicks + state.partialTick) / state.lifeTicks);
        int a = (int) (255.0F * alpha);
        int g = spec.isAvailable() ? 255 : 55;
        int b = spec.isAvailable() ? 255 : 55;
        stack.pushPose();
        stack.mulPose(camera.orientation);
        collector.submitCustomGeometry(stack, RenderTypes.LINES_TRANSLUCENT, (pose, vc) -> {
            Matrix4f m = pose.pose();
            if ("tp-marking".equals(state.rendererId)) {
                float radius = 0.38F + 0.04F * (float) Math.sin(state.ageInTicks * 0.4F);
                for (int i = 0; i < 20; i++) {
                    double p0 = Math.PI * 2.0D * i / 20.0D;
                    double p1 = Math.PI * 2.0D * (i + 1) / 20.0D;
                    line(vc, m, (float) Math.cos(p0) * radius, (float) Math.sin(p0) * radius, 0,
                            (float) Math.cos(p1) * radius, (float) Math.sin(p1) * radius, 0,
                            255, g, b, a);
                }
                line(vc, m, -0.16F, 0, 0, 0.16F, 0, 0, 255, g, b, a);
                line(vc, m, 0, -0.16F, 0, 0, 0.16F, 0, 255, g, b, a);
            } else {
                float size = "wire-marker".equals(state.rendererId) ? 0.45F : 0.35F;
                line(vc, m, 0, size, 0, size, 0, 0, 255, g, b, a);
                line(vc, m, size, 0, 0, 0, -size, 0, 255, g, b, a);
                line(vc, m, 0, -size, 0, -size, 0, 0, 255, g, b, a);
                line(vc, m, -size, 0, 0, 0, size, 0, 255, g, b, a);
            }
        });
        stack.popPose();
    }
}
