/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Some drawing utils.
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@ForcePreloadTexture
public class ACRenderingHelper {
	
	public static ResourceLocation
		GLOW_L = glowtex("left"),
		GLOW_R = glowtex("right"),
		GLOW_U = glowtex("up"),
		GLOW_D = glowtex("down"),
		GLOW_RU = glowtex("ru"),
		GLOW_RD = glowtex("rd"),
		GLOW_LU = glowtex("lu"),
		GLOW_LD = glowtex("ld");

	public static void drawGlow(double x, double y, double width, double height, double size, Color glowColor) {
		glowColor.bind();
		
		final double s = size;
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		gdraw(GLOW_L,  x - s, 	  y, 		  s, 	 height);
		gdraw(GLOW_R,  x + width, y, 		  s, 	 height);
		gdraw(GLOW_U,  x, 	  	  y - s, 	  width, s);
		gdraw(GLOW_D,  x, 	  	  y + height, width, s);
		gdraw(GLOW_RU, x + width, y - s, 	  s, 	 s);
		gdraw(GLOW_RD, x + width, y + height, s, 	 s);
		gdraw(GLOW_LU, x - s, 	  y - s, 	  s, 	 s);
		gdraw(GLOW_LD, x - s, 	  y + height, s, 	 s);
	}
	
	private static void gdraw(ResourceLocation tex, double x0, double y0, double w, double h) {
		RenderUtils.loadTexture(tex);
		HudUtils.rect(x0, y0, w, h);
	}
	
	private static ResourceLocation glowtex(String path) {
		return Resources.getTexture("guis/glow_" + path);
	}
	
}
