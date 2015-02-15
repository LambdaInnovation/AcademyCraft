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
import cn.liutils.api.gui.widget.StateButton;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * The awesome wireless matrix
 * @author WeathFolD
 */
public class GuiMatrix extends LIGuiScreen {
	
	static final ResourceLocation 
		TEX = new ResourceLocation("academy:textures/guis/wireless_mat.png"),
		TEX_DIAG = new ResourceLocation("academy:textures/guis/wireless_dialogue.png");
	
	public boolean load = false;
	public String channelName;
	
	public Queue<Event> events = new LinkedList();
	
	PageMain pageMain;

	public GuiMatrix(TileMatrix mat) {
		gui.addWidget(pageMain = new PageMain());
	}
	
	public class PageMain extends Widget {
		
		SB ssid, pwd;
		
		public PageMain() {
			setSize(202, 185);
			this.alignStyle = AlignStyle.CENTER;
			this.initTexDraw(TEX, 0, 0, 404, 370);
			this.setTexResolution(512, 512);
		}
		
		@Override
		public void onAdded() {
			//31 49 149 21 ->31 104
			ssid = new SB() {
				@Override
				public void onMouseDown(double mx, double my) {
					gui.addWidget(new Dialogue() {});
				}
			};
			ssid.setPos(15.5, 24.5);
			
			pwd = new SB() {
				@Override
				public void onMouseDown(double mx, double my) {
					gui.addWidget(new Dialogue() {});
				}
			};
			pwd.setPos(15.5, 52);
			
			addWidgets(ssid, pwd);
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
		
		private abstract class SB extends Widget {
			public SB() {
				setSize(74.5, 10.5);
			}
			public void draw(double mx, double my, boolean h) {
				if(h) {
					RenderUtils.bindColor(126, 241, 241, 180);
					HudUtils.drawModalRect(0, 0, width, height);
				}
			}
		}
	}
	
	class Dialogue extends Widget {
		public Dialogue() {
			this.setSize(109.5, 94.5);
			this.alignStyle = AlignStyle.CENTER;
			this.initTexDraw(TEX_DIAG, 0, 0, 219, 189);
		}
		
		@Override
		public void onAdded() {
			pageMain.doesListenKey = false;
		}
		
		public void dispose() {
			pageMain.doesListenKey = true;
			super.dispose();
		}
		
		@Override
		public void draw(double mx, double my, boolean b) {
			drawBlackout();
			super.draw(mx, my, b);
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
