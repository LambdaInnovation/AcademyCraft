/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.gui.dev;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.core.block.dev.IDevAction;
import cn.academy.core.block.dev.MsgActionStart;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.academy.misc.util.ACUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeathFolD
 *
 */
public class DiagActionConfirm extends DialogueBase {
	
	final IDevAction devAction;
	public boolean result;
	final int id, par;

	public DiagActionConfirm(GuiDeveloper dev, int id, int par) {
		super("confirm", dev, 5);
		devAction = TileDeveloper.getAction(id, par);
		this.id = id;
		this.par = par;
		this.setTitle(ACLangs.actionConfirm());
		this.alignStyle = AlignStyle.CENTER;
	}
	
	@Override
	public void onAdded() {
		addWidgets(new ButtonNormal(ACLangs.confirm(), 25, 48) {
			@Override
			public void buttonPressed(double mx, double my) {
				result = true;
				onConfirm();
				DiagActionConfirm.this.dispose();
			}
		},new ButtonNormal(ACLangs.cancel(), 65, 48) {
			@Override
			public void buttonPressed(double mx, double my) {
				result = false;
				DiagActionConfirm.this.dispose();
			}
		});
	}
	
	@Override
	public int getDrawPriority() {
		return 4;
	}
	
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		ACUtils.drawText(ACLangs.confirmHead(), 57.5, 15, 4.5, Align.CENTER);
		ACUtils.drawText(ACLangs.confirmTail(), 57.5, 35.5, 5.5, Align.CENTER);
		RenderUtils.bindColor(200, 97, 29);
		ACUtils.drawText(devAction.getActionInfo(dev.data), 57.5, 24, 6, Align.CENTER, 102);
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	//--------EVENT HANDLER-----------
	/**
	 * Called when we confirm the dialogue action.
	 */
	public void onConfirm() {
		//Sync to the server
		dev.dev.startStimulating(id, par);
		AcademyCraft.netHandler.sendToServer(new MsgActionStart(dev.dev, id, par));
		
		//Open the progress gui
		dev.getGui().addWidget(new DiagStimulate(dev, devAction));
	}

}
