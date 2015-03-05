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

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityRay;
import cn.liutils.util.RenderUtils;

/**
 * This implementation is using multi billboards for varying textures. If you only use 1 texture don't use this procedure.(low efficiency)
 * @author WeathFolD
 */
public class RendererRayTiling<T extends EntityRay> extends RendererRayBase<T> {
	
	//buffer for current draw call
	protected Vec3 verts[] = new Vec3[4];
	
	protected boolean crossed = false;
	
	ResourceLocation texture;
	
	public double widthFp = 0.2, widthTp = 0.5;
	
	public double ratio = 2.0; // tWidth / tHeight
	
	public RendererRayTiling(ResourceLocation tex) {
		for(int i = 0; i < 4; ++i) {
			verts[i] = Vec3.createVectorHelper(0, 0, 0);
		}
		setTexture(tex);
	}
	
	public void setTexture(ResourceLocation tex) {
		texture = tex;
	}
	
	public void setRatio(double d) {
		ratio = d;
	}
	
	/**
	 * @param i which subBillboard index it is(index from 0...n)
	 */
	protected ResourceLocation nextTexture(T ent, int i) {
		return texture;
	}

	@Override
	protected void drawAtOrigin(T ent, double len, boolean h) {
		//len = Math.min(len, 20.0);
		double width = h ? widthFp : widthTp;
		double forw = width * ratio; //per billboard forward in distance.
		int i = 0;
		//System.out.println("d-" + dist);
		for(double cur = 0.0; cur < len; cur += forw) {
			RenderUtils.loadTexture(nextTexture(ent, i));
			int rotation = i % 2 == 0 ? 30 : 0;
			GL11.glPushMatrix(); {
				double u0 = 0.0, u1 = Math.min(1.0, (len - cur) / forw), v0 = 0.0, v1 = 1.0;
				double z0 = cur, z1 = Math.min(len, cur + forw);
				sv(verts[0], 0, -0.5 * width, z0);
				sv(verts[1], 0, 0.5 * width, z0);
				sv(verts[2], 0, 0.5 * width, z1);
				sv(verts[3], 0, -0.5 * width, z1);
				drawPerBillboard(ent, i);
				billboard(u0, v0, u1, v1);
				if(crossed) {
					GL11.glRotated(90, 0, 0, 1);
					billboard(u0, v0, u1, v1);
				}
				i++;
			} GL11.glPopMatrix();
		}
	}
	
	protected void drawPerBillboard(T ent, int i) {}
	
	protected void billboard(double u0, double v0, double u1, double v1) {
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		if(disableLight) t.setBrightness(15728880);
		t.addVertexWithUV(verts[0].xCoord, verts[0].yCoord, verts[0].zCoord, u0, v0);
		t.addVertexWithUV(verts[1].xCoord, verts[1].yCoord, verts[1].zCoord, u0, v1);
		t.addVertexWithUV(verts[2].xCoord, verts[2].yCoord, verts[2].zCoord, u1, v1);
		t.addVertexWithUV(verts[3].xCoord, verts[3].yCoord, verts[3].zCoord, u1, v0);
		t.draw();
	}

}
