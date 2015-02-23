/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityMarker;

/**
 * @author WeathFolD
 *
 */
public class RenderMarker extends Render {
	
	final double[][] mulArray = {
			{0, 0, 0},
			{1, 0, 0},
			{1, 0, 1},
			{0, 0, 1},
			{0, 0, 0},
			{1, 1, 0},
			{1, 1, 1},
			{0, 1, 1},
	};
	final double[] rotArray = {
		0, 90, 180, 270, 0, 90, 180, 270
	};
	
	public RenderMarker() {}

	@Override
	public void doRender(Entity ent, double x, double y, double z,
			float a, float b) {
		EntityMarker marker = (EntityMarker) ent;
		Entity targ = marker.target;
		double width = targ.width, height = targ.height;
		Tessellator t = Tessellator.instance;
		if(targ != null) {
			x = targ.posX - RenderManager.renderPosX;
			y = targ.posY - RenderManager.renderPosY;
			z = targ.posZ - RenderManager.renderPosZ;
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			for(int i = 0; i < 8; ++i) {
				GL11.glPushMatrix();
				boolean rev = i >= 4;
				double sx = width * mulArray[i][0], sy = height * mulArray[i][1], sz = width * mulArray[i][2];
				final double len = 0.1 * width;
				GL11.glTranslated(sx, sy, sz);
				GL11.glRotated(rotArray[i], 0, 1, 0);
				GL11.glColor4d(marker.r, marker.g, marker.b, marker.a);
				t.startDrawing(GL11.GL_LINES);
				t.addVertex(0, 0, 0);
				t.addVertex(0, rev ? len : -len, 0);
				t.addVertex(0, 0, 0);
				t.addVertex(len, 0, 0);
				t.addVertex(0, 0, 0);
				t.addVertex(0, 0, len);
				t.draw();
				GL11.glPopMatrix();
			}
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
