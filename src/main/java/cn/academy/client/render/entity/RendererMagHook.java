package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.entity.EntityMagHook;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
//import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
@RegEntityRender(EntityMagHook.class)
public class RendererMagHook extends Render {

    final ObjLegacyRender model = Resources.getModel("maghook"),
            model_open = Resources.getModel("maghook_open");
    
    final ResourceLocation texture = Resources.getTexture("models/maghook");

    public RendererMagHook(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(Entity ent, double x, double y, double z, float a,
            float b) {
        EntityMagHook hook = (EntityMagHook) ent;
        ObjLegacyRender realModel = model;
        if (hook.isHit) {
            realModel = model_open;
            hook.preRender();
            x = hook.posX - renderManager.viewerPosX;
            y = hook.posY - renderManager.viewerPosY;
            z = hook.posZ - renderManager.viewerPosZ;
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