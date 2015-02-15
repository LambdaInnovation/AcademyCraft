/**
 * 
 */
package cn.academy.energy.client.gui;

import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * The awesome wireless matrix
 * @author WeathFolD
 */
public class GuiMatrix extends LIGuiScreen {
	
	static final ResourceLocation TEX = new ResourceLocation("academy:textures/guis/wireless_mat.png");
	
	public boolean load = false;
	public String channelName;
	
	public Queue<Event> events = new LinkedList();

	public GuiMatrix(TileMatrix mat) {
		gui.addWidget(new PageMain());
	}
	
	public static class PageMain extends Widget {
		public PageMain() {
			setSize(202, 185);
			this.alignStyle = AlignStyle.CENTER;
			this.initTexDraw(TEX, 0, 0, 404, 370);
			this.setTexResolution(512, 512);
		}
		
		@Override
		public void draw(double mx, double my, boolean hovering) {
			super.draw(mx, my, hovering);
			GL11.glPushMatrix(); {
				HudUtils.drawRect(125, 185, 250, 370, 72.5, 25, 145, 50);
			} GL11.glPopMatrix();
			
			//Progress bar
			double prog = .3;
			//h = 129
			GL11.glPushMatrix(); {
				double len = prog * 129;
				GL11.glTranslated(53.2, 126.5, 0);
				HudUtils.drawRect(-36, -32.25, 34, 383, 72, len / 2, 144, len);
			} GL11.glPopMatrix();
			
			RenderUtils.bindColor(108, 236, 236);
			String pct = String.format("%.2f%%", prog * 100);
			drawText(pct, 52, 122, 7.2f, Align.CENTER);
		}
	}
	
	public static interface Event {
		void execute(GuiMatrix mat);
	}
	
	public static class InitReply implements Event {
		final boolean successful;
		public InitReply(boolean suc) {
			successful = suc;
		}
		@Override
		public void execute(GuiMatrix mat) {
			// TODO Open GUI
		}
	}
	
	private static void drawText(String text, double x, double y, float size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	private static void drawText(String text, double x, double y, float size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}

}
