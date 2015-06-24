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
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.core.entity.IRay;
import cn.liutils.util.client.ViewOptimize;
import cn.liutils.util.generic.VecUtils;

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
		
//		if(ray.needsViewOptimize()) {
//			ViewOptimize.fix(ray);
//		}
		double length = ray.getLength();
		
		// A advanced ray view optimization with no ending point displacement.
		// This works by reversely calculating the angle needed to rotate and the length of the 'fixed' ray,
		// And then perform the rotation at the end point of the ray.
		if(ray.needsViewOptimize()) {
			Vec3 fix = ViewOptimize.getFixVector(ray);
			double dx = length - fix.xCoord, dy = 0 - fix.yCoord, dz = 0 - fix.zCoord;
			double dxzsq = dx * dx + dz * dz;
			double pitch = Math.atan2(dy, Math.sqrt(dxzsq)) * 180 / Math.PI;
			double yaw = Math.atan2(dx, dz) * 180 / Math.PI;
			
			length = Math.sqrt(dxzsq + dy * dy);
			
			// Apply transformation
			GL11.glTranslated(length, 0, 0);
			GL11.glRotated(270 + yaw, 0, 1, 0);
			GL11.glRotated(pitch, 0, 0, 1);
			GL11.glTranslated(-length, 0, 0);
		}
		
		GL11.glRotatef(ent.rotationPitch, 0, 0, -1);
		double fix = ray.getStartFix();
		
		GL11.glTranslated(fix, 0, 0);
		draw(ent, ray.getLength() - fix);
		
		GL11.glPopMatrix();
	}
	
	/**
	 * Render the ray in x+ direction. The transformation is automatically applied.
	 * Note that if you want view optimizing, you must do it yourself( We don't know where is your begin pos and where is end).
	 */
	protected abstract void draw(Entity entity, double suggestedLength);

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
