package cn.li.mc262.client.render.effect;

import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcver.ResourceLocations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public final class ScriptedBlockBodyRenderer<T extends Entity>
        extends AbstractScriptedGeometryRenderer<T> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public ScriptedBlockBodyRenderer(EntityRendererProvider.Context context) {
        super(context, "block-body");
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public void extractRenderState(T entity, ScriptedEntityRenderState<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockState blockState = resolveBlockState(ScriptedRenderAccess.getSyncedBlockId(entity));
        state.blockModel.clear();
        if (blockState != null) {
            blockModelResolver.update(state.blockModel, blockState, DISPLAY_CONTEXT);
        }
    }

    @Override
    public void submit(ScriptedEntityRenderState<T> state, PoseStack stack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        ScriptedBlockBodySpec spec = ScriptedEntitySpecAccess.getScriptedBlockBodySpec(state.entityType);
        boolean magManip = spec != null && "magmanip-block".equals(spec.getHookId());
        stack.pushPose();
        if (magManip) {
            float time = state.ageInTicks;
            stack.translate(0.0F, 0.5F, 0.0F);
            stack.mulPose(Axis.YP.rotationDegrees(time * (1.0F + Math.floorMod(state.entityId * 37, 200) / 100.0F)));
            stack.mulPose(Axis.XP.rotationDegrees(time * (1.0F + Math.floorMod(state.entityId * 61, 200) / 100.0F)));
            stack.translate(-0.5F, -0.5F, -0.5F);
        } else {
            stack.translate(-0.5F, 0.0F, -0.5F);
        }
        if (!state.blockModel.isEmpty()) {
            state.blockModel.submit(stack, collector, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        stack.popPose();
        if (magManip) {
            submitSurroundArcs(state, stack, collector);
        }
        super.submit(state, stack, collector, camera);
    }

    private static void submitSurroundArcs(ScriptedEntityRenderState<?> state, PoseStack stack,
                                           SubmitNodeCollector collector) {
        collector.submitCustomGeometry(stack, RenderTypes.LINES_TRANSLUCENT, (pose, vc) -> {
            Matrix4f matrix = pose.pose();
            for (int arc = 0; arc < 3; arc++) {
                float px = 0, py = 0, pz = 0;
                for (int segment = 0; segment <= 10; segment++) {
                    double angle = Math.PI * 2.0D * segment / 10.0D
                            + state.ageInTicks * (0.045D + arc * 0.012D) + arc * 2.094D;
                    float radius = 0.68F + 0.04F * (float) Math.sin(
                            state.entityId * 0.731F + segment * 2.37F + state.ageInTicks * 0.31F);
                    float x = arc == 1 ? (float) Math.sin(angle * 2.0D) * 0.12F : (float) Math.cos(angle) * radius;
                    float y = arc == 2 ? 0.5F + (float) Math.sin(angle * 2.0D) * 0.12F
                            : 0.5F + (float) Math.sin(angle) * radius;
                    float z = arc == 0 ? (float) Math.sin(angle * 2.0D) * 0.12F : (float) Math.sin(angle) * radius;
                    if (segment > 0) line(vc, matrix, px, py, pz, x, y, z, 145, 225, 255, 220);
                    px = x; py = y; pz = z;
                }
            }
        });
    }

    private static BlockState resolveBlockState(String blockId) {
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocations.parse(blockId));
            return block == null ? null : block.defaultBlockState();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
