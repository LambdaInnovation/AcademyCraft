/**
 * 
 */
package cn.academy.ability.electro.client.render.skill;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.api.client.render.SkillRenderer;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.misc.IntRandomSequence;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class ChargePlaneEffect extends SkillRenderer {
	
	static ResourceLocation[] TEXS = ACClientProps.ANIM_ARC_W;
	static double[][] pts = {
			{.3, -0.1},
			{0.6, 0.7},
			{0.8, 0.05},
			{-0.05, 0.7}
	};
	static double[] sizes = { 0.3, 0.4, 0.2, 0.4 };
	
	IntRandomSequence seq = new IntRandomSequence(4, TEXS.length);
	long lct;

	public ChargePlaneEffect() {}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void renderHud(EntityPlayer player, ScaledResolution sr, long time) {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glDepthMask(false);
		//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			GL11.glColor4d(1, 1, 1, 0.4);
			double w = sr.getScaledWidth_double(), h = sr.getScaledHeight_double();
			if(lct == 0 || time - lct > 200) {
				lct = time;
				seq.rebuild();
			}
			for(int i = 0; i < 4; ++i) {
				RenderUtils.loadTexture(TEXS[seq.get(i)]);
				HudUtils.drawRect(w * pts[i][0], h * pts[i][1], sizes[i] * w, sizes[i] * w);
			}
			RenderUtils.bindIdentity();
		} GL11.glPopMatrix();
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}

}
