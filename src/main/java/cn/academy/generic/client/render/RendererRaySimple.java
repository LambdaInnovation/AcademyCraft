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

import org.lwjgl.opengl.GL11;

import cn.academy.generic.client.Resources;
import cn.academy.generic.entity.IRay;
import cn.liutils.render.material.Material;
import cn.liutils.render.mesh.Mesh;
import cn.liutils.render.mesh.MeshUtils;

/**
 * @author WeAthFolD
 */
public class RendererRaySimple<T extends IRay> extends RendererRayBase<T> {
	
	public static final double DEFAULT_WIDTH = 0.5;
	
	public double width;
	
	/**
	 * tile should be 1px infinite tile, and blend should be square textures.
	 */
	Material blendIn, tile, blendOut;
	
	Mesh mesh, mesh2;
	
	public RendererRaySimple(Material _blendIn, Material _tile, Material _blendOut) {
		blendIn = _blendIn;
		tile = _tile;
		blendOut = _blendOut;
		
		mesh = MeshUtils.createBillboard(null, -0.5, -0.5, 0.5, 0.5);
		mesh2 = MeshUtils.createBillboard(null, -0.5, -0.5, 0.5, 0.5);
		
		setWidth(DEFAULT_WIDTH);
	}
	
	public RendererRaySimple setWidth(double w) {
		width = w;
		return this;
	}

	@Override
	protected void drawAtOrigin(T ray) {
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		
		GL11.glPushMatrix();
		GL11.glScaled(width, width, width);
		GL11.glTranslated(-0.5, 0, 0);
		mesh.draw(blendIn);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glScaled(ray.getLength(), width, width);
		GL11.glTranslated(0.5, 0, 0);
//		double u = ray.getLength() / width;
//		mesh2.setUV(1, new double[] { u, 0 });
//		mesh2.setUV(2, new double[] { u, 1 });
		mesh2.draw(tile);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glScaled(width, width, width);
		GL11.glTranslated(ray.getLength() / width + 0.5, 0, 0);
		mesh.draw(blendOut);
		GL11.glPopMatrix();
		
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		
	}
	
	public static RendererRaySimple createFromName(String name) {
		try {
			Material[] mats = Resources.getRayTextures(name);
			return new RendererRaySimple(mats[0], mats[1], mats[2]);
		} catch(Exception e) {
			System.out.println("FFFFF");
			e.printStackTrace();
			return null;
		}
	}

}
