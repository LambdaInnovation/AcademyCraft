package cn.academy.vanilla.electromaster.client.renderer;

import cn.academy.core.Resources;
import cn.academy.vanilla.electromaster.entity.EntityMagHook;
import cn.lambdalib2.util.RenderUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RendererMagHook extends Render {

    final IModelCustom model = Resources.getModel("maghook"),
            model_open = Resources.getModel("maghook_open");
    
    final ResourceLocation texture = Resources.getTexture("models/maghook");

    @Override
    public void doRender(Entity ent, double x, double y, double z, float a,
            float b) {
        EntityMagHook hook = (EntityMagHook) ent;
        IModelCustom realModel = model;
        if (hook.isHit) {
            realModel = model_open;
            hook.preRender();
            x = hook.posX - RenderManager.renderPosX;
            y = hook.posY - RenderManager.renderPosY;
            z = hook.posZ - RenderManager.renderPosZ;
        }
        
        GL11.glPushMatrix();
        RenderUtils.loadTexture(texture);
        GL11.glTranslated(x, y, z);
        GL11.glRotated(-hook.rotationYaw + 90, 0, 1, 0);
        GL11.glRotated(hook.rotationPitch - 90, 0, 0, 1);
        double scale = 0.0054;
        GL11.glScaled(scale, scale, scale);
        realModel.renderAll();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity var1) {
        return null;
    }

}