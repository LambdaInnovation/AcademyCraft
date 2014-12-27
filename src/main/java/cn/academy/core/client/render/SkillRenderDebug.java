/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.SkillState;
import cn.liutils.api.client.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class SkillRenderDebug extends SkillRenderer {
	
	public static SkillRenderDebug instance = new SkillRenderDebug();

	private SkillRenderDebug() {
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void renderHandEffect(EntityPlayer player, SkillState state, HandRenderType type) {
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void renderSurroundings(EntityPlayer player, SkillState state) {
		GL11.glPushMatrix(); {
			GL11.glTranslated(-.5, 0, -.5);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glColor4f(1, 1, 1, 0.3F);
			RenderUtils.drawCube(1, 1, 2);
			GL11.glDisable(GL11.GL_BLEND);
		} GL11.glPopMatrix();
	}
	
	private static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}

}
