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
package cn.academy.energy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.InputBox;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

public class Dialogues {
	
	static ResourceLocation TEX_DIAG = new ResourceLocation("academy:textures/guis/wireless_dialogue.png");
	
	enum DiagState { 
		LOADING(279, 95, ACLangs.transmitting()), 
		FAIL(253, 95, ACLangs.opFailed()), 
		SUCCESS(220, 95, ACLangs.opSuccessful());
		DiagState(int _u, int _v, String str) {
			u = _u;
			v = _v;
			msg = str;
		}
		public final int u, v;
		public final String msg;
	};

	public static abstract class Dialogue extends Widget {
		public Dialogue() {
			this.setSize(109.5, 94.5);
			this.alignStyle = AlignStyle.CENTER;
			this.initTexDraw(TEX_DIAG, 0, 0, 219, 189);
			this.setTexResolution(512, 512);
		}
		
		@Override
		public void draw(double mx, double my, boolean b) {
			drawBlackout();
			super.draw(mx, my, b);
		}
	}
	
	public static class WigOK extends Widget {
		{
			setSize(10.5, 10.5);
			initTexDraw(TEX_DIAG, 220, 121, 21, 21);
		}
	}
	
	public static class WigBad extends Widget {
		{
			setSize(10.5, 10.5);
			initTexDraw(TEX_DIAG, 253, 95, 21, 21);
		}
	}
	
	public static class StateDiag extends Dialogue {
		
		public DiagState state = DiagState.LOADING;

		public StateDiag() {}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			
			GL11.glColor4d(0.3, 1, 1, 0.8);
			drawText(ACLangs.opStatus(), 54, 10, 7.5, Align.CENTER);
			
			GL11.glColor4d(1, 1, 1, 1);
			RenderUtils.loadTexture(TEX_DIAG);
			
			HudUtils.drawRect(49, 36, state.u, state.v, 10.5, 10.5, 21, 21);
			
			GL11.glColor4d(1, 1, 1, 0.5);
			GL11.glPushMatrix(); {
				GL11.glTranslated(54, 41, 0);
				GL11.glRotated(Minecraft.getSystemTime() / 200D, 0, 0, 1);
				//GL11.glRotated(0, 0, 0, 1);
				final double wid = 24.5, hi = 28.5;
				HudUtils.drawRect(-wid / 2, -hi / 2, 226, 36, wid, hi, wid * 2, hi * 2);
			} GL11.glPopMatrix();
			
			GL11.glColor4d(0, 1, 1, 0.8);
			drawText(state.msg, 53, 60, 6, Align.CENTER);
			RenderUtils.bindIdentity();
		}
		
		public void initCancel() {
			addWidget(new Dialogues.WigOK() {
				{
					setPos(49, 72);
				}
				@Override
				public void onMouseDown(double mx, double my) {
					Minecraft.getMinecraft().thePlayer.closeScreen();
				}
			});
		}
		
	}
	
	abstract static class InputPassword extends Dialogue {
		
		public final String cn;
		InputBox box;

		public InputPassword(String _cn) {
			cn = _cn;
		}
		
		public abstract void performAction(String pwd);
		
		@Override
		public void onAdded() {
			addWidget(box = new InputBox(46, 51, 46, 8.5, 6, 1, 12)
				.setFont(ACClientProps.FONT_YAHEI_32).setEcho(true).setTextColor(0, 255, 255, 255)); 
			
			addWidget(new Dialogues.WigOK() {
				{
					setPos(50, 75);
				}
				
				@Override
				public void onMouseDown(double mx, double my) {
					performAction(box.getContent());
				}
			});
		}
		
		@Override
		public void draw(double mx, double my, boolean b) {
			super.draw(mx, my, b);
			RenderUtils.bindColor(100, 255, 255);
			drawText(ACLangs.wirelessLogin(), 54, 10, 7, Align.CENTER);
			
			//23 202 172 21
			RenderUtils.loadTexture(Dialogues.TEX_DIAG);
			HudUtils.drawRect(9, 30, 23, 202, 86, 10.5, 172, 21);
			HudUtils.drawRect(9, 50, 23, 230, 86, 10.5, 172, 21);
			
			drawText(cn, 68, 31, 6, Align.CENTER, 48);
		}
		
	}
	
	public static void drawText(String text, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	public static void drawText(String text, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}
	
	public static void drawText(String text, double x, double y, double size, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(text, x, y, size, cst);
	}
	
	public static void drawText(String text, double x, double y, double size, Align align, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(text, x, y, size, align, cst);
	}

}
