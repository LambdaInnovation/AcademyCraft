/**
 * 
 */
package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.data.AbilityData;

/**
 * An empty skill and also the base class of all skills.
 * @author WeathFolD, acaly
 *
 */
public class SkillBase {
	
	/*
	Category parent;

	public SkillBase(Category cat) {
		parent = cat;
	}
	*/
	
	/**
	 * Called by RawEventHandler when the skill is reset.
	 * Add patterns to the RawEventHandler instance in this function.
	 * Override this function to add pattern to your skill.
	 * @param reh The handler instance to add pattern into.
	 */
	public void initPattern(RawEventHandler reh) {}
	
}
