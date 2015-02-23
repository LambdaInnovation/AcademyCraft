/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACModels;
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
			{0, 1, 0},
			{1, 1, 0},
			{1, 1, 1},
			{0, 1, 1},
	};
	final double[] rotArray = {
		0, -90, -180, -270, 0, -90, -180, -270
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
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPushMatrix();
			GL11.glTranslated(x - width / 2, y, z - width / 2);
			GL11.glColor4d(marker.r, marker.g, marker.b, marker.a);
			for(int i = 0; i < 8; ++i) {
				GL11.glPushMatrix();
				boolean rev = i < 4;
				double sx = width * mulArray[i][0], sy = height * mulArray[i][1], sz = width * mulArray[i][2];
				final double len = 0.2 * width;
				GL11.glTranslated(sx, sy, sz);
				GL11.glRotated(rotArray[i], 0, 1, 0);
				GL11.glColor4d(marker.r, marker.g, marker.b, marker.a);
				GL11.glLineWidth(3f);
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
				t.startDrawing(GL11.GL_LINES);
				t.setBrightness(15728880);
				t.addVertex(0, 0, 0);
				t.addVertex(0, rev ? len : -len, 0);
				t.addVertex(0, 0, 0);
				t.addVertex(len, 0, 0);
				t.addVertex(0, 0, 0);
				t.addVertex(0, 0, len);
				t.draw();
				
				GL11.glPushMatrix();
				GL11.glScaled(0.1, 0.1, 0.1);
				//ACModels.MDL_SOLAR.renderAll();
				GL11.glPopMatrix();
				GL11.glPopMatrix();
			}
			GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_LIGHTING);
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
