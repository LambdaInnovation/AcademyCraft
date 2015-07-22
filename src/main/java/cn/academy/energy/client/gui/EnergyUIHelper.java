package cn.academy.energy.client.gui;

import javax.vecmath.Vector2d;

import cn.academy.core.client.ACRenderingHelper;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;

public class EnergyUIHelper {
	
	public static final Color 
		CRL_GLOW = new Color().setColor4i(0, 214, 232, 180),
		CRL_BACK = new Color().setColor4i(0, 128, 165, 90);
	
	public static void drawBox(double x, double y, double width, double height) {
		CRL_BACK.bind();
		HudUtils.colorRect(x, y, width, height);
		
		ACRenderingHelper.drawGlow(x, y, width, height, 3, CRL_GLOW);
	}
	
	public static void drawTextBox(String str, double x, double y, double size) {
		drawTextBox(str, x, y, size, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, Align align) {
		final double trimLength = 120;
		Vector2d vec = Font.font.simDrawWrapped(str, size, trimLength);
		double X0 = x, Y0 = y, MARGIN = Math.min(5, size * 0.3);
		
		if(align == Align.CENTER) {
			X0 -= vec.x / 2;
		} else if(align == Align.RIGHT) {
			X0 -= vec.x;
		}
		
		drawBox(X0, Y0, MARGIN * 2 + vec.x, MARGIN * 2 + vec.y);
		Font.font.drawWrapped(str, X0 + MARGIN, Y0 + MARGIN, size, 0xffffff, trimLength);
	}
	
}
