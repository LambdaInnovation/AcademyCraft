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
		GuiDeveloper.drawText(ACLangs.confirmHead(), 57.5, 15, 7, Align.CENTER);
		GuiDeveloper.drawText(ACLangs.confirmTail(), 57.5, 35.5, 7, Align.CENTER);
		RenderUtils.bindColor(200, 97, 29);
		GuiDeveloper.drawText(devAction.getActionInfo(dev.data), 57.5, 24, 9, Align.CENTER);
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
