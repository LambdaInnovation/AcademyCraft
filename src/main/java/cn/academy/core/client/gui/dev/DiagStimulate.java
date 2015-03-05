/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.gui.dev;

import cn.academy.core.AcademyCraft;
import cn.academy.core.block.dev.IDevAction;
import cn.academy.core.block.dev.MsgActionStart;
import cn.academy.core.client.ACLangs;
import cn.academy.misc.util.ACUtils;
import cn.liutils.api.gui.widget.RandBufProgressBar;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class DiagStimulate extends DialogueBase {
	
	private class ButtonConfirm extends ButtonNormal {
		
		final boolean reopen;

		public ButtonConfirm(boolean r) {
			super(ACLangs.confirm(), 46, 46.5);
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
	
	final String info;

	public DiagStimulate(final GuiDeveloper dev, IDevAction ida) {
		super("stimulate", dev, 9); //⑨
		this.setTitle(ACLangs.stimProg());
		this.alignStyle = AlignStyle.CENTER;
		this.devAction = ida;
		info = devAction.getActionInfo(dev.data);
	}
	
	@Override
	public void onAdded() {
		addWidget(new ButtonWarning(ACLangs.cancel(), 46, 46.5) {
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
				info);
		ACUtils.drawText(text, 6, 24, 5, 104);
		
		//StimTimes
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		text = String.format("%s: %d/%d", ACLangs.attemptes(), dev.dev.stimSuccess, dev.dev.maxStimTimes);
		ACUtils.drawText(text, 6, 31, 4.5);
		
		//StimFails
		RenderUtils.bindColor(ERROR_COLOR);
		double len = GuiDeveloper.strLen(text, 4.5);
		text = String.format("(%d %s)", dev.dev.stimFailure, ACLangs.fails());
		ACUtils.drawText(text, 15 + len, 31, 4.5, 104 - 7 - len);
		
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		//SyncRate
		text = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.getSyncRateForDisplay());
		ACUtils.drawText(text, 6, 38.5, 4.5);
		
		RenderUtils.bindIdentity();
	}
	
	@Override
	public int getDrawPriority() {
		return 8;
	}

}
