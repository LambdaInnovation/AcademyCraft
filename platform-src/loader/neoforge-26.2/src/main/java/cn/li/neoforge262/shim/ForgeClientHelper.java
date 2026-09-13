package cn.li.neoforge262.shim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** NeoForge-only helpers for first-person render event integration. */
public final class ForgeClientHelper {
    private ForgeClientHelper() {
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

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(translateX, translateY, translateZ);
            if (rotateX != 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
            }
            if (rotateY != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(rotateY));
            }
            if (rotateZ != 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(rotateZ));
            }
            minecraft.gameRenderer.itemInHandRenderer.submitHandsWithItems(
                    event.getPartialTick(),
                    poseStack,
                    event.getSubmitNodeCollector(),
                    player,
                    event.getPackedLight());
        } finally {
            poseStack.popPose();
        }
        return true;
    }
}
