/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityRay;

/**
 * This only peforms correct translation.
 * @author WeathFolD
 */
public abstract class RendererRayBase<T extends EntityRay> extends Render {
	

	protected boolean 
		disableLight = true,
		viewOptimize = true;
	
	protected double 
		fpOffsetX = -0.09,
		fpOffsetY = -0.02,
		fpOffsetZ = -0.02;

	protected double 
		tpOffsetX = -0.3,
		tpOffsetY = -0.6,
		tpOffsetZ = 0.2;
	
	protected double alpha = 1.0;

	public RendererRayBase() {
	}
	
	public void setAlpha(double f) {
		alpha = f;
	}

	@Override
	public final void doRender(Entity var1, double x, double y, double z,
			float h, float a) {
		
		long time = Minecraft.getSystemTime();
		T er = (T) var1;
		if(!er.isLoaded()) {
			return;
		}
		er.beforeRender();
		x = er.posX - RenderManager.renderPosX;
		y = er.posY - RenderManager.renderPosY;
		z = er.posZ - RenderManager.renderPosZ;
		
		EntityPlayer clientPlayer = Minecraft.getMinecraft().thePlayer;
		boolean firstPerson = 
				Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 
				&& clientPlayer.equals(er.getSpawner());
		
		double len = er.getDisplayRayLen();
		
		//System.out.println(firstPerson);
		if(er.doesFollowSpawner()) {
			//Pos injection, for better viewing effect
			x = 0;
			y = 0;
			z = 0;
		}
		
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		if(disableLight) GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glPushMatrix(); {
			GL11.glTranslated(x, y, z);
			GL11.glRotated(er.rotationYaw, 0, -1, 0);
			GL11.glRotated(er.rotationPitch, 1, 0, 0);
			GL11.glColor4d(1, 1, 1, alpha * er.getAlpha());
			
			if(firstPerson) {
				if(er.isNearPlayer())
					transformFirstPerson(er, x, y, z);
			} else {
				if(er.isNearPlayer())
					transformThirdPerson(er, x, y, z);
				optimizeView(er, x, y, z);
			}
			
			if(disableLight) {
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
				Tessellator.instance.setBrightness(15728880);
			}
			drawAtOrigin(er, len, firstPerson);
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	protected void transformFirstPerson(T ent, double x, double y, double z) {
		if(this.viewOptimize) 
			GL11.glTranslated(fpOffsetX, fpOffsetY, fpOffsetZ);
	}
	
	protected void transformThirdPerson(T ent, double x, double y, double z) {
		if(this.viewOptimize)
			GL11.glTranslated(tpOffsetX, tpOffsetY, tpOffsetZ);
	}
	
	protected void optimizeView(T ent, double x, double y, double z) {
		{ //View-orientation fixing
			double 
				tanα = Math.tan(Math.atan2(y, Math.sqrt(x * x + z * z)) - ent.rotationPitch * Math.PI / 180),
				tanβ = Math.tan(Math.atan2(x, z) - ent.rotationYaw * Math.PI / 180);
			double φ;
			if(tanα == 0) {
				φ = Math.atan(tanβ);
			} else {
				φ = Math.atan(tanβ * Math.sqrt(1 + 1/(tanα * tanα)));
			}
			φ *= 180 / Math.PI;
			GL11.glRotated(-φ + 90, 0, 0, 1);
		}
	}
	
	protected abstract void drawAtOrigin(T ent, double len, boolean firstPerson);
	
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
