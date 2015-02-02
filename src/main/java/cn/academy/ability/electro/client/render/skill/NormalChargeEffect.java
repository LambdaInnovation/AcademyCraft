/**
 * 
 */
package cn.academy.ability.electro.client.render.skill;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.particle.EntityCloudFX;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.client.render.CubePointFactory;
import cn.academy.ability.electro.client.render.RenderSmallArc;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.pattern.IPattern;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class NormalChargeEffect extends SkillRenderer {
	
	RenderSmallArc effSurround;
	
	public NormalChargeEffect(int itensity) {
		effSurround = new RenderSmallArc(new CubePointFactory(1.2, 2.1, 0.9), .7, itensity);
	}

	@SideOnly(Side.CLIENT)
	public void renderSurroundings(EntityPlayer player, long time) {
		GL11.glPushMatrix(); {
			GL11.glTranslated(-.6, 0, -.45);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			effSurround.draw();
			GL11.glColor4f(1, 1, 1, 0.3F);
			//RenderUtils.drawCube(1.2, 0.9, 2.1);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1, 1, 1, 1);
		} GL11.glPopMatrix();
	}
	
}
