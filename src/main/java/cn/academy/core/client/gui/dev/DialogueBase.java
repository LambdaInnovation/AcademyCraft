/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.TextButton;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;

/**
 * Generic dialogue window.
 * @author WeathFolD
 */
public class DialogueBase extends Widget {
	
	public static final ResourceLocation TEXTURE = ACClientProps.TEX_GUI_AD_DIAG;
	
	public class ButtonWarning extends Button {
		
		public ButtonWarning(String id, double x, double y) {
			super(id, x, y);
			this.setTexMapping(465, 0, 47, 15);
			this.setDownMapping(465, 15);
		}
	}
	
	public class ButtonNormal extends Button {

		public ButtonNormal(String id, double x, double y) {
			super(id, x, y);
			this.setTexMapping(465, 30, 47, 15);
			this.setDownMapping(465, 45);
			this.setTextColor(dev.DEFAULT_COLOR[0] / 350.0, dev.DEFAULT_COLOR[1] / 350.0, dev.DEFAULT_COLOR[2] / 350.0, 1);
		}
		
	}
	
	private class Button extends TextButton {

		public Button(String id, double x, double y) {
			super(id, DialogueBase.this, x, y, 23.5, 7.5);
			this.setTexture(TEXTURE, 512, 512);
		}
		
		@Override
		public void draw(double mx, double my, boolean mouseHovering) {
			super.draw(mx, my, mouseHovering);
			GL11.glColor4d(1, 1, 1, 0.3);
			RenderUtils.loadTexture(TEXTURE);
			HudUtils.drawRect(-1.5, -.5, 410, 1, 26.5, 8.5, 53, 19);
			GL11.glColor4d(1, 1, 1, 1);
		}
		
	}
	
	GuiDeveloper dev;
	Blackout blackout;
	String title = "";

	public DialogueBase(String id, GuiDeveloper dev, int prio) {
		super(id, dev.getGui(), 0, 0, 115, 59);
		this.setTexture(TEXTURE, 512, 512);
		this.setTexMapping(0, 0, 230, 118);
		this.dev = dev;
		blackout = new Blackout("black_" + id);
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
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, title, 2.5, 1.5, 9);
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	/**
	 * Renders black cover
	 */
	private class Blackout extends Widget {
		
		public Blackout(String id) {
			super(id, DialogueBase.this.dev.getGui(), 
				0, 0, DialogueBase.this.dev.width, DialogueBase.this.dev.height);
			this.draw = true;
			this.receiveEvent = true;
			this.setAlignStyle(Alignment.TOPLEFT);
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
