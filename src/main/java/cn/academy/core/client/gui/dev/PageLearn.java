/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.Set;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.gui.part.LIGuiPart;

/**
 * @author WeathFolD
 *
 */
public class PageLearn extends DevSubpage {

	public PageLearn(GuiScreen parent) {
		super(parent, "adsp_learn", ACClientProps.TEX_GUI_AD_LEARNING);
	}

	@Override
	public void addElements(Set<LIGuiPart> set) {
	}

	@Override
	public void onPartClicked(LIGuiPart part, float subX, float subY) {
	}

}
