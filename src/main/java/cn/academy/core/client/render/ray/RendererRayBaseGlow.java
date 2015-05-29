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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.core.entity.IRay;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.client.ViewOptimize;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Motion3D;

/**
 * Renderer to draw glow texture
 * @author WeAthFolD
 */
public abstract class RendererRayBaseGlow<T extends IRay> extends Render {

	@Override
	public void doRender(Entity entity, double x, double y, double z, 
			float a, float b) {
		T ray = (T) entity;
		
		NBTTagCompound nbt = entity.getEntityData();
		Minecraft mc = Minecraft.getMinecraft();
		
		GL11.glPushMatrix();
		
		doTransform(ray);
		
		Vec3 position = ray.getPosition();
		Vec3 relativePosition = VecUtils.subtract(position, 
				VecUtils.vec(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ));
		RenderUtils.glTranslate(relativePosition);
		
		//Calculate the most appropriate 'billboard-up' direction.
		
		//The ray viewing direction.
		Vec3 dir = new Motion3D(entity, true).getMotionVec();
		//Pick two far enough start and end point.
		Vec3 start = VecUtils.scalarMultiply(dir, ray.getStartFix()), end = VecUtils.add(start, VecUtils.scalarMultiply(dir, ray.getLength() - ray.getStartFix()));
		//Get closest point for view judging.
		Vec3 pt = VecUtils.vec(0, 0, 0);
		
		//The player viewing direction towards pt.
		Vec3 perpViewDir = VecUtils.subtract(pt, VecUtils.neg(relativePosition));
		
		// cross product to get the 'up' vector
		Vec3 upDir = VecUtils.crossProduct(perpViewDir, dir);
		
		//TODO: Is this really necessary?
//		if(upDir.lengthVector() < 1.0E-2) {
//			upDir = Vec3.createVectorHelper(nbt.getDouble("upX"), nbt.getDouble("upY"), nbt.getDouble("upZ"));
//		}
		upDir = upDir.normalize();
		
		//DEBUG
//		GL11.glDisable(GL11.GL_TEXTURE_2D);
//		Tessellator t = Tessellator.instance;
//		
//		t.startDrawing(GL11.GL_LINES);
//		//VecUtils.tessellate(start);
//		//VecUtils.tessellate(end);
//		
//		VecUtils.tessellate(pt);
//		VecUtils.tessellate(VecUtils.add(pt, VecUtils.scalarMultiply(upDir, 5)));
//		t.draw();
//		
//		GL11.glEnable(GL11.GL_TEXTURE_2D);
		//DEBUG END
		if(ray.needsViewOptimize()) {
			Vec3 vec = ViewOptimize.getFixVector(ray);
			vec.rotateAroundY((float) ((270 - entity.rotationYaw) / 180 * Math.PI));
			start = VecUtils.add(start, vec);
			end = VecUtils.add(end, vec);
		}
		
		doTransform(ray);
		
		//Now delegate to the render itself~
		draw(ray, start, end, upDir);
		
		GL11.glPopMatrix();
		
//		nbt.setDouble("upX", upDir.xCoord);
//		nbt.setDouble("upY", upDir.yCoord);
//		nbt.setDouble("upZ", upDir.zCoord);
	}
	
	protected void doPostTransform(T ray) {}
	
	protected void doTransform(T ray) {}
	
	protected void drawBoard(Vec3 start, Vec3 end, Vec3 upDir, double width) {
		width /= 2;
		Vec3 
			v1 = VecUtils.add(start, VecUtils.scalarMultiply(upDir, width)),
			v2 = VecUtils.add(start, VecUtils.scalarMultiply(upDir, -width)),
			v3 = VecUtils.add(end, 	 VecUtils.scalarMultiply(upDir, -width)),
			v4 = VecUtils.add(end,   VecUtils.scalarMultiply(upDir, width));
		
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		t.setBrightness(15728880);
		RenderUtils.addVertex(v1, 0, 1);
		RenderUtils.addVertex(v2, 0, 0);
		RenderUtils.addVertex(v3, 1, 0);
		RenderUtils.addVertex(v4, 1, 1);
		t.draw();
	}
	
	/**
	 * Draw the ray at the origin. The ray's heading direction should be toward x+, 
	 * and normal is always in z direction.
	 * @param start THe start point
	 * @param end The end point
	 * @param sideDir the suggested billboard-up direction. You can ignore this if not drawing a billboard.
	 */
	protected abstract void draw(T ray, Vec3 start, Vec3 end, Vec3 sideDir);

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
