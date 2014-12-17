/**
 * 
 */
package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.data.AbilityData;

/**
 * @author WeathFolD
 *
 */
public abstract class SkillBase {
	
	Category parent;

	public SkillBase(Category cat) {
		parent = cat;
	}
	
	//Prototype, may need to add parameters
	public abstract void onSkillEvent(EntityPlayer player, SkillEventType type, AbilityData data);

	/**
	 * Called by RawEventHandler when the skill is reset.
	 * Add patterns to the RawEventHandler instance in this function.
	 * @param reh The handler instance to add pattern into.
	 */
	public abstract void initPattern(RawEventHandler reh);
}
