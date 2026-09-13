package cn.li.mc1201.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class ScriptedRayCompositeRenderer<T extends Entity> extends EntityRenderer<T> {
    public ScriptedRayCompositeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        ScriptedRaySpec spec = ScriptedRenderAccess.getRaySpec(entity);
        if (spec == null) {
            return;
        }

        float length = (float) Math.max(0.1D, spec.getLength());
        float age = ScriptedRenderAccess.getAgeTicks(entity) + partialTick;
        float life = Math.max(1.0F, spec.getLifeTicks());
        float blendInTicks = (float) Math.max(1.0D, spec.getBlendInMs() / 50.0D);
        float blendOutTicks = (float) Math.max(1.0D, spec.getBlendOutMs() / 50.0D);

        float alphaIn = Math.min(1.0F, age / blendInTicks);
        float alphaOut = Math.min(1.0F, Math.max(0.0F, (life - age) / blendOutTicks));
        float alpha = Math.max(0.0F, Math.min(alphaIn, alphaOut));
        if (alpha <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        int a = (int) (220 * alpha);

        int startColor = spec.getStartColor();
        int endColor = spec.getEndColor();
        float inner = (float) spec.getInnerWidth();
        float outer = (float) spec.getOuterWidth();
        float glow = (float) spec.getGlowWidth();

        emitLine(lines, mat, 0.0F, length, startColor, endColor, a);
        emitLine(lines, mat, inner, length, startColor, endColor, a);
        emitLine(lines, mat, -inner, length, startColor, endColor, a);
        emitLine(lines, mat, outer, length, brighten(startColor), brighten(endColor), (int) (a * 0.85F));
        emitLine(lines, mat, -outer, length, brighten(startColor), brighten(endColor), (int) (a * 0.85F));
        emitLine(lines, mat, glow, length, brighten(startColor), brighten(endColor), (int) (a * 0.55F));
        emitLine(lines, mat, -glow, length, brighten(startColor), brighten(endColor), (int) (a * 0.55F));

        poseStack.popPose();
    }

    private static void emitLine(VertexConsumer lines, Matrix4f mat, float sideOffset, float length,
                                 int startColor, int endColor, int alpha) {
        lines.vertex(mat, sideOffset, 0.0F, 0.0F)
                .color((startColor >> 16) & 255, (startColor >> 8) & 255, startColor & 255, alpha)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        lines.vertex(mat, sideOffset, 0.0F, -length)
                .color((endColor >> 16) & 255, (endColor >> 8) & 255, endColor & 255, alpha)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 255) + 30);
        int g = Math.min(255, ((color >> 8) & 255) + 30);
        int b = Math.min(255, (color & 255) + 30);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
