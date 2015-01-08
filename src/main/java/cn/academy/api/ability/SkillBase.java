/**
 * 
 */
package cn.academy.api.ability;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.data.AbilityData;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.client.render.SkillRenderDebug;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An empty skill and also the base class of all skills.
 * @author WeathFolD, acaly
 *
 */
public class SkillBase {

	/**
	 * Called by RawEventHandler when the skill is reset.
	 * Add patterns to the RawEventHandler instance in this function.
	 * Override this function to add pattern to your skill.
	 * @param reh The handler instance to add pattern into.
	 */
	public void initPattern(RawEventHandler reh) {}
	
	/**
	 * Get the internal identifier of the skill.
	 * @return skill name(identifier)
	 */
	public String getInternalName() {
		return "null";
	}
	
	/**
	 * Get the logo of the skill to be displayed in GUIs.
	 * @return the logo
	 */
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.TEX_QUESTION_MARK;
	}
	
	/**
	 * Get the name to be displayed of the skill.
	 * @return display name
	 */
	@SideOnly(Side.CLIENT)
	public final String getDisplayName() {
		return StatCollector.translateToLocal("skl_" + getInternalName());
	}
	
	public int getMaxSkillLevel() {
		return 1;
	}
	
	public void onSkillExpChange(AbilityData data, int skillID, float oldValue, float newValue) {
	}
}
