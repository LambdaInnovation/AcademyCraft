/**
 * 
 */
package cn.academy.ability.electro.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.pattern.IPattern;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class SRElecBase extends SkillRenderer {
	
	public static class PlayerGen extends RenderSmallArc {
		public PlayerGen() {
			super(new CubePointFactory(1.2, 2.1, 0.9), .7);
		}
	}
	
	private Map<EntityPlayer, RenderSmallArc> effects = new HashMap<EntityPlayer, RenderSmallArc>();

	public SRElecBase() {
	}

	@SideOnly(Side.CLIENT)
	public void renderSurroundings(EntityPlayer player, IPattern pattern) {
		GL11.glPushMatrix(); {
			GL11.glTranslated(-.6, 0, -.45);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			getRenderer(player).draw();
			GL11.glColor4f(1, 1, 1, 0.3F);
			//RenderUtils.drawCube(1.2, 0.9, 2.1);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1, 1, 1, 1);
		} GL11.glPopMatrix();
	}
	
	private RenderSmallArc getRenderer(EntityPlayer p) {
		RenderSmallArc res = effects.get(p);
		if(res == null) {
			res = new PlayerGen();
			effects.put(p, res);
		}
		return res;
	}
	
}
