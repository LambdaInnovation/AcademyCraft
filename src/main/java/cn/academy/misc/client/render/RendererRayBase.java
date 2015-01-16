/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityRay;

/**
 * This only peforms correct translation.
 * @author WeathFolD
 */
public abstract class RendererRayBase extends Render {
	

	protected boolean 
		disableLight = true,
		viewOptimize = true;
	
	protected double 
		fpOffsetX = 0.0,
		fpOffsetY = -0.2,
		fpOffsetZ = -0.2;

	protected double 
		tpOffsetX = 0.0,
		tpOffsetY = -0.2,
		tpOffsetZ = -0.4;
	
	protected double alpha = 1.0;

	public RendererRayBase() {
	}
	
	public void setAlpha(double f) {
		alpha = f;
	}

	@Override
	public final void doRender(Entity var1, double x, double y, double z,
			float h, float a) {
		EntityRay er = (EntityRay) var1;
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		if(disableLight)
			GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glPushMatrix(); {
			GL11.glTranslated(x, y, z);
			GL11.glRotated(er.rotationYaw, 0, 1, 0);
			GL11.glRotated(er.rotationPitch, -1, 0, 0);
			if(this.viewOptimize) {
				boolean firstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
				if(firstPerson) {
					GL11.glTranslated(fpOffsetX, fpOffsetY, fpOffsetZ);
				} else {
					GL11.glTranslated(tpOffsetX, tpOffsetY, tpOffsetZ);
				}
			}
			if(disableLight) {
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
				Tessellator.instance.setBrightness(15728880);
			}
			drawAtOrigin(er);
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	protected abstract void drawAtOrigin(EntityRay ent);
	
	protected void sv(Vec3 v, double x, double y, double z) {
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}

	/* (non-Javadoc)
	 * @see net.minecraft.client.renderer.entity.Render#getEntityTexture(net.minecraft.entity.Entity)
	 */
	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
