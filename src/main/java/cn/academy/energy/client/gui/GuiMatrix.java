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
import cn.academy.energy.msg.matrix.MsgChangePwd;
import cn.academy.energy.msg.matrix.MsgGuiLoadQuery;
import cn.academy.energy.msg.matrix.MsgInitMatrix;
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
	
	enum DiagState { 
		LOADING(279, 95), FAIL(253, 95), SUCCESS(220, 95);
		DiagState(int _u, int _v) {
			u = _u;
			v = _v;
		}
		public final int u, v;
	};
	
	PageMain pageMain;
	StateDiag stateDiag; //Current state dialogue, not necessarily alive all the time
	final TileMatrix mat;

	public GuiMatrix(TileMatrix _mat) {
		mat = _mat;
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
					mat.gui.addWidget(new PwdWindow(mat));
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
			double prog = mat.mat.getEnergy() / mat.mat.getMaxEnergy();
			//h = 129
			GL11.glPushMatrix(); {
				double len = prog * 129;
				GL11.glTranslated(53.2, 126.5, 0);
				HudUtils.drawRect(-36, -32.25, 34, 383, 72, len / 2, 144, len);
			} GL11.glPopMatrix();
			
			RenderUtils.bindColor(108, 236, 236);
			String pct = String.format("%.2f%%", prog * 100);
			drawText(pct, 52, 122, 6.2f, Align.CENTER);
			
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
	
	public static abstract class Dialogue extends Widget {
		final GuiMatrix mat;
		
		public Dialogue(GuiMatrix _mat) {
			mat = _mat;
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
	
	static class WigOK extends Widget {
		{
			setSize(10.5, 10.5);
			initTexDraw(TEX_DIAG, 220, 121, 21, 21);
		}
	}
	
	static class WigBad extends Widget {
		{
			setSize(10.5, 10.5);
			initTexDraw(TEX_DIAG, 253, 95, 21, 21);
		}
	}
	
	static class PageInit extends Dialogue {
		
		InputBox ssid, pwd, pwd2;
		
		static final long TIME_WAIT = 1500;
		String errStr;
		long lastTime;

		public PageInit(GuiMatrix _mat) {
			super(_mat);
		}
		
		@Override
		public void onAdded() {
			//Confirm button
			addWidget(new WigOK() {
				{
					setPos(26, 73);
				}
				@Override
				public void onMouseDown(double mx, double my) {
					//VALIDATION
					if(!pwd.getContent().equals(pwd2.getContent())) {
						setErrMessage(ACLangs.inconsistentPass());
						return;
					}
					AcademyCraft.netHandler.sendToServer(
						new MsgInitMatrix(mat.mat, 
						ssid.getContent(), pwd.getContent()));
					PageInit.this.dispose();
					mat.gui.addWidget(mat.stateDiag = new StateDiag(mat));
				}
			});
			
			//Abort button
			addWidget(new WigBad() {
				{
					setPos(69, 73);
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
			
			if(errStr != null) {
				long time = Minecraft.getSystemTime();
				GL11.glColor4d(1, 0.2, 0.2, 0.8);
				drawText(errStr, 12, 63, 5.5);
				if(time - lastTime > TIME_WAIT) {
					errStr = null;
				}
			}
			RenderUtils.bindIdentity();
		}
		
		private void setErrMessage(String msg) {
			this.errStr = msg;
			this.lastTime = Minecraft.getSystemTime();
		}
		
	}
	
	static class StateDiag extends Dialogue {
		
		private DiagState state = DiagState.LOADING;
		public String msg = ACLangs.transmitting();

		public StateDiag(GuiMatrix _mat) {
			super(_mat);
		}
		
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
			addWidget(new WigOK() {
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
	
	static class PwdWindow extends Dialogue {
		
		InputBox opw, pwd, pwd2;
		
		static final long TIME_WAIT = 1500;
		String errStr;
		long lastTime;

		public PwdWindow(GuiMatrix _mat) {
			super(_mat);
		}
		
		@Override
		public void onAdded() {
			//Confirm button
			addWidget(new WigOK() {
				{
					setPos(26, 73);
				}
				@Override
				public void onMouseDown(double mx, double my) {
					//VALIDATION
					if(!pwd.getContent().equals(pwd2.getContent())) {
						setErrMessage(ACLangs.inconsistentPass());
						return;
					}
					AcademyCraft.netHandler.sendToServer(
						new MsgChangePwd(mat.pageMain.channelName, 
						opw.getContent(), pwd.getContent()));
					PwdWindow.this.dispose();
					mat.gui.addWidget(mat.stateDiag = new StateDiag(mat));
				}
			});
			
			//Abort button
			addWidget(new WigBad() {
				{
					setPos(69, 73);
				}
				
				@Override
				public void onMouseDown(double mx, double my) {
					//Close
					Minecraft.getMinecraft().thePlayer.closeScreen();
				}
			});
			
			addWidget(opw = new InputBox(48, 25, 46, 8.5, 6, 1, 12)
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
			
			if(errStr != null) {
				long time = Minecraft.getSystemTime();
				GL11.glColor4d(1, 0.2, 0.2, 0.8);
				drawText(errStr, 12, 63, 5.5);
				if(time - lastTime > TIME_WAIT) {
					errStr = null;
				}
			}
			RenderUtils.bindIdentity();
		}
		
		private void setErrMessage(String msg) {
			this.errStr = msg;
			this.lastTime = Minecraft.getSystemTime();
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
			if(mat.stateDiag == null)
				return;
			if(successful) {
				mat.stateDiag.msg = ACLangs.opSuccessful();
				mat.stateDiag.state = DiagState.SUCCESS;
			} else {
				mat.stateDiag.msg = ACLangs.channelExists();
				mat.stateDiag.state = DiagState.FAIL;
			}
			mat.stateDiag.initCancel();
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
				mat.getGui().addWidget(mat.pageMain = new PageMain(mat, cn));
			} else {
				mat.getGui().addWidget(new PageInit(mat));
			}
		}
		
	}
	
	public static class PwdResponse implements Event {
		
		final boolean successful;
		
		public PwdResponse(boolean _suc) {
			successful = _suc;
		}

		@Override
		public void execute(GuiMatrix mat) {
			if(mat.stateDiag == null)
				return;
			if(successful) {
				mat.stateDiag.msg = ACLangs.opSuccessful();
				mat.stateDiag.state = DiagState.SUCCESS;
			} else {
				mat.stateDiag.msg = ACLangs.opFailed();
				mat.stateDiag.state = DiagState.FAIL;
			}
			mat.stateDiag.initCancel();
		}
		
	}
	
	private static void drawText(String text, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	private static void drawText(String text, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}

}
