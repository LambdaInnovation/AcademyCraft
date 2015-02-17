/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.StateButton;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;

/**
 * Generic dialogue window.
 * @author WeathFolD
 */
public class DialogueBase extends Widget {
	
	public static final ResourceLocation TEXTURE = ACClientProps.TEX_GUI_AD_DIAG;
	
	public abstract class ButtonWarning extends StateButton {
		
		public ButtonWarning(String id, double x, double y) {
			super(x, y, 23.5, 7.5, TEXTURE, 47, 15,
				new double[][] {
					{465, 0},
					{465, 15},
					{465, 0}
				});
			this.setText(id);
			this.setTextData(4, new int[][] {
				{255, 95, 95, 255},
				{255, 95, 95, 255},
				{150, 150, 150, 255}
			});
			setFont(ACClientProps.FONT_YAHEI_32);
		}
		
		@Override
		public void draw(double mx, double my, boolean hov) {
			super.draw(mx, my, hov);
			drawButtonFrame();
		}
	}
	
	public abstract class ButtonNormal extends StateButton {

		public ButtonNormal(String id, double x, double y) {
			super(x, y, 23.5, 7.5, TEXTURE, 47, 15,
				new double[][] {
					{465, 30},
					{465, 45},
					{465, 30}
				});
			this.setText(id);
			this.setTextData(4, new int[][] {
				dev.DEFAULT_COLOR,
				dev.DEFAULT_COLOR,
				{150, 150, 150, 255}
			});
			setFont(ACClientProps.FONT_YAHEI_32);
		}
		
		@Override
		public void draw(double mx, double my, boolean hov) {
			super.draw(mx, my, hov);
			drawButtonFrame();
		}
		
	}
	
	private static void drawButtonFrame() {
		GL11.glColor4d(1, 1, 1, 0.3);
		RenderUtils.loadTexture(TEXTURE);
		HudUtils.drawRect(-1.5, -.5, 410, 1, 26.5, 8.5, 53, 19);
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	GuiDeveloper dev;
	Blackout blackout;
	String title = "";

	public DialogueBase(String id, GuiDeveloper dev, int prio) {
		super(0, 0, 115, 59);
		this.initTexDraw(TEXTURE, 0, 0, 230, 117);
		this.dev = dev;
		dev.getGui().addWidget(blackout = new Blackout("black_" + id));
	}
	
	public void setTitle(String str) {
		title = str;
	}
	
	@Override
	public void dispose() {
		blackout.dispose();
		super.dispose();
	}
	
	@Override
	public void draw(double mx, double my, boolean mouseHovering) {
		super.draw(mx, my, mouseHovering);
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		GuiDeveloper.drawText(title, 2.5, 2.4, 6);
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	/**
	 * Renders black cover
	 */
	private class Blackout extends Widget {
		
		public Blackout(String id) {
			super(0, 0, DialogueBase.this.dev.width, DialogueBase.this.dev.height);
		}
		
		@Override
		public void draw(double mx, double my, boolean mouseHovering) {
			GL11.glColor4d(0, 0, 0, 0.6);
			HudUtils.drawModalRect(0, 0, dev.width, dev.height);
			GL11.glColor4d(1, 1, 1, 1);
		}
		
		@Override
		public int getDrawPriority() {
			return DialogueBase.this.getDrawPriority() - 1;
		}
		
	}

}
