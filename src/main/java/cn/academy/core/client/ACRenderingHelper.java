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

import org.lwjgl.opengl.GL11;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

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
		Tessellator t = Tessellator.instance;
		gdraw(GLOW_L,  x - s, 	  y, 		  s, 	 height);
		gdraw(GLOW_R,  x + width, y, 		  s, 	 height);
		gdraw(GLOW_U,  x, 	  	  y - s, 	  width, s);
		gdraw(GLOW_D,  x, 	  	  y + height, width, s);
		gdraw(GLOW_RU, x + width, y - s, 	  s, 	 s);
		gdraw(GLOW_RD, x + width, y + height, s, 	 s);
		gdraw(GLOW_LU, x - s, 	  y - s, 	  s, 	 s);
		gdraw(GLOW_LD, x - s, 	  y + height, s, 	 s);
	}
	
	public static boolean isThePlayer(EntityPlayer p) {
		return p.equals(Minecraft.getMinecraft().thePlayer);
	}
	
	public static double getHeightFix(EntityPlayer p) {
		return isThePlayer(p) ? 0.0 : 1.6;
	}
	
	/**
	 * Draws a circular progress bar at (0, 0) with radius 1
	 */
	public static void drawCircularProgbar(ResourceLocation texture, double progress) {
		
		progress *= 360;
		if(progress > 360)
			progress %= 360;
		
		Tessellator t = Tessellator.instance;
		
		GL11.glPushMatrix();
		RenderUtils.loadTexture(texture);
		for(int i = 0; i < 4; ++i) {
			double angle = Math.min(90, progress - 90 * i);
			if(angle <= 0)
				break;
			double u1, v1;
			t.startDrawing(GL11.GL_TRIANGLES);
			
			if(angle <= 45) {
				u1 = Math.tan(MathUtils.toRadians(angle));
				v1 = 0;
			} else {
				u1 = 1;
				v1 = 0;
				
				double x = Math.tan(MathUtils.toRadians(90 - angle));
				t.addVertexWithUV(1, -1, 0, 1, 0);
				t.addVertexWithUV(0, 0, 0, 0, 1);
				t.addVertexWithUV(1, -x, 0, 1, 1 - x);
			}
			
			t.addVertexWithUV(0, -1, 0, 0, 0);
			t.addVertexWithUV(0, 0, 0, 0, 1);
			t.addVertexWithUV(u1,  -1 -v1, 0, u1, v1);
			t.draw();
			
			GL11.glRotated(90, 0, 0, 1);
		}
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
	
	public static void lineSegment(double x0, double y0, double x1, double y1, float width) {
		float hw = width / 2;
		Tessellator t = Tessellator.instance;
		double dy = y1 - y0, dx = x1 - x0, len = Math.sqrt(dy * dy + dx * dx);
		double theta = MathUtils.toAngle(Math.atan2(dy, dx));
		GL11.glPushMatrix();
		GL11.glTranslated(x0, y0, 0);
		GL11.glRotated(theta, 0, 0, 1);
		t.startDrawingQuads();
		t.addVertex(0, -hw, 0);
		t.addVertex(0, hw, 0);
		t.addVertex(len, hw, 0);
		t.addVertex(len, -hw, 0);
		t.draw();
		GL11.glPopMatrix();
	}
	
	private static void gdraw(ResourceLocation tex, double x, double y, double width, double height) {
		RenderUtils.loadTexture(tex);
		HudUtils.rect(x, y, width, height);
	}
	
	private static ResourceLocation glowtex(String path) {
		return Resources.getTexture("guis/glow_" + path);
	}
	
}
