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
package cn.academy.core.client.render;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.api.client.render.SkillRenderer;
import cn.liutils.util.RenderUtils;
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
	public void renderHandEffect(EntityPlayer player, HandRenderType type, long time) {
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void renderSurroundings(EntityPlayer player, long time) {
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
