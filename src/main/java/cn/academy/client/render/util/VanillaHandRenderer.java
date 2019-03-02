package cn.academy.client.render.util;

import cn.academy.datapart.HandRenderOverrideData;
import cn.lambdalib2.registry.mc.RegEventHandler;
import cn.lambdalib2.util.ReflectionUtils;
import cn.lambdalib2.vis.CompTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SideOnly(Side.CLIENT)
public class VanillaHandRenderer {
    private static Field _fFarPlaneDistance = ReflectionUtils.getObfField(EntityRenderer.class, "farPlaneDistance", "field_78530_s");
    private static Method _mGetFOVModifier = ReflectionUtils.getObfMethod(EntityRenderer.class, "getFOVModifier", "func_78481_a",
        java.lang.Float.TYPE, java.lang.Boolean.TYPE);
    private static Method _mViewBobbing = ReflectionUtils.getObfMethod(EntityRenderer.class, "applyBobbing", "func_78475_f",
        java.lang.Float.TYPE);

    public static void renderHand(float partialTicks) {
        renderHand(partialTicks, CompTransform.identity);
    }

    public static void renderHand(float partialTicks, CompTransform transform) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            EntityRenderer renderer = mc.entityRenderer;

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();

            GL11.glEnable(GL11.GL_BLEND);

            Project.gluPerspective(
                (float) _mGetFOVModifier.invoke(renderer, partialTicks, false),
                (float) mc.displayWidth / mc.displayHeight, 0.05F,
                (float) _fFarPlaneDistance.get(renderer) * 2.0F);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            transform.doTransform();

            if (mc.gameSettings.viewBobbing) {
                _mViewBobbing.invoke(renderer, partialTicks); // setupViewBobbing(partialTicks)
            }

            renderer.itemRenderer.renderItemInFirstPerson(partialTicks);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public enum EventHandler {
        @RegEventHandler
        instance;

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onRenderHand(RenderHandEvent ev) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            HandRenderOverrideData data = HandRenderOverrideData.get(player);
            Minecraft mc = Minecraft.getMinecraft();

            if (data.isRendererPresent()) {
                ev.setCanceled(true);

                if (mc.gameSettings.thirdPersonView == 0 &&
                    !mc.player.isPlayerSleeping() &&
                    !mc.gameSettings.hideGUI) {
                    mc.entityRenderer.enableLightmap();
                    data.getRenderer().renderHand(ev.getPartialTicks());
                    mc.entityRenderer.disableLightmap();
                }
            }
        }
    }

}
