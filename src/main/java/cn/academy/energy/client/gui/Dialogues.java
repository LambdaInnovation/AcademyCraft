package cn.academy.energy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.Widget.AlignStyle;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

public class Dialogues {
	
	static ResourceLocation TEX_DIAG = new ResourceLocation("academy:textures/guis/wireless_dialogue.png");
	
	enum DiagState { 
		LOADING(279, 95), FAIL(253, 95), SUCCESS(220, 95);
		DiagState(int _u, int _v) {
			u = _u;
			v = _v;
		}
		public final int u, v;
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
		public String msg = ACLangs.transmitting();

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
			drawText(msg, 53, 60, 6, Align.CENTER);
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
	
	private static void drawText(String text, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	private static void drawText(String text, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}

}
