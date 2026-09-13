package cn.li.mc1211.client.render.effect;

import cn.li.mcver.ResourceLocations;

import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public final class ScriptedBlockBodyRenderer<T extends Entity> extends EntityRenderer<T> {
    public ScriptedBlockBodyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        String blockId = ScriptedRenderAccess.getSyncedBlockId(entity);
        BlockState blockState = resolveBlockState(blockId);
        boolean magManip = isMagManip(entity);

        poseStack.pushPose();
        if (magManip) {
            float time = entity.tickCount + partialTick;
            float yawSpeed = 1.0F + Math.floorMod(entity.getId() * 37, 200) / 100.0F;
            float pitchSpeed = 1.0F + Math.floorMod(entity.getId() * 61, 200) / 100.0F;
            poseStack.translate(0.0, 0.5, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * yawSpeed));
            poseStack.mulPose(Axis.XP.rotationDegrees(time * pitchSpeed));
            poseStack.translate(-0.5, -0.5, -0.5);
        } else {
            poseStack.translate(-0.5, 0.0, -0.5);
        }

        if (blockState != null) {
            BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        } else {
            float w = Math.max(0.2F, entity.getBbWidth() * 0.5F);
            float h = Math.max(0.2F, entity.getBbHeight());
            poseStack.translate(0.5, 0.0, 0.5);
            Matrix4f mat = poseStack.last().pose();
            VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
            drawBoxLines(vc, mat, -w, 0.0F, -w, w, h, w, 210);
        }

        poseStack.popPose();

        if (magManip) {
            drawMagManipArcs(entity, partialTick, poseStack, bufferSource);
        }
    }

    private static boolean isMagManip(Entity entity) {
        var spec = ScriptedEntitySpecAccess.getScriptedBlockBodySpec(entity.getType());
        return spec != null && "magmanip-block".equals(spec.getHookId());
    }

    /**
     * Original MagManipEntityBlock owns a long-lived thin EntitySurroundArc.
     * Render three animated, deterministic electric loops around the captured
     * block while retaining the modern hand transform and sound enhancements.
     */
    private static void drawMagManipArcs(Entity entity,
                                         float partialTick,
                                         PoseStack poseStack,
                                         MultiBufferSource bufferSource) {
        float time = entity.tickCount + partialTick;
        float seed = entity.getId() * 0.731F;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        int segments = 9;

        for (int arc = 0; arc < 3; arc++) {
            float previousX = 0.0F;
            float previousY = 0.0F;
            float previousZ = 0.0F;
            for (int segment = 0; segment <= segments; segment++) {
                double angle = (Math.PI * 2.0D * segment / segments)
                        + time * (0.045D + arc * 0.012D)
                        + arc * 2.094D;
                double jitter = 0.045D * Math.sin(seed + segment * 2.37D + time * 0.31D);
                float radius = (float) (0.68D + jitter);
                float x;
                float y;
                float z;
                if (arc == 0) {
                    x = (float) (Math.cos(angle) * radius);
                    y = 0.5F + (float) (Math.sin(angle) * radius);
                    z = (float) (Math.sin(angle * 2.0D + seed) * 0.12D);
                } else if (arc == 1) {
                    x = (float) (Math.sin(angle * 2.0D + seed) * 0.12D);
                    y = 0.5F + (float) (Math.cos(angle) * radius);
                    z = (float) (Math.sin(angle) * radius);
                } else {
                    x = (float) (Math.cos(angle) * radius);
                    y = 0.5F + (float) (Math.sin(angle * 2.0D + seed) * 0.12D);
                    z = (float) (Math.sin(angle) * radius);
                }
                if (segment > 0) {
                    electricLine(consumer, matrix,
                            previousX, previousY, previousZ,
                            x, y, z);
                }
                previousX = x;
                previousY = y;
                previousZ = z;
            }
        }
    }

    private static void electricLine(VertexConsumer consumer,
                                     Matrix4f matrix,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2) {
        consumer.addVertex(matrix, x1, y1, z1)
                .setColor(125, 220, 255, 225)
                .setNormal(0.0F, 1.0F, 0.0F)
                ;
        consumer.addVertex(matrix, x2, y2, z2)
                .setColor(235, 250, 255, 225)
                .setNormal(0.0F, 1.0F, 0.0F)
                ;
    }

    private static BlockState resolveBlockState(String blockId) {
        try {
            ResourceLocation loc = ResourceLocations.parse(blockId);
            Block block = BuiltInRegistries.BLOCK.get(loc);
            if (block == null) return null;
            return block.defaultBlockState();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void line(VertexConsumer vc, Matrix4f mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int a) {
        vc.addVertex(mat, x1, y1, z1).setColor(255, 255, 255, a).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, x2, y2, z2).setColor(255, 255, 255, a).setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void drawBoxLines(VertexConsumer vc, Matrix4f mat,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     int alpha) {
        line(vc, mat, x0, y0, z0, x1, y0, z0, alpha);
        line(vc, mat, x1, y0, z0, x1, y0, z1, alpha);
        line(vc, mat, x1, y0, z1, x0, y0, z1, alpha);
        line(vc, mat, x0, y0, z1, x0, y0, z0, alpha);

        line(vc, mat, x0, y1, z0, x1, y1, z0, alpha);
        line(vc, mat, x1, y1, z0, x1, y1, z1, alpha);
        line(vc, mat, x1, y1, z1, x0, y1, z1, alpha);
        line(vc, mat, x0, y1, z1, x0, y1, z0, alpha);

        line(vc, mat, x0, y0, z0, x0, y1, z0, alpha);
        line(vc, mat, x1, y0, z0, x1, y1, z0, alpha);
        line(vc, mat, x1, y0, z1, x1, y1, z1, alpha);
        line(vc, mat, x0, y0, z1, x0, y1, z1, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
