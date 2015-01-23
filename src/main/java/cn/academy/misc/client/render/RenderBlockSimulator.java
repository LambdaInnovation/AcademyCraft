/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityBlockSimulator;
import cn.liutils.util.RenderUtils;

/**
 * @author WeathFolD
 */
public class RenderBlockSimulator extends Render {

	public RenderBlockSimulator() {}

	@Override
	public void doRender(Entity ent, double x, double y, double z,
			float var8, float var9) {
		EntityBlockSimulator ebs = (EntityBlockSimulator) ent;
		if(!ebs.hasDepth)
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		if(!ebs.hasLight)
			GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_FOG);
		GL11.glPushMatrix(); {
			RenderUtils.loadTexture(ebs.texture);
			GL11.glTranslated(x + .05, y + .05, z + .05);
			RenderUtils.drawCube(.9, .9, .9, true);
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_FOG);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
