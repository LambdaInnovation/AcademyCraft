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
package cn.academy.generic.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.generic.entity.IRay;
import cn.liutils.util.VecUtils;

/**
 * @author WeAthFolD
 *
 */
public abstract class RendererRayBase<T extends IRay> extends Render {

	/**
	 * TODO: Too heavy mathematics and polar<->xyz. Maybe we can tweak it?
	 */
	@Override
	public void doRender(Entity entity, double x, double y, double z, 
			float a, float b) {
		T ray = (T) entity;
		
		NBTTagCompound nbt = entity.getEntityData();
		
		GL11.glPushMatrix();
		
		doTransform(ray);
		
		Vec3 position = ray.getPosition();
		Vec3 relativePosition = VecUtils.subtract(position, 
				VecUtils.vec(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ));
		VecUtils.glTranslate(relativePosition);
		
		//Calculate the most appropriate 'billboard-up' direction.
		
		//The ray viewing direction.
		Vec3 dir = VecUtils.toDirVector(entity.rotationYaw, entity.rotationPitch);
		//Pick two far enough start and end point.
		Vec3 start = VecUtils.vec(0, 0, 0), end = VecUtils.scalarMultiply(dir, 100);
		//Get closest point for view judging.
		Vec3 pt = VecUtils.getClosestPointOn(VecUtils.neg(relativePosition), start, end);
		//The player viewing direction towards pt.
		Vec3 perpViewDir = VecUtils.subtract(pt, VecUtils.neg(relativePosition));
		
		// cross product to get the 'up' vector
		Vec3 upDir = VecUtils.crossProduct(perpViewDir, dir);
		if(upDir.lengthVector() < 1.0E-2) {
			upDir = Vec3.createVectorHelper(nbt.getDouble("upX"), nbt.getDouble("upY"), nbt.getDouble("upZ"));
		}
		upDir.normalize();
		
		//Rotate the upDir to align it to z axis and calc the angle by arctan. Now we get it!
		double phi = Math.atan2(Math.sqrt(upDir.xCoord * upDir.xCoord + upDir.zCoord * upDir.zCoord), upDir.yCoord) * 180 / Math.PI;
		
		GL11.glRotated(-90 -entity.rotationYaw, 0, 1, 0);
		GL11.glRotated(entity.rotationPitch, 0, 0, -1);
		
		doTransform(ray);
		
		GL11.glRotated(phi, 1, 0, 0);
		
		//Now delegate to the render itself~
		drawAtOrigin(ray);
		
		GL11.glPopMatrix();
		
		nbt.setDouble("upX", upDir.xCoord);
		nbt.setDouble("upY", upDir.yCoord);
		nbt.setDouble("upZ", upDir.zCoord);
	}
	
	protected void doPostTransform(T ray) {}
	
	protected void doTransform(T ray) {}
	
	/**
	 * Draw the ray at the origin. The ray's heading direction should be toward x+, 
	 * and normal is always in z direction.
	 */
	protected abstract void drawAtOrigin(T ray);

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
