/**
 * 
 */
package cn.academy.core.client.gui.dev;

import org.lwjgl.opengl.GL11;

import cn.academy.core.block.dev.IDevelopAction;
import cn.academy.core.client.ACLangs;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;
import cn.liutils.util.render.TrueTypeFont;

/**
 * @author WeathFolD
 *
 */
public class DiagActionConfirm extends DialogueBase {
	
	final IDevelopAction devAction;
	public boolean result;

	public DiagActionConfirm(GuiDeveloper dev, IDevelopAction act) {
		super("confirm", dev, 5);
		devAction = act;
		this.setTitle(ACLangs.actionConfirm());
		
		//Init widgets
		new ButtonNormal("confirm", 25, 48) {
			{
				this.setTextProps(ACLangs.confirm(), 6);
			}
			@Override
			public void onMouseDown(double mx, double my) {
				result = true;
				onConfirm();
				System.out.println("Mew");
				DiagActionConfirm.this.dispose();
			}
		};
		
		new ButtonNormal("cancel", 65, 48) {
			{
				this.setTextProps(ACLangs.cancel(), 6);
			}
			@Override
			public void onMouseDown(double mx, double my) {
				result = false;
				DiagActionConfirm.this.dispose();
			}
		};
	}
	
	@Override
	public int getDrawPriority() {
		return 4;
	}
	
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		TrueTypeFont font = TextUtils.FONT_CONSOLAS_64;
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		TextUtils.drawText(font, ACLangs.confirmHead(), 57.5, 15, 7, TrueTypeFont.ALIGN_CENTER);
		TextUtils.drawText(font, ACLangs.confirmTail(), 57.5, 35.5, 7, TrueTypeFont.ALIGN_CENTER);
		RenderUtils.bindColor(200, 97, 29);
		TextUtils.drawText(font, devAction.getActionInfo(dev.data), 57.5, 24, 9, TrueTypeFont.ALIGN_CENTER);
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	//--------EVENT HANDLER-----------
	/**
	 * Called when we confirm the dialogue action.
	 */
	public void onConfirm() {
		//Open the progress gui
		//Sync to the server
	}

}
