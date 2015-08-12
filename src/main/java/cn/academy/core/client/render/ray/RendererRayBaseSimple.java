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

import static cn.liutils.util.generic.VecUtils.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.core.entity.IRay;
import cn.liutils.util.client.ViewOptimize;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.Motion3D;

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
		
		double length = ray.getLength();
		double fix = ray.getStartFix();
		
		Vec3 vo;
		if(ray.needsViewOptimize())
			vo = ViewOptimize.getFixVector(ray);
		else
			vo = vec(0, 0, 0);
		// Rotate fix vector to world coordinate
		vo.rotateAroundY(MathUtils.toRadians(270 - ent.rotationYaw));
		
		Vec3 start = vec(0, 0, 0),
			end = add(start, multiply(new Motion3D(ent, true).getMotionVec(), length));
		start = add(start, vo);
		
		x += start.xCoord;
		y += start.yCoord;
		z += start.zCoord;
		
		Vec3 delta = subtract(end, start);
		double dxzsq = delta.xCoord * delta.xCoord + delta.zCoord * delta.zCoord;
		double npitch = MathUtils.toAngle(Math.atan2(delta.yCoord, Math.sqrt(dxzsq)));
		double nyaw = MathUtils.toAngle(Math.atan2(delta.xCoord, delta.zCoord));
		
		GL11.glTranslated(x, y, z);
		GL11.glRotated(-90 + nyaw, 0, 1, 0);
		GL11.glRotated(npitch, 0, 0, 1);
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
