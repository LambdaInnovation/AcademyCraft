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
			super(ACLangs.confirm(), 46, 45.5);
			reopen = r;
		}

		@Override
		public void buttonPressed(double mx, double my) {
			//Action successfully peformed, which means current GUI data no more fresh.
			if(reopen) {
				dev.reload(); //drop all the cur instance and creat new one
			} else DiagStimulate.this.dispose();
		}
		
	}
	
	final IDevAction devAction;
	RandBufProgressBar progress;

	public DiagStimulate(final GuiDeveloper dev, IDevAction ida) {
		super("stimulate", dev, 9); //⑨
		this.setTitle(ACLangs.stimProg());
		this.alignStyle = AlignStyle.CENTER;
		this.devAction = ida;
	}
	
	@Override
	public void onAdded() {
		addWidget(new ButtonWarning(ACLangs.cancel(), 46, 45.5) {
			@Override
			public void draw(double mx, double my, boolean hover) {
				super.draw(mx, my, hover);
				if(!dev.dev.isStimulating) {
					DiagStimulate.this.addWidget(new ButtonConfirm(dev.dev.isStimSuccessful()));
					progress.fluctRegion = 0;
					this.dispose();
				}
			}

			@Override
			public void buttonPressed(double mx, double my) {
				dev.dev.isStimulating = false;
				AcademyCraft.netHandler.sendToServer(new MsgActionStart(dev.dev, -1, 0));
			}
		});
		
		addWidget(progress = new RandBufProgressBar(6.5, 16, 103, 5.5, 13, 137, 206, 11) {
			{
				this.addSetTexture(TEXTURE);
			}
			@Override
			public double getProgress() {
				return (double)dev.dev.stimSuccess / dev.dev.maxStimTimes;
			}
		});
	}
	
	final int[] ERROR_COLOR = {213, 45, 6};
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		RenderUtils.loadTexture(TEXTURE);
		HudUtils.drawRect(6, 15.5, 12, 121, 104, 6.5, 208, 13);
		
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		if(!dev.dev.isStimulating && !dev.dev.isStimSuccessful()) {
			RenderUtils.bindColor(ERROR_COLOR);
		}
		//CurAction
		String text = String.format("%s: %s", 
				dev.dev.isStimulating ? ACLangs.curAction() : (dev.dev.isStimSuccessful() ? ACLangs.successful() : ACLangs.aborted()),  
				devAction.getActionInfo(dev.data));
		dev.drawText(text, 6, 24, 7);
		
		//StimTimes
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		text = String.format("%s: %d/%d", ACLangs.attemptes(), dev.dev.stimSuccess, dev.dev.maxStimTimes);
		dev.drawText(text, 6, 31, 6);
		
		//StimFails
		RenderUtils.bindColor(ERROR_COLOR);
		double len = TextUtils.getWidth(GuiDeveloper.FONT, text, 6);
		text = String.format("(%d %s)", dev.dev.stimFailure, ACLangs.fails());
		dev.drawText(text, 6 + len, 31, 6);
		
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		//SyncRate
		text = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.getSyncRateForDisplay());
		dev.drawText(text, 6, 38.5, 6);
		
		RenderUtils.bindIdentity();
	}
	
	@Override
	public int getDrawPriority() {
		return 8;
	}

}
