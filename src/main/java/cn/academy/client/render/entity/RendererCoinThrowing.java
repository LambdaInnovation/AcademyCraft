package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.entity.EntityCoinThrowing;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 
 * @author KSkun
 */
@RegEntityRender(EntityCoinThrowing.class)
public class RendererCoinThrowing extends Render {

    public RendererCoinThrowing(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(Entity var1, double x, double y, double z,
            float var8, float var9) {
        EntityCoinThrowing etc = (EntityCoinThrowing) var1;
        EntityPlayer player = etc.player;
        boolean fp = player == Minecraft.getMinecraft().player
                && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
        
        double dt = (GameTimer.getTime() * 1000) % 150;
        
        if(etc.player == null)
            return;
        //If syncedSingle and in client computer, do not render
        if(etc.isSync && player == Minecraft.getMinecraft().player)
            return;
        if(etc.posY < player.posY)
            return;
        GL11.glPushMatrix(); {
            //x = player.posX - RenderManager.renderPosX;
            //y = etc.posY - RenderManager.renderPosY;
            //z = player.posZ - RenderManager.renderPosZ;
            if(player == Minecraft.getMinecraft().player) {
                x = z = 0;
            }
            
            GL11.glTranslated(x, y, z);
            if(fp) {
                GL11.glRotated(player.rotationYaw, 0, -1, 0);
            } else GL11.glRotated(player.renderYawOffset, 0, -1, 0);
            GL11.glTranslated(-0.63, 1, 0.30);
            float scale = 0.3F;
            GL11.glScalef(scale, scale, scale);
            GL11.glTranslated(0.5, 0.5, 0);
            GL11.glRotated((dt * 360.0 / 300.0), etc.axis.x, etc.axis.y, etc.axis.z);
            GL11.glTranslated(-0.5, -0.5, 0);
            RenderUtils.drawEquippedItem(0.0625, Resources.TEX_COIN_FRONT, Resources.TEX_COIN_BACK);
        } GL11.glPopMatrix();
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Entity var1) {
        return null;
    }

}