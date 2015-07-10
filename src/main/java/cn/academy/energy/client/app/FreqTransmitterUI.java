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
package cn.academy.energy.client.app;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACRenderingHelper;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;

/**
 * @author WeAthFolD
 */
public class FreqTransmitterUI extends AuxGui {
	
	private static final Color
		BG_COLOR = new Color().setColor4i(58, 77, 83, 100),
		GLOW_COLOR = new Color().setColor4i(0, 255, 251, 130);
	
	private static final double GLOW_SIZE = 2;

	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		GL11.glPushMatrix(); {
			
			GL11.glTranslated(15, 15, 0);
			
			final float isize = 18;
			final float fsize = 10;
			String str = "Frequency Transmitter";
			double len = Font.font.strLen(str, fsize);
			
			drawBox(0, 0, 30 + len, 18);
			
			ResourceLocation icon = AppFreqTransmitter.instance.getIcon();
			RenderUtils.loadTexture(icon);
			GL11.glColor4d(1, 1, 1, 1);
			HudUtils.rect(2, 0, isize, isize);
			
			Font.font.draw(str, isize + 6, 4, fsize, 0xffffff);
		
		} GL11.glPopMatrix();
		
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	private static void drawBox(double x, double y, double width, double height) {
		BG_COLOR.bind();
		HudUtils.colorRect(x, y, width, height);
		
		ACRenderingHelper.drawGlow(x, y, width, height, GLOW_SIZE, GLOW_COLOR);
	}

}
