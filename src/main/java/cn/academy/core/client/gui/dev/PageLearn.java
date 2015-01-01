/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.Set;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.TextUtils;
import cn.liutils.api.client.TrueTypeFont;
import cn.liutils.api.client.gui.part.LIGuiButton;
import cn.liutils.api.client.gui.part.LIGuiPart;
import cn.liutils.api.client.util.HudUtils;

/**
 * @author WeathFolD
 *
 */
public class PageLearn extends DevSubpage {

	public PageLearn(GuiDeveloper parent) {
		super(parent, "adsp_learn", ACClientProps.TEX_GUI_AD_LEARNING);
	}

	@Override
	public void drawPage() {
		super.drawPage();
		GL11.glPushMatrix(); {
			dev.bindColor(dev.DEFAULT_COLOR);
			double prog = dev.dev.curEnergy / dev.dev.getMaxEnergy();
			HudUtils.drawTexturedModalRect(8.5, 112.5, 17, 293, 122 * prog, 11, 244 * prog, 11);
			String str = ACLangs.machineStat();
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 100.5, 9);
			
			str = String.format("%s: %.0f/%.0f EU", ACLangs.curEnergy(), dev.dev.curEnergy, dev.dev.getMaxEnergy());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 121, 8);
			
			str = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.syncRateDisplay());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 129, 8);
			GL11.glColor4f(1, 1, 1, 1);
		} GL11.glPopMatrix();
	}
	
	static final int[] BUTTON_COLOR = {120, 206, 255};
	@Override
	public void addElements(Set<LIGuiPart> set) {
		LIGuiButton btn = new LIGuiButton("btn_learn", 34F, 26F, 61.5F, 13.5F) {
			@Override
			public void drawAtOrigin(float mx, float my, boolean mouseHovering) {
				super.drawAtOrigin(mx, my, mouseHovering);
				String str = ACLangs.learnAbility();
				dev.bindColor(dev.DEFAULT_COLOR);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 31F, 3F, 8, TrueTypeFont.ALIGN_CENTER);
				GL11.glColor4f(1, 1, 1, 1);
			}
		};
		btn.setDownCoords(1, 419).setInvaildCoords(1, 477)
				.setTexSize(123F, 27F).setTextureCoords(1, 448)
				.setTextureOverride(ACClientProps.TEX_GUI_AD_LEARNING);
		set.add(btn);

	}

	@Override
	public void onPartClicked(LIGuiPart part, float subX, float subY) {
	}

}
