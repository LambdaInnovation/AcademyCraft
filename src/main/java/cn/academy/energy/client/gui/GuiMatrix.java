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

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.academy.energy.client.gui.Dialogues.DiagState;
import cn.academy.energy.client.gui.Dialogues.Dialogue;
import cn.academy.energy.client.gui.Dialogues.StateDiag;
import cn.academy.energy.msg.matrix.MsgChangePwd;
import cn.academy.energy.msg.matrix.MsgGuiLoadQuery;
import cn.academy.energy.msg.matrix.MsgInitMatrix;
import cn.academy.misc.util.ACUtils;
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
		TEX = new ResourceLocation("academy:textures/guis/wireless_mat.png");
	
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
			ACUtils.drawText(pct, 52, 122, 6.2f, Align.CENTER);
			
			ACUtils.drawText(channelName, 52, 25, 7f, Align.CENTER);
			ACUtils.drawText("******", 52, 55, 7f, Align.CENTER);
		}
		
		private abstract class SB extends Widget {
			public SB() {
				setSize(74.5, 10.5);
			}
			@Override
			public void draw(double mx, double my, boolean h) {
				if(h) {
					RenderUtils.bindColor(126, 241, 241, 180);
					HudUtils.drawModalRect(0, 0, width, height);
				}
			}
		}
	}
	
	private static abstract class MatDialogue extends Dialogue {
		final GuiMatrix mat;
		
		public MatDialogue(GuiMatrix _mat) {
			mat = _mat;
		}
	}
	
	static class PageInit extends MatDialogue {
		
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
			addWidget(new Dialogues.WigOK() {
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
					if(ssid.getContent().equals("")) {
						setErrMessage(ACLangs.ssidnotnull());
						return;
					}
					AcademyCraft.netHandler.sendToServer(
						new MsgInitMatrix(mat.mat, 
						ssid.getContent(), pwd.getContent()));
					PageInit.this.dispose();
					mat.gui.addWidget(mat.stateDiag = new StateDiag());
				}
			});
			
			//Abort button
			addWidget(new Dialogues.WigBad() {
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
				.setTextColor(0x00FFFF));
			
			addWidget(pwd = new InputBox(48, 39, 46, 8.5, 6, 1, 12)
				.setEcho(true).setTextColor(0x00FFFF));
			
			addWidget(pwd2 = new InputBox(48, 53, 46, 8.5, 6, 1, 12)
				.setEcho(true).setTextColor(0x00FFFF));
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			RenderUtils.bindColor(0, 255, 255, 255);
			ACUtils.drawText(ACLangs.matInit(), 54, 8.5, 7, Align.CENTER);
			
			//input elements
			RenderUtils.loadTexture(Dialogues.TEX_DIAG);
			HudUtils.drawRect(11, 24, 23, 202, 86, 38.5, 172, 77);
			
			if(errStr != null) {
				long time = Minecraft.getSystemTime();
				GL11.glColor4d(1, 0.2, 0.2, 0.8);
				ACUtils.drawText(errStr, 12, 63, 5.5);
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
	
	static class PwdWindow extends MatDialogue {
		
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
			addWidget(new Dialogues.WigOK() {
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
					mat.gui.addWidget(mat.stateDiag = new StateDiag());
				}
			});
			
			//Abort button
			addWidget(new Dialogues.WigBad() {
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
				.setTextColor(0x00FFFF));
			
			addWidget(pwd = new InputBox(48, 39, 46, 8.5, 6, 1, 12)
				.setEcho(true).setTextColor(0x00FFFF));
			
			addWidget(pwd2 = new InputBox(48, 53, 46, 8.5, 6, 1, 12)
				.setEcho(true).setTextColor(0x00FFFF));
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			RenderUtils.bindColor(0, 255, 255, 255);
			ACUtils.drawText(ACLangs.matChangePwd(), 54, 8.5, 7, Align.CENTER);
			
			//input elements
			RenderUtils.loadTexture(Dialogues.TEX_DIAG);
			HudUtils.drawRect(11, 38, 23, 230, 86, 24.5, 172, 49);
			HudUtils.drawRect(11, 24, 23, 286, 86, 10.5, 172, 21);
			
			if(errStr != null) {
				long time = Minecraft.getSystemTime();
				GL11.glColor4d(1, 0.2, 0.2, 0.8);
				ACUtils.drawText(errStr, 12, 63, 5.5);
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
				mat.stateDiag.state = DiagState.SUCCESS;
			} else {
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
				mat.stateDiag.state = DiagState.SUCCESS;
			} else {
				mat.stateDiag.state = DiagState.FAIL;
			}
			mat.stateDiag.initCancel();
		}
		
	}

}
