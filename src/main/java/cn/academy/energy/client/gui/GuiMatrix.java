/**
 * 
 */
package cn.academy.energy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.academy.energy.msg.matrix.MsgGuiLoadQuery;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.InputBox;
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
	
	PageMain pageMain;

	public GuiMatrix(TileMatrix mat) {
		AcademyCraft.netHandler.sendToServer(new MsgGuiLoadQuery(mat));
	}
	
	@Override
    public boolean doesGuiPauseGame() {
        return false;
    }
	
	public static class PageMain extends Widget {
		
		final String channelName;
		final GuiMatrix mat;
		
		public PageMain(GuiMatrix _mat, String _channelName) {
			setSize(202, 185);
			channelName = _channelName;
			mat = _mat;
			this.alignStyle = AlignStyle.CENTER;
			this.initTexDraw(TEX, 0, 0, 404, 370);
			this.setTexResolution(512, 512);
		}
		
		@Override
		public void onAdded() {
			//31 49 149 21 ->31 104
			SB pwd = new SB() {
				@Override
				public void onMouseDown(double mx, double my) {
					mat.gui.addWidget(new Dialogue(mat) {});
				}
			};
			pwd.setPos(15.5, 52);
			
			addWidgets(pwd);
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
			
			drawText(channelName, 52, 25, 7f, Align.CENTER);
			drawText("******", 52, 55, 7f, Align.CENTER);
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
	
	static abstract class Dialogue extends Widget {
		final GuiMatrix mat;
		
		public Dialogue(GuiMatrix _mat) {
			mat = _mat;
			this.setSize(109.5, 94.5);
			this.alignStyle = AlignStyle.CENTER;
			this.initTexDraw(TEX_DIAG, 0, 0, 219, 189);
			this.setTexResolution(512, 512);
		}
		
		@Override
		public void onAdded() {
			mat.pageMain.doesListenKey = false;
		}
		
		public void dispose() {
			mat.pageMain.doesListenKey = true;
			super.dispose();
		}
		
		@Override
		public void draw(double mx, double my, boolean b) {
			drawBlackout();
			super.draw(mx, my, b);
		}
	}
	
	static class PageInit extends Dialogue {
		
		InputBox ssid, pwd, pwd2;

		public PageInit(GuiMatrix _mat) {
			super(_mat);
		}
		
		@Override
		public void onAdded() {
			//Confirm button
			addWidget(new Widget() {
				{
					setSize(10.5, 10.5);
					setPos(26, 73);
					initTexDraw(TEX_DIAG, 220, 95, 21, 21);
				}
				
				@Override
				public void onMouseDown(double mx, double my) {
					//VALIDATION
					
					//Close or continue
				}
			});
			
			//Abort button
			addWidget(new Widget() {
				{
					setSize(10.5, 10.5);
					setPos(69, 73);
					initTexDraw(TEX_DIAG, 253, 95, 21, 21);
				}
				
				@Override
				public void onMouseDown(double mx, double my) {
					//Close
					Minecraft.getMinecraft().thePlayer.closeScreen();
				}
			});
			
			addWidget(ssid = new InputBox(48, 25, 46, 8.5, 6, 1, 12)
				.setFont(ACClientProps.FONT_YAHEI_32).setTextColor(0, 255, 255, 255));
			
			addWidget(pwd = new InputBox(48, 39, 46, 8.5, 6, 1, 12)
				.setFont(ACClientProps.FONT_YAHEI_32).setEcho(true).setTextColor(0, 255, 255, 255));
			
			addWidget(pwd2 = new InputBox(48, 53, 46, 8.5, 6, 1, 12)
				.setFont(ACClientProps.FONT_YAHEI_32).setEcho(true).setTextColor(0, 255, 255, 255));
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			RenderUtils.bindColor(0, 255, 255, 255);
			drawText(ACLangs.matInit(), 54, 8.5, 7, Align.CENTER);
			
			//input elements
			RenderUtils.loadTexture(TEX_DIAG);
			HudUtils.drawRect(11, 24, 23, 202, 86, 38.5, 172, 77);
		}
		
	}
	
	public static interface Event {
		void execute(GuiMatrix mat);
	}
	
	public void executeEvent(Event event) {
		event.execute(this);
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
	
	public static class GuiInit implements Event {
		
		final boolean load;
		final String cn;
		
		public GuiInit(boolean _load, String _cn) {
			load = _load;
			cn = _cn;
		}

		@Override
		public void execute(GuiMatrix mat) {
			if(load) {
				mat.getGui().addWidget(new PageMain(mat, cn));
			} else {
				mat.getGui().addWidget(new PageInit(mat));
			}
		}
		
	}
	
	private static void drawText(String text, double x, double y, float size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	private static void drawText(String text, double x, double y, float size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}

}
