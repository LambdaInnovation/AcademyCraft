/**
 * 
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
	
	ResourceLocation texture;
	
	public double width = 0.2;
	
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
	protected void drawAtOrigin(T ent) {
		double dist = ent.getTraceDistance();
		dist = Math.min(dist, 20.0);
		double forw = width * ratio; //per billboard forward in distance.
		int i = 0;
		//System.out.println("d-" + dist);
		for(double cur = 0.0; cur < dist; cur += forw) {
			RenderUtils.loadTexture(nextTexture(ent, i));
			int rotation = i % 2 == 0 ? 30 : 0;
			GL11.glPushMatrix(); {
				double u0 = 0.0, u1 = Math.min(1.0, (dist - cur) / forw), v0 = 0.0, v1 = 1.0;
				double z0 = cur, z1 = Math.min(dist, cur + forw);
				sv(verts[0], 0, -0.5 * width, z0);
				sv(verts[1], 0, 0.5 * width, z0);
				sv(verts[2], 0, 0.5 * width, z1);
				sv(verts[3], 0, -0.5 * width, z1);
				drawPerBillboard(ent, i);
				billboard(u0, v0, u1, v1);
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
