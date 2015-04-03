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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.academy.energy.client.gui.Dialogues.DiagState;
import cn.academy.energy.client.gui.Dialogues.Dialogue;
import cn.academy.energy.client.gui.Dialogues.InputPassword;
import cn.academy.energy.client.gui.Dialogues.StateDiag;
import cn.academy.energy.msg.fr.MsgFRAction;
import cn.academy.energy.msg.fr.MsgFRInitQuery;
import cn.academy.misc.util.ACUtils;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.ListVertical;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeathFolD
 *
 */
public class GuiFreqRegulator extends LIGuiScreen {
	
	private static final ResourceLocation TEX = new ResourceLocation("academy:textures/guis/freqreg.png");

	final TileUserBase target;
	static final int[] TEXT_COLOR = { 120, 255, 255, 255 };
	
	//Sync Data
	public boolean synced;
	public Map<String, int[]> channels = new HashMap();
	public String curChannel;
	
	//Pages&States
	MainPage pageMain;
	StateDiag stateDiag;
	
	String selection;
	int[] selCoords;

	public GuiFreqRegulator(TileUserBase _target) {
		target = _target;
		gui.addWidget(pageMain = new MainPage());
		AcademyCraft.netHandler.sendToServer(new MsgFRInitQuery(target));
	}
	
	//---Main page
	private class MainPage extends Widget {
		Aborter aborter;
		
		public MainPage() {
			this.setSize(250, 171);
			this.initTexDraw(TEX, 0, 0, 500, 342);
			this.alignStyle = AlignStyle.CENTER;
		}
		
		@Override
		public void onAdded() {
			addWidget(new ActionButton());
			addWidget(aborter = new Aborter());
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			aborter.doesDraw = selection != null;
			
			RenderUtils.bindColor(TEXT_COLOR);
			ACUtils.drawText(ACLangs.freqReg(), 166, 29, 6, Align.CENTER);
			ACUtils.drawText(ACLangs.frChannelSelect(), 60, 31, 5.7);
			
			String ssTitle, ssName;
			if(selection == null) {
				ssTitle = ACLangs.frCurrentChannel();
				ssName = synced ? 
						(curChannel == null ? ACLangs.notConnected() : curChannel) 
						: ACLangs.loading();
			} else {
				ssTitle = ACLangs.frSelectedChannel();
				ssName = selection;
			}
			
			ACUtils.drawText(ssTitle, 164, 88, 7, Align.CENTER);
			ACUtils.drawText(ssName, 164, 101, 6, Align.CENTER, 50);
			RenderUtils.bindIdentity();
		}
	}
	
	private class ActionButton extends Widget {
		public ActionButton() {
			setPos(150, 50);
			setSize(28.5, 27);
			initTexDraw(TEX, 298, 349, 59, 54);
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			RenderUtils.loadTexture(TEX);
			if(h) {
				HudUtils.drawRect(-6, -6, 287, 414, 41, 39, 82, 78);
				
				RenderUtils.bindColor(TEXT_COLOR);
				String actionName = selection == null ? ACLangs.frClearConn() : ACLangs.frEstbConn();
				ACUtils.drawText(actionName, 30, -5, 6);
				RenderUtils.bindIdentity();
			}
			super.draw(mx, my, h);
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			if(selection == null) {
				if(synced && curChannel != null) {
					gui.addWidget(new ClearConfirm());
				}
			} else {
				gui.addWidget(new FreqPwd(selection, selCoords));
			}
		}
	}
	
	private class Aborter extends Widget {
		public Aborter() {
			setPos(197, 102);
			setSize(8, 8);
			initTexDraw(TEX, 0, 359, 21, 21);
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			selection = null;
			selCoords = null;
		}
	}
	
	private class CnList extends ListVertical {

		public CnList() {
			super("poi~", 52.5, 39.5, 63.5, 76);
		}
		
		@Override
		public void onAdded() {
			for(Entry<String, int[]> ent : channels.entrySet()) {
				addWidget(new SingleCn(ent));
			}
		}
		
	}
	
	private class SingleCn extends Widget {
		
		final String cn;
		final int[] coords;
		
		public SingleCn(Entry<String, int[]> ent) {
			setSize(63.5, 9.5);
			cn = ent.getKey();
			coords = ent.getValue();
		}
		
		@Override
		public void draw(double mx, double my, boolean b) {
			if(b) {
				GL11.glColor4d(1, 1, 1, .5);
				HudUtils.drawModalRect(0, 0, width, height);
			}
			RenderUtils.bindColor(TEXT_COLOR);
			ACUtils.drawText(cn, 2, 1, 5, 59);
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			selection = cn;
			selCoords = coords;
		}
		
	}
	
	//---Sub dialogues
	private class FreqPwd extends InputPassword {
		
		final int[] coords;

		public FreqPwd(String _cn, int[] _coords) {
			super(_cn);
			coords = _coords;
		}

		@Override
		public void performAction(String pwd) {
			gui.addWidget(stateDiag = new StateDiag());
			AcademyCraft.netHandler.sendToServer(new MsgFRAction(target, coords, pwd));
			dispose();
		}
		
	}
	
	private class ClearConfirm extends Dialogue {
		
		@Override
		public void onAdded() {
			addWidget(new Dialogues.WigOK() {
				{
					setPos(50, 75);
				}
				
				@Override
				public void onMouseDown(double mx, double my) {
					gui.addWidget(stateDiag = new StateDiag());
					AcademyCraft.netHandler.sendToServer(new MsgFRAction(target));
					ClearConfirm.this.dispose();
				}
			});
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			RenderUtils.bindColor(TEXT_COLOR);
			ACUtils.drawText(ACLangs.frClearConfirm(), 54, 36, 6.5, Align.CENTER);
		}
	}
	
	
	public void onInit() {
		pageMain.addWidget(new CnList());
	}
	
	public void actionFinish(boolean b) {
		if(stateDiag != null) {
			if(b) {
				stateDiag.state = DiagState.SUCCESS;
			} else {
				stateDiag.state = DiagState.FAIL;
			}
			stateDiag.initCancel();
		}
	}
	
    @Override
	public boolean doesGuiPauseGame()  {
        return false;
    }
    
}
