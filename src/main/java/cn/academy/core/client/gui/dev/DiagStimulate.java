/**
 * 
 */
package cn.academy.core.client.gui.dev;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraftMod;
import cn.academy.core.block.dev.IDevAction;
import cn.academy.core.block.dev.MsgActionStart;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;

/**
 * @author WeathFolD
 *
 */
public class DiagStimulate extends DialogueBase {
	
	private class ButtonConfirm extends ButtonNormal {
		
		final boolean reopen;

		public ButtonConfirm(boolean r) {
			super("confirm", 46, 45.5);
			this.setTextProps(ACLangs.confirm(), 6);
			reopen = r;
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			//Action successfully peformed, which means current GUI data no more fresh.
			//Let user reopen once.
			if(reopen) {
				dev.dev.userQuit();
				dev.user.closeScreen();
			} else DiagStimulate.this.dispose();
		}
		
	}
	
	final IDevAction devAction;

	public DiagStimulate(final GuiDeveloper dev, IDevAction ida) {
		super("stimulate", dev, 9); //⑨
		this.setTitle(ACLangs.stimProg());
		this.devAction = ida;
		
		new ButtonWarning("abort", 46, 45.5) {
			{
				this.setTextProps(ACLangs.cancel(), 6);
			}
			
			@Override
			public void draw(double mx, double my, boolean hover) {
				super.draw(mx, my, hover);
				if(!dev.dev.isStimulating) {
					new ButtonConfirm(dev.dev.isStimSuccessful());
					dispose();
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				dev.dev.isStimulating = false;
				AcademyCraftMod.netHandler.sendToServer(new MsgActionStart(dev.dev, -1, 0));
			}
		};
	}
	
	final int[] ERROR_COLOR = {213, 45, 6};
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		RenderUtils.loadTexture(TEXTURE);
		HudUtils.drawRect(6, 15.5, 12, 121, 104, 6.5, 208, 13);
		double prog = (double) dev.dev.stimSuccess / dev.dev.maxStimTimes;
		HudUtils.drawRect(6.5, 16, 13, 137, 103 * prog, 5.5, 206 * prog, 11);
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		if(!dev.dev.isStimulating && !dev.dev.isStimSuccessful()) {
			RenderUtils.bindColor(ERROR_COLOR);
		}
		//CurAction
		String text = String.format("%s: %s", 
				dev.dev.isStimulating ? ACLangs.curAction() : (dev.dev.isStimSuccessful() ? ACLangs.successful() : ACLangs.aborted()),  
				devAction.getActionInfo(dev.data));
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6, 24, 7);
		
		//StimTimes
		text = String.format("%s: %d/%d", ACLangs.attemptes(), dev.dev.stimSuccess, dev.dev.maxStimTimes);
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6, 31, 6);
		
		//StimFails
		RenderUtils.bindColor(ERROR_COLOR);
		double len = TextUtils.getWidth(TextUtils.FONT_CONSOLAS_64, text, 6);
		text = String.format("(%d %s)", dev.dev.stimFailure, ACLangs.fails());
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6 + len, 31, 6);
		
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		//SyncRate
		text = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.syncRateDisplay());
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6, 38.5, 6);
		
		GL11.glColor4d(1, 1, 1, 1);
		
	}
	
	@Override
	public int getDrawPriority() {
		return 8;
	}

}
