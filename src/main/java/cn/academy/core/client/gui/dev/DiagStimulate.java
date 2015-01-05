/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.ResourceLocation;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class DiagStimulate extends DialogueBase {
	
	private class ButtonConfirm extends ButtonNormal {

		public ButtonConfirm() {
			super("confirm", 46, 45.5);
			this.setTextProps(ACLangs.confirm(), 6);
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			DiagStimulate.this.dispose();
		}
		
	}

	public DiagStimulate(final GuiDeveloper dev) {
		super("stimulate", dev, 9); //⑨
		this.setTitle(ACLangs.stimProg());
		
		new ButtonWarning("abort", 46, 45.5) {
			{
				this.setTextProps(ACLangs.cancel(), 6);
			}
			
			@Override
			public void draw(double mx, double my, boolean hover) {
				super.draw(mx, my, hover);
				if(!dev.dev.isStimulating) {
					new ButtonConfirm();
					dispose();
				}
			}
		};
	}
	
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		RenderUtils.loadTexture(TEXTURE);
		HudUtils.drawRect(6, 15.5, 12, 121, 104, 6.5, 208, 13);
		double prog = (double) dev.dev.stimSuccess / dev.dev.maxStimTimes;
		HudUtils.drawRect(6.5, 16, 13, 137, 103 * prog, 5.5, 206 * prog, 11);
	}
	
	@Override
	public int getDrawPriority() {
		return 8;
	}

}
