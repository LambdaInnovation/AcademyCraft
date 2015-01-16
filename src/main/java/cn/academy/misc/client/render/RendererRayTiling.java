/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import cn.academy.misc.entity.EntityRay;
import cn.liutils.util.RenderUtils;

/**
 * This implementation is using multi billboards for varying textures. If you only use 1 texture don't use this procedure.(low efficiency)
 * @author WeathFolD
 */
public class RendererRayTiling extends RendererRayBase {
	
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
	
	protected ResourceLocation nextTexture() {
		return texture;
	}

	@Override
	protected void drawAtOrigin(EntityRay ent) {
		double dist = ent.getTraceDistance();
		double forw = width * ratio; //per billboard forward in distance.
		for(double cur = 0.0; cur < dist; cur += forw) {
			RenderUtils.loadTexture(nextTexture());
			double u0 = 0.0, u1 = Math.min(1.0, (dist - cur) / forw), v0 = 0.0, v1 = 1.0;
			double z0 = cur, z1 = Math.min(dist, cur + forw);
			sv(verts[0], -0.5 * width, 0, z0);
			sv(verts[1], 0.5 * width, 0, z0);
			sv(verts[2], 0.5 * width, 0, z1);
			sv(verts[3], -0.5 * width, 0, z1);
			billboard(u0, v0, u1, v1);
			
			sv(verts[0], 0, -0.5 * width, z0);
			sv(verts[1], 0, 0.5 * width, z0);
			sv(verts[2], 0, 0.5 * width, z1);
			sv(verts[3], 0, -0.5 * width, z1);
			//billboard(u0, v0, u1, v1);
		}
	}
	
	private void billboard(double u0, double v0, double u1, double v1) {
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		t.addVertexWithUV(verts[0].xCoord, verts[0].yCoord, verts[0].zCoord, u0, v0);
		t.addVertexWithUV(verts[1].xCoord, verts[1].yCoord, verts[1].zCoord, u0, v1);
		t.addVertexWithUV(verts[2].xCoord, verts[2].yCoord, verts[2].zCoord, u1, v1);
		t.addVertexWithUV(verts[3].xCoord, verts[3].yCoord, verts[3].zCoord, u1, v0);
		t.draw();
	}

}
