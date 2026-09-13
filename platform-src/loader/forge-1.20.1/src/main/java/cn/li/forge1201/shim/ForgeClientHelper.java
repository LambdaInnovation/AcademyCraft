package cn.li.forge1201.shim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * Forge-only client helpers. Vanilla registration helpers live in
 * {@link cn.li.mc1201.client.ClientHelper}.
 */
@OnlyIn(Dist.CLIENT)
public final class ForgeClientHelper {
    private ForgeClientHelper() {
    }

    public static void setFluidRenderLayerTranslucent(Fluid sourceFluid, Fluid flowingFluid) {
        ItemBlockRenderTypes.setRenderLayer(sourceFluid, RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(flowingFluid, RenderType.translucent());
    }

    public static boolean renderTransformedMainHand(RenderHandEvent event,
                                                    float translateX,
                                                    float translateY,
                                                    float translateZ,
                                                    float rotateX,
                                                    float rotateY,
                                                    float rotateZ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        if (!(event.getMultiBufferSource() instanceof MultiBufferSource.BufferSource bufferSource)) {
            return false;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(translateX, translateY, translateZ);
        if (rotateX != 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
        }
        if (rotateY != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotateY));
        }
        if (rotateZ != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotateZ));
        }

        minecraft.gameRenderer.itemInHandRenderer.renderHandsWithItems(
            event.getPartialTick(),
            poseStack,
            bufferSource,
            player,
            event.getPackedLight());
        poseStack.popPose();
        return true;
    }
}
