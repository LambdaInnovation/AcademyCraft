/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.render.ray;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.entity.IRay;

/**
 * @author WeAthFolD
 *
 */
public abstract class RendererRayBaseSimple extends Render {

	@Override
	public void doRender(Entity ent, 
		double x, double y, double z, float a, float b) {
		IRay ray = (IRay) ent;
		
		GL11.glPushMatrix();
		
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(270 - ent.rotationYaw, 0, 1, 0);
		
		if(ray.needsViewOptimize()) {
			ViewOptimize.fix();
		}
		
		GL11.glRotatef(ent.rotationPitch, 0, 0, -1);
		
		draw(ent);
		
		GL11.glPopMatrix();
	}
	
	/**
	 * Render the ray in x+ direction. The transformation is automatically applied,.
	 */
	protected abstract void draw(Entity entity);

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
