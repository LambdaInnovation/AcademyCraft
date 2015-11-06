package cn.academy.ability.client.skilltree;

import javax.vecmath.Vector2d;

import static org.lwjgl.opengl.GL11.*;

import cn.academy.core.client.ACRenderingHelper;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.Font;
import cn.lambdalib.util.helper.Font.Align;

class RenderingHelper {
	
	static final Color 
		CRL_BACK = new Color().setColor4d(.4, .4, .4, .5),
		CRL_GLOW = new Color().setColor4d(1, 1, 1, .3);
	
	static final float ZLEV = 15;
	
	public static void drawBox(double x, double y, double width, double height) {
		glPushMatrix();
		glTranslated(0, 0, ZLEV);
		CRL_BACK.bind();
		HudUtils.colorRect(x, y, width, height);
		ACRenderingHelper.drawGlow(x, y, width, height, 3, CRL_GLOW);
		glPopMatrix();
	}
	
	public static void drawTextBox(String str, double x, double y, double size) {
		glEnable(GL_BLEND);
		drawTextBox(str, x, y, size, 233333, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit) {
		glEnable(GL_BLEND);
		drawTextBox(str, x, y, size, limit, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit, Align align) {
		Vector2d vec = Font.font.simDrawWrapped(str, size, limit);
		double X0 = x, Y0 = y, MARGIN = Math.min(5, size * 0.3);
		
		if(align == Align.CENTER) {
			X0 -= vec.x / 2;
		} else if(align == Align.RIGHT) {
			X0 -= vec.x;
		}
		
		drawBox(X0, Y0, MARGIN * 2 + vec.x, MARGIN * 2 + vec.y);
		
		glPushMatrix();
		glTranslated(0, 0, ZLEV);
		Font.font.drawWrapped(str, X0 + MARGIN, Y0 + MARGIN, size, 0xffffff, limit);
		glPopMatrix();
	}
	
}
