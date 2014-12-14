/**
 * 
 */
package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
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

}
