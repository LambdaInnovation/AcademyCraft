/**
 * 
 */
package cn.academy.core.client.gui.dev;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.core.block.dev.IDevAction;
import cn.academy.core.block.dev.MsgActionStart;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.liutils.api.gui.widget.RandBufProgressBar;
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
			super("confirm", 46 * SCALE, 45.5 * SCALE);
			this.setTextProps(ACLangs.confirm(), 6 * SCALE);
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
	final RandBufProgressBar progress;

	public DiagStimulate(final GuiDeveloper dev, IDevAction ida) {
		super("stimulate", dev, 9); //⑨
		this.setTitle(ACLangs.stimProg());
		this.devAction = ida;
		
		new ButtonWarning("abort", 46 * SCALE, 45.5 * SCALE) {
			{
				this.setTextProps(ACLangs.cancel(), 6 * SCALE);
			}
			
			@Override
			public void draw(double mx, double my, boolean hover) {
				super.draw(mx, my, hover);
				if(!dev.dev.isStimulating) {
					new ButtonConfirm(dev.dev.isStimSuccessful());
					progress.fluctRegion = 0;
					this.dispose();
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				dev.dev.isStimulating = false;
				AcademyCraft.netHandler.sendToServer(new MsgActionStart(dev.dev, -1, 0));
			}
		};
		
		progress = new RandBufProgressBar("progress", this, 6.5 * SCALE, 16 * SCALE, 103 * SCALE, 5.5 * SCALE) {
			{
				this.setTexMapping(13, 137, 206, 11);
				this.setTexture(TEXTURE, 512, 512);
			}
			@Override
			public double getProgress() {
				//System.out.println(dev.dev.stimSuccess + "/" + dev.dev.maxStimTimes + ":" + ((double)dev.dev.stimSuccess) / dev.dev.maxStimTimes);
				return ((double)dev.dev.stimSuccess) / dev.dev.maxStimTimes;
			}
		};
	}
	
	final int[] ERROR_COLOR = {213, 45, 6};
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		RenderUtils.loadTexture(TEXTURE);
		HudUtils.drawRect(6 * SCALE, 15.5 * SCALE, 12, 121, 104 * SCALE, 6.5 * SCALE, 208, 13);
		
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		if(!dev.dev.isStimulating && !dev.dev.isStimSuccessful()) {
			RenderUtils.bindColor(ERROR_COLOR);
		}
		//CurAction
		String text = String.format("%s: %s", 
				dev.dev.isStimulating ? ACLangs.curAction() : (dev.dev.isStimSuccessful() ? ACLangs.successful() : ACLangs.aborted()),  
				devAction.getActionInfo(dev.data));
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6 * SCALE, 24 * SCALE, 7 * SCALE);
		
		//StimTimes
		text = String.format("%s: %d/%d", ACLangs.attemptes(), dev.dev.stimSuccess, dev.dev.maxStimTimes);
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6 * SCALE, 31 * SCALE, 6 * SCALE);
		
		//StimFails
		RenderUtils.bindColor(ERROR_COLOR);
		double len = TextUtils.getWidth(TextUtils.FONT_CONSOLAS_64, text, 6 * SCALE);
		text = String.format("(%d %s)", dev.dev.stimFailure, ACLangs.fails());
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6 * SCALE + len, 31 * SCALE, 6 * SCALE);
		
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		//SyncRate
		text = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.getSyncRateForDisplay());
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 6 * SCALE, 38.5 * SCALE, 6 * SCALE);
		
		GL11.glColor4d(1, 1, 1, 1);
		
	}
	
	@Override
	public int getDrawPriority() {
		return 8;
	}

}
