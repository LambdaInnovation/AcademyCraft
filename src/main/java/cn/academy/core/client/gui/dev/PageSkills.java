/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.Set;

import net.minecraft.util.ResourceLocation;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.gui.part.LIGuiPart;

/**
 * @author WeathFolD
 *
 */
public class PageSkills extends DevSubpage {

	public PageSkills(GuiDeveloper parent) {
		super(parent, "skills", ACClientProps.TEX_GUI_AD_SKILL);
	}

	@Override
	public void addElements(Set<LIGuiPart> set) {
		
	}

	@Override
	public void onPartClicked(LIGuiPart part, float subX, float subY) {
		
	}

}
