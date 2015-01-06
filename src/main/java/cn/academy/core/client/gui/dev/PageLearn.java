/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.Set;

import org.lwjgl.opengl.GL11;

import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.widget.TextButton;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;
import cn.liutils.util.render.TrueTypeFont;

/**
 * @author WeathFolD
 *
 */
public class PageLearn extends DevSubpage {

	public PageLearn(PageMainBase parent) {
		super(parent, "page.adlearning", ACClientProps.TEX_GUI_AD_LEARNING);
		TextButton btn = new TextButton("btn_learn", this, 34F, 26F, 61.5F, 13.5F) {
			{
				setTexMapping(1, 448, 123, 27);
				setDownMapping(1, 419);
				setTexture(ACClientProps.TEX_GUI_AD_LEARNING, 512, 512);
				setTextProps(ACLangs.learnAbility(), 8);
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				new DiagActionConfirm(dev, TileDeveloper.ID_LEVEL_UPGRADE, 1);
			}
		};
		
	}

	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		GL11.glPushMatrix(); {
			//Energy bar
			double prog = dev.dev.curEnergy / dev.dev.getMaxEnergy();
			HudUtils.drawRect(8.5, 112.5, 17, 293, 122 * prog, 5.5, 244 * prog, 11);
			
			RenderUtils.bindColor(dev.DEFAULT_COLOR);
			//Machine stat
			String str = ACLangs.machineStat();
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 100.5, 9);
			//Current Energy
			str = String.format("%s: %.0f/%.0f EU", ACLangs.curEnergy(), dev.dev.curEnergy, dev.dev.getMaxEnergy());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 121, 8);
			//Sync Rate
			str = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.syncRateDisplay());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 129, 8);
			GL11.glColor4f(1, 1, 1, 1);
		} GL11.glPopMatrix();
	}
	
	static final int[] BUTTON_COLOR = {120, 206, 255};

}
