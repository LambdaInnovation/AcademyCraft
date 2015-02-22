/**
 * 
 */
package cn.academy.api.ability;

import java.util.Random;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.data.AbilityData;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An empty skill and also the base class of all skills.
 * Skill is stored in Abilities class, and handled by category, but not necessarily by one cat
 * (This may be a feature in further updates). One skill can currently only specify ONE key to operate on.
 * see {@link #initPattern(RawEventHandler)} to know how to set up a skill's control listening.
 * You must also specify this skill's logo and name to be drawn in various GUIs.
 * @author WeathFolD, acaly
 */
public class SkillBase {
	
	ResourceLocation logo;
	String name = "null";
	int maxSkillLv = 1;
	
	protected static final Random rand = new Random();
	
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
	public final String getInternalName() {
		return name;
	}
	
	public void setName(String _name) {
		name = _name;
	}
	
	public void setMaxSkillLevel(int i) {
		maxSkillLv = i;
	}
	
	public final int getMaxSkillLevel() {
		return maxSkillLv;
	}
	
	/**
	 * Return the translated skill description(hint).
	 */
	public final String getDescription() {
		return StatCollector.translateToLocal(getInternalName() + ".desc");
	}
	
	/**
	 * Return the index of the skill in some category. Ret -1 if skill is not in the cat.
	 */
	public final int getIndexInCategory(Category cat) {
		for(int i = 0; i < cat.getSkillCount(); ++i) {
			if(cat.getSkill(i) == this)
				return i;
		}
		return -1;
	}
	
	@Deprecated
	public void onSkillExpChange(AbilityData data, int skillID, float oldValue, float newValue) {}
	
	/**
	 * Get the logo of the skill to be displayed in GUIs.
	 * @return the logo
	 */
	@SideOnly(Side.CLIENT)
	public final ResourceLocation getLogo() {
		return logo == null ? ACClientProps.TEX_QUESTION_MARK : logo;
	}
	
	/**
	 * Just specify the path after "/textures/abilities/".
	 */
	protected void setLogo(String name) {
		logo = new ResourceLocation("academy:textures/abilities/" + name);
	}
	
	/**
	 * Get the name to be displayed of the skill.
	 * @return display name
	 */
	@SideOnly(Side.CLIENT)
	public final String getDisplayName() {
		return StatCollector.translateToLocal("skl_" + getInternalName());
	}
	
	/**
	 * @return If this skill is a 'dummy' skill (Skill that can't be controlled, only receive other events)
	 */
	public boolean isDummy() {
		return false;
	}
	
}
